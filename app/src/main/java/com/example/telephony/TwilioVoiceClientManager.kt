package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.CapabilityTokenDto
import com.example.data.repository.BizVoiceRepository
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import com.twilio.voice.Call
import com.twilio.voice.CallException
import com.twilio.voice.ConnectOptions
import com.twilio.voice.Voice
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Real Twilio Voice Android SDK VoIP Manager.
 * 
 * Complies with Twilio Voice Architecture:
 * 1. Originate call through Client Leg (client:<identity>) using an Access/Capability Token from backend.
 * 2. Never calls api.twilio.com REST API directly from mobile client.
 * 3. Passes destination number exclusively as custom parameter `To` (e.g. `To: "+918469620312"`).
 * 4. Never passes a `From` parameter (the backend derives caller ID from token identity and executes <Dial>).
 * 5. Caches Capability Token with safe TTL and refreshes before expiration.
 * 6. Uses com.twilio.voice.Voice.connect and com.twilio.audioswitch.AudioSwitch for real VoIP audio routing.
 */
class TwilioVoiceClientManager(
    private val context: Context,
    private val repository: BizVoiceRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    companion object {
        private const val TAG = "TWILIO_VOICE_CLIENT"
        private const val TOKEN_TTL_MS = 50 * 60 * 1000L // 50 minutes safe cache TTL
    }

    // Twilio AudioSwitch for managing audio routing (Speakerphone, Earpiece, Bluetooth, Wired)
    private val audioSwitch: AudioSwitch = AudioSwitch(context.applicationContext, loggingEnabled = true)

    // Token Cache
    private var cachedTokenDto: CapabilityTokenDto? = null
    private var tokenFetchedAt: Long = 0L

    // Active Twilio Call & Session
    data class VoiceSession(
        val sessionId: String,
        val clientIdentity: String,
        val toPhoneNumber: String,
        val call: Call? = null,
        val startTime: Long = System.currentTimeMillis()
    )

    private var activeSession: VoiceSession? = null
    private var activeTwilioCall: Call? = null

    // Callbacks for CallManager
    interface CallEventListener {
        fun onConnecting(sessionId: String)
        fun onRinging(sessionId: String)
        fun onConnected(sessionId: String)
        fun onReconnecting(sessionId: String)
        fun onDisconnected(sessionId: String, durationSeconds: Long)
        fun onConnectFailure(sessionId: String, errorMessage: String)
    }

    private var eventListener: CallEventListener? = null

    init {
        try {
            audioSwitch.start { audioDevices, selectedDevice ->
                Log.d(TAG, "AudioSwitch devices: ${audioDevices.map { it.name }}, selected: ${selectedDevice?.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioSwitch: ${e.message}", e)
        }
    }

    fun setEventListener(listener: CallEventListener?) {
        this.eventListener = listener
    }

    /**
     * Checks if cached capability token is still valid.
     */
    private fun isTokenValid(): Boolean {
        val token = cachedTokenDto?.token
        if (token.isNullOrBlank()) return false
        val age = System.currentTimeMillis() - tokenFetchedAt
        return age < TOKEN_TTL_MS
    }

    /**
     * Retrieves or refreshes the Twilio Voice Access/Capability Token from the backend API.
     * Sent with the user's `authToken` header via repository.
     */
    suspend fun getOrFetchAccessToken(forceRefresh: Boolean = false): Result<CapabilityTokenDto> {
        if (!forceRefresh && isTokenValid() && cachedTokenDto != null) {
            return Result.success(cachedTokenDto!!)
        }

        val result = repository.getCapabilityToken(forceRefresh = forceRefresh)
        if (result.isSuccess) {
            val tokenData = result.getOrNull()
            if (tokenData != null && tokenData.token.isNotBlank()) {
                cachedTokenDto = tokenData
                tokenFetchedAt = System.currentTimeMillis()
                Log.i(TAG, "Successfully refreshed Voice Access Token for identity: ${tokenData.identity}")
                return Result.success(tokenData)
            }
        }
        return result
    }

    /**
     * Originates an outbound call using the real Twilio Voice SDK.
     * 
     * Passes destination as custom parameter `To`.
     * Does NOT pass `From` parameter (backend derives caller ID).
     */
    fun connectOutbound(toPhoneNumber: String, onResult: (Result<String>) -> Unit) {
        val cleanDestination = toPhoneNumber.trim()
        if (cleanDestination.length < 3) {
            onResult(Result.failure(IllegalArgumentException("Invalid destination phone number.")))
            return
        }

        // Task 5: Verify runtime microphone permission BEFORE calling Voice.connect()
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            val err = "Microphone permission (RECORD_AUDIO) is required to place calls."
            Log.e(TAG, "connectOutbound aborted: $err")
            onResult(Result.failure(SecurityException(err)))
            return
        }

        scope.launch {
            try {
                // Step 1: Ensure valid capability token from backend
                val tokenResult = getOrFetchAccessToken()
                if (tokenResult.isFailure) {
                    val err = tokenResult.exceptionOrNull()?.message ?: "Failed to obtain voice token from backend"
                    Log.e(TAG, "Cannot place call: $err")
                    onResult(Result.failure(Exception("Voice Authorization Error: $err")))
                    return@launch
                }

                val tokenData = tokenResult.getOrNull()!!
                val clientIdentity = tokenData.identity
                val sessionId = "client_call_" + UUID.randomUUID().toString().take(12)

                // Step 2: Build real Twilio ConnectOptions
                // Send "To" and ONLY "To". Never send "From".
                val params = mapOf("To" to cleanDestination)
                val connectOptions = ConnectOptions.Builder(tokenData.token)
                    .params(params)
                    .build()

                Log.i(TAG, "Connecting real Twilio Voice call: sessionId=$sessionId, clientIdentity=$clientIdentity, To=$cleanDestination")

                // Step 3: Implement real Call.Listener
                val callListener = object : Call.Listener {
                    override fun onConnectFailure(call: Call, error: CallException) {
                        Log.e(TAG, "Twilio Call.Listener onConnectFailure: code=${error.errorCode}, message=${error.message}, explanation=${error.explanation}")
                        stopAudioRouting()
                        activeTwilioCall = null
                        activeSession = null

                        val explanation = error.explanation?.ifBlank { null } ?: "Unknown Twilio Voice error"
                        val formattedError = "Call failed (Error ${error.errorCode}: ${error.message ?: explanation})"
                        eventListener?.onConnectFailure(sessionId, formattedError)
                    }

                    override fun onRinging(call: Call) {
                        Log.i(TAG, "Twilio Call.Listener onRinging: callSid=${call.sid}")
                        eventListener?.onRinging(sessionId)
                    }

                    override fun onConnected(call: Call) {
                        Log.i(TAG, "Twilio Call.Listener onConnected: callSid=${call.sid}, from=${call.from}, to=${call.to}")
                        activeSession = activeSession?.copy(startTime = System.currentTimeMillis())
                        eventListener?.onConnected(sessionId)
                    }

                    override fun onReconnecting(call: Call, error: CallException) {
                        Log.w(TAG, "Twilio Call.Listener onReconnecting: code=${error.errorCode}, message=${error.message}")
                        eventListener?.onReconnecting(sessionId)
                    }

                    override fun onReconnected(call: Call) {
                        Log.i(TAG, "Twilio Call.Listener onReconnected: callSid=${call.sid}")
                        eventListener?.onConnected(sessionId)
                    }

                    override fun onDisconnected(call: Call, error: CallException?) {
                        if (error != null) {
                            Log.w(TAG, "Twilio Call.Listener onDisconnected with error: code=${error.errorCode}, message=${error.message}")
                        } else {
                            Log.i(TAG, "Twilio Call.Listener onDisconnected: callSid=${call.sid}")
                        }
                        val duration = activeSession?.let { (System.currentTimeMillis() - it.startTime) / 1000 } ?: 0L
                        stopAudioRouting()
                        activeTwilioCall = null
                        activeSession = null
                        eventListener?.onDisconnected(sessionId, duration)
                    }
                }

                // Step 4: Activate audio and invoke Voice.connect()
                startAudioRouting()
                val twilioCall = Voice.connect(context, connectOptions, callListener)
                activeTwilioCall = twilioCall
                activeSession = VoiceSession(
                    sessionId = sessionId,
                    clientIdentity = clientIdentity,
                    toPhoneNumber = cleanDestination,
                    call = twilioCall,
                    startTime = System.currentTimeMillis()
                )

                // Notify caller that call initiation was accepted by SDK
                onResult(Result.success(sessionId))
                eventListener?.onConnecting(sessionId)

            } catch (e: Exception) {
                Log.e(TAG, "Error initiating real Twilio Voice call: ${e.message}", e)
                stopAudioRouting()
                activeTwilioCall = null
                activeSession = null
                onResult(Result.failure(e))
                eventListener?.onConnectFailure("client_call_init", e.message ?: "Failed to initialize call")
            }
        }
    }

    /**
     * Disconnects the active Twilio Call.
     */
    fun disconnect(sessionId: String? = null) {
        try {
            activeTwilioCall?.disconnect()
            Log.i(TAG, "activeTwilioCall.disconnect() invoked")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect active Twilio Call: ${e.message}", e)
        }
        stopAudioRouting()
        activeTwilioCall = null
        val session = activeSession
        activeSession = null
        if (session != null) {
            val duration = (System.currentTimeMillis() - session.startTime) / 1000
            eventListener?.onDisconnected(session.sessionId, maxOf(0L, duration))
        }
    }

    /**
     * Controls real WebRTC audio stream mute via Twilio Call object.
     */
    fun setMuted(isMuted: Boolean) {
        try {
            activeTwilioCall?.mute(isMuted)
            Log.d(TAG, "Twilio Call mute set to: $isMuted (callSid=${activeTwilioCall?.sid})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle Twilio Call mute: ${e.message}")
        }
    }

    /**
     * Controls speakerphone routing via Twilio AudioSwitch.
     */
    fun setSpeakerphoneOn(isOn: Boolean) {
        try {
            val available = audioSwitch.availableAudioDevices
            val targetDevice = if (isOn) {
                available.firstOrNull { it is AudioDevice.Speakerphone }
            } else {
                available.firstOrNull { it is AudioDevice.Earpiece || it is AudioDevice.BluetoothHeadset || it is AudioDevice.WiredHeadset }
            }
            if (targetDevice != null) {
                audioSwitch.selectDevice(targetDevice)
                Log.d(TAG, "AudioSwitch selected device: ${targetDevice.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle speakerphone in AudioSwitch: ${e.message}")
        }
    }

    private fun startAudioRouting() {
        try {
            audioSwitch.activate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate AudioSwitch: ${e.message}")
        }
    }

    private fun stopAudioRouting() {
        try {
            audioSwitch.deactivate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deactivate AudioSwitch: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            activeTwilioCall?.disconnect()
            activeTwilioCall = null
            audioSwitch.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup exception: ${e.message}")
        }
    }
}
