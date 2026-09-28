package com.example.telephony

import android.content.Context
import android.util.Log
import com.example.data.repository.BizVoiceRepository
import com.twilio.voice.CallException
import com.twilio.voice.CallInvite
import com.twilio.voice.CancelledCallInvite
import com.twilio.voice.MessageListener
import com.twilio.voice.RegistrationException
import com.twilio.voice.RegistrationListener
import com.twilio.voice.UnregistrationListener
import com.twilio.voice.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Registers this device with Twilio Voice so inbound `<Client>` legs can reach it.
 *
 * Until Voice.register() succeeds, Twilio has no device to ring and every inbound
 * Client leg fails with SipResponseCode 480 / CallStatus "no-answer". Registration is
 * what turns the capability token's push_credential_sid grant into an actual device
 * registration with Twilio's signalling edge.
 *
 * Token and identity must both come from the backend capability token endpoint, and the
 * identity must equal the value embedded in the token (grants.identity). If they
 * disagree, Twilio registers a different identity than the backend dials and the call
 * fails with 480 again.
 */
class IncomingCallRegistrar(
    private val context: Context,
    private val repository: BizVoiceRepository,
    private val scope: CoroutineScope,
    private val onIncomingCall: (CallInvite) -> Unit,
    private val onCancelledCall: (CancelledCallInvite) -> Unit
) {
    companion object {
        private const val TAG = "INCOMING_CALL_REG"
        private const val REGISTER_INTERVAL_MS = 5 * 60 * 1000L
    }

    private val isRegistered = AtomicBoolean(false)
    private val inFlight = AtomicBoolean(false)
    private var registeredIdentity: String? = null
    private var registeredToken: String? = null
    private var lastRegisteredAt: Long = 0L

    private val messageListener = object : MessageListener {
        override fun onCallInvite(callInvite: CallInvite) {
            Log.i(
                TAG,
                "Incoming CallInvite: sid=${callInvite.callSid} from=${callInvite.from} " +
                    "to=${callInvite.to} params=${callInvite.customParameters}"
            )
            onIncomingCall(callInvite)
        }

        override fun onCancelledCallInvite(callInvite: CancelledCallInvite, error: CallException?) {
            Log.w(TAG, "Cancelled CallInvite: sid=${callInvite.callSid} errorCode=${error?.errorCode}")
            onCancelledCall(callInvite)
        }
    }

    private val registrationListener = object : RegistrationListener {
        override fun onRegistered(token: String, identity: String) {
            isRegistered.set(true)
            registeredIdentity = identity
            lastRegisteredAt = System.currentTimeMillis()
            Log.i(TAG, "Voice.register OK for identity=$identity")
        }

        override fun onError(error: RegistrationException, token: String, identity: String) {
            isRegistered.set(false)
            Log.e(
                TAG,
                "Voice.register FAILED identity=$identity code=${error.errorCode} " +
                    "message=${error.message} explanation=${error.explanation}"
            )
        }
    }

    private val unregistrationListener = object : UnregistrationListener {
        override fun onUnregistered(token: String, identity: String) {
            Log.i(TAG, "Voice.unregister OK for identity=$identity")
        }

        override fun onError(error: RegistrationException, token: String, identity: String) {
            Log.w(TAG, "Voice.unregister FAILED identity=$identity code=${error.errorCode}")
        }
    }

    val isDeviceRegistered: Boolean get() = isRegistered.get()
    val currentIdentity: String? get() = registeredIdentity

    /**
     * Fetches a capability token and registers the device for incoming calls.
     *
     * Safe to call often: concurrent calls collapse into one, and a still-valid
     * registration is skipped unless a refresh is forced.
     */
    fun registerForIncomingCalls(forceRefresh: Boolean = false) {
        if (!inFlight.compareAndSet(false, true)) {
            Log.d(TAG, "registerForIncomingCalls already running, skipping")
            return
        }

        scope.launch {
            try {
                val withinInterval = System.currentTimeMillis() - lastRegisteredAt < REGISTER_INTERVAL_MS
                if (!forceRefresh && isRegistered.get() && withinInterval) {
                    Log.d(TAG, "Device already registered for $registeredIdentity, skipping")
                    return@launch
                }

                val tokenResult = repository.getCapabilityToken(forceRefresh = forceRefresh)
                val tokenData = tokenResult.getOrNull()
                if (tokenResult.isFailure || tokenData == null || tokenData.token.isBlank()) {
                    val err = tokenResult.exceptionOrNull()?.message ?: "empty token"
                    Log.e(TAG, "Cannot register, no capability token: $err")
                    return@launch
                }

                if (!forceRefresh && isRegistered.get() && tokenData.token == registeredToken) {
                    Log.d(TAG, "Token unchanged and device registered, skipping")
                    return@launch
                }

                Log.i(TAG, "Registering device for identity=${tokenData.identity}")
                withContext(Dispatchers.IO) {
                    Voice.register(
                        tokenData.token,
                        Voice.RegistrationChannel.FCM,
                        tokenData.identity,
                        registrationListener
                    )
                }
                registeredToken = tokenData.token
            } catch (e: Exception) {
                Log.e(TAG, "registerForIncomingCalls threw: ${e.message}", e)
            } finally {
                inFlight.set(false)
            }
        }
    }

    /**
     * Unregisters the device, e.g. on logout, so the identity stops receiving calls.
     */
    fun unregisterFromIncomingCalls() {
        val identity = registeredIdentity
        registeredToken = null
        registeredIdentity = null
        lastRegisteredAt = 0L
        isRegistered.set(false)
        if (identity.isNullOrBlank()) return

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Voice.unregister(identity, Voice.RegistrationChannel.FCM, identity, unregistrationListener)
                }
            } catch (e: Exception) {
                Log.e(TAG, "unregisterFromIncomingCalls threw: ${e.message}", e)
            }
        }
    }

    /**
     * Feeds an FCM data payload to the Twilio SDK.
     *
     * @return true when the payload was a recognised Twilio VoIP message, meaning it was
     *         consumed and an invite or cancellation was dispatched to the callbacks.
     */
    fun handlePushMessage(data: Map<String, String>): Boolean = try {
        Voice.handleMessage(context, data, messageListener)
    } catch (e: Exception) {
        Log.e(TAG, "Voice.handleMessage failed: ${e.message}", e)
        false
    }
}
