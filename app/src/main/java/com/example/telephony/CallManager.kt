package com.example.telephony

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.repository.BizVoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages softphone telephony sessions, audio routing, keypad tones,
 * and call state transitions via the Twilio Voice Client Architecture.
 */
class CallManager(
    private val context: Context,
    private val repository: BizVoiceRepository
) {
    companion object {
        private const val TAG = "CALL_MANAGER"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val voiceClientManager = TwilioVoiceClientManager(context, repository, scope)

    // Primary tone generator on music stream for loud feedback
    private var primaryToneGen: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 95)
    } catch (_: Exception) {
        null
    }

    // Voice call tone generator for in-call / earpiece tones
    private var voiceToneGen: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_VOICE_CALL, 90)
    } catch (_: Exception) {
        null
    }

    private var incomingRingtone: Ringtone? = null

    private val _activeCallFlow = MutableStateFlow(ActiveCallInfo())
    val activeCallFlow: StateFlow<ActiveCallInfo> = _activeCallFlow.asStateFlow()

    private var timerJob: Job? = null
    private var ringingToneJob: Job? = null

    init {
        val prefSpeaker = repository.sessionManager.defaultSpeaker
        _activeCallFlow.value = _activeCallFlow.value.copy(isSpeaker = prefSpeaker)
        initRingtone()
        setupVoiceClientListeners()
    }

    private fun setupVoiceClientListeners() {
        voiceClientManager.setEventListener(object : TwilioVoiceClientManager.CallEventListener {
            override fun onConnecting(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Connecting session $sessionId")
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    callId = sessionId,
                    state = CallState.CALLING
                )
                playKeypadTone('1')
            }

            override fun onRinging(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Ringing session $sessionId")
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    callId = sessionId,
                    state = CallState.RINGING
                )
                startOutgoingRingbackLoop()
            }

            override fun onConnected(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Connected session $sessionId")
                stopAllRingingAndTones()
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    callId = sessionId,
                    state = CallState.CONNECTED,
                    startTime = System.currentTimeMillis()
                )
                startCallTimer()
            }

            override fun onReconnecting(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Reconnecting session $sessionId")
                _activeCallFlow.value = _activeCallFlow.value.copy(state = CallState.RECONNECTING)
            }

            override fun onDisconnected(sessionId: String, durationSeconds: Long) {
                Log.i(TAG, "Voice Client Event: Disconnected session $sessionId (duration: ${durationSeconds}s)")
                handleCallDisconnected(durationSeconds)
            }

            override fun onConnectFailure(sessionId: String, errorMessage: String) {
                Log.e(TAG, "Voice Client Event: Connect failure session $sessionId: $errorMessage")
                stopAllRingingAndTones()
                playTone(ToneGenerator.TONE_PROP_NACK, 500)
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    state = CallState.FAILED,
                    errorMessage = errorMessage
                )
                scope.launch {
                    delay(2500)
                    resetCallState(CallState.IDLE)
                }
            }
        })
    }

    private fun initRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (ringtoneUri != null) {
                incomingRingtone = RingtoneManager.getRingtone(context, ringtoneUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    incomingRingtone?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Initiates a client-originated outbound call using the Twilio Voice Client.
     * The parent leg is client:<identity> and child leg is the customer's phone number.
     * Rings the customer exactly once and creates bridged two-way audio.
     */
    fun startOutgoingCall(phoneNumber: String, contactName: String? = null): Result<Unit> {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.length < 3) {
            return Result.failure(Exception("Please enter a valid phone number."))
        }

        // Cancel any pending tones & timer
        stopAllRingingAndTones()
        timerJob?.cancel()

        val initialSpeaker = repository.sessionManager.defaultSpeaker

        _activeCallFlow.value = ActiveCallInfo(
            callId = "call_init",
            remotePhoneNumber = cleanNumber,
            remoteName = contactName,
            direction = CallDirection.OUTGOING,
            state = CallState.PREPARING,
            durationSeconds = 0,
            isMuted = false,
            isSpeaker = initialSpeaker,
            isHold = false,
            dtmfLog = "",
            startTime = System.currentTimeMillis()
        )

        voiceClientManager.setSpeakerphoneOn(initialSpeaker)
        voiceClientManager.setMuted(false)

        // Connect through client leg
        voiceClientManager.connectOutbound(cleanNumber) { result ->
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unable to connect call"
                stopAllRingingAndTones()
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    state = CallState.FAILED,
                    errorMessage = errorMsg
                )
                playTone(ToneGenerator.TONE_PROP_NACK, 500)
                scope.launch {
                    delay(2500)
                    resetCallState(CallState.IDLE)
                }
            }
        }

        return Result.success(Unit)
    }

    private fun handleCallDisconnected(finalDuration: Long) {
        stopAllRingingAndTones()
        timerJob?.cancel()

        val current = _activeCallFlow.value
        if (current.state == CallState.IDLE || current.state == CallState.ENDED) return

        val duration = maxOf(current.durationSeconds, finalDuration)
        val recordStatus = if (duration > 0) CallRecordStatus.COMPLETED else CallRecordStatus.CANCELED

        // Play prompt tone
        try {
            primaryToneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 400)
        } catch (_: Exception) {}

        saveCallRecord(
            status = recordStatus,
            direction = current.direction,
            duration = duration
        )

        _activeCallFlow.value = current.copy(
            state = CallState.ENDED,
            durationSeconds = duration
        )

        scope.launch {
            delay(1500)
            resetCallState(CallState.IDLE)
        }
    }

    fun triggerIncomingCall(phoneNumber: String, callerName: String? = null) {
        stopAllRingingAndTones()
        timerJob?.cancel()

        val callId = "call_in_" + UUID.randomUUID().toString().substring(0, 8)
        _activeCallFlow.value = ActiveCallInfo(
            callId = callId,
            remotePhoneNumber = phoneNumber,
            remoteName = callerName,
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            durationSeconds = 0,
            isMuted = false,
            isSpeaker = repository.sessionManager.defaultSpeaker,
            isHold = false,
            dtmfLog = "",
            startTime = System.currentTimeMillis()
        )

        startIncomingRingingLoop()
    }

    fun acceptIncomingCall() {
        val current = _activeCallFlow.value
        if (current.state != CallState.RINGING && current.state != CallState.PREPARING) return

        stopAllRingingAndTones()

        _activeCallFlow.value = current.copy(
            state = CallState.CONNECTED,
            startTime = System.currentTimeMillis()
        )
        voiceClientManager.setSpeakerphoneOn(current.isSpeaker)
        voiceClientManager.setMuted(current.isMuted)
        startCallTimer()
    }

    fun declineIncomingCall() {
        stopAllRingingAndTones()
        val current = _activeCallFlow.value
        saveCallRecord(
            status = CallRecordStatus.NO_ANSWER,
            direction = CallDirection.MISSED,
            duration = 0
        )
        resetCallState(CallState.ENDED)
    }

    fun toggleMute() {
        val current = _activeCallFlow.value
        val newMute = !current.isMuted
        _activeCallFlow.value = current.copy(isMuted = newMute)
        voiceClientManager.setMuted(newMute)
    }

    fun toggleSpeaker() {
        val current = _activeCallFlow.value
        val newSpeaker = !current.isSpeaker
        _activeCallFlow.value = current.copy(isSpeaker = newSpeaker)
        voiceClientManager.setSpeakerphoneOn(newSpeaker)
    }

    fun toggleHold() {
        val current = _activeCallFlow.value
        if (current.state == CallState.CONNECTED) {
            _activeCallFlow.value = current.copy(isHold = true, state = CallState.ON_HOLD)
            voiceClientManager.setMuted(true)
        } else if (current.state == CallState.ON_HOLD) {
            _activeCallFlow.value = current.copy(isHold = false, state = CallState.CONNECTED)
            voiceClientManager.setMuted(current.isMuted)
        }
    }

    fun toggleRecording() {
        val current = _activeCallFlow.value
        if (current.state == CallState.CONNECTED || current.state == CallState.ON_HOLD) {
            val newRecording = !current.isRecording
            _activeCallFlow.value = current.copy(isRecording = newRecording)
            if (newRecording) {
                playTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            } else {
                playTone(ToneGenerator.TONE_PROP_PROMPT, 150)
            }
        }
    }

    fun sendDtmf(digit: Char) {
        val toneType = getDtmfTone(digit)
        playTone(toneType, 180)

        val current = _activeCallFlow.value
        _activeCallFlow.value = current.copy(
            dtmfLog = current.dtmfLog + digit
        )
    }

    fun playKeypadTone(digit: Char) {
        val toneType = getDtmfTone(digit)
        playTone(toneType, 140)
    }

    private fun getDtmfTone(digit: Char): Int {
        return when (digit) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> ToneGenerator.TONE_PROP_BEEP
        }
    }

    fun endCall() {
        stopAllRingingAndTones()
        val current = _activeCallFlow.value
        timerJob?.cancel()

        if (current.state == CallState.IDLE) return

        _activeCallFlow.value = current.copy(state = CallState.ENDING)
        voiceClientManager.disconnect(current.callId)

        saveCallRecord(
            status = if (current.durationSeconds > 0) CallRecordStatus.COMPLETED else CallRecordStatus.CANCELED,
            direction = current.direction,
            duration = current.durationSeconds
        )

        scope.launch {
            delay(400)
            resetCallState(CallState.ENDED)
            delay(300)
            resetCallState(CallState.IDLE)
        }
    }

    fun resetToIdle() {
        stopAllRingingAndTones()
        timerJob?.cancel()
        voiceClientManager.disconnect()
        _activeCallFlow.value = ActiveCallInfo(state = CallState.IDLE)
        restoreAudioSettings()
    }

    private fun startOutgoingRingbackLoop() {
        ringingToneJob?.cancel()
        ringingToneJob = scope.launch {
            while (isActive) {
                try {
                    primaryToneGen?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1200)
                    voiceToneGen?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1200)
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    private fun startIncomingRingingLoop() {
        ringingToneJob?.cancel()
        ringingToneJob = scope.launch {
            try {
                if (incomingRingtone == null) {
                    initRingtone()
                }
                incomingRingtone?.play()
            } catch (_: Exception) {}

            while (isActive) {
                try {
                    if (incomingRingtone == null || incomingRingtone?.isPlaying == false) {
                        primaryToneGen?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
                    }
                } catch (_: Exception) {}
                delay(3200)
            }
        }
    }

    private fun stopAllRingingAndTones() {
        ringingToneJob?.cancel()
        ringingToneJob = null
        try {
            if (incomingRingtone?.isPlaying == true) {
                incomingRingtone?.stop()
            }
        } catch (_: Exception) {}
        try {
            primaryToneGen?.stopTone()
            voiceToneGen?.stopTone()
        } catch (_: Exception) {}
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _activeCallFlow.value
                if (current.state == CallState.CONNECTED || current.state == CallState.ON_HOLD) {
                    val newDuration = current.durationSeconds + 1
                    val newRecDuration = if (current.isRecording) current.recordingDurationSeconds + 1 else current.recordingDurationSeconds
                    _activeCallFlow.value = current.copy(
                        durationSeconds = newDuration,
                        recordingDurationSeconds = newRecDuration
                    )
                }
            }
        }
    }

    private fun saveCallRecord(status: CallRecordStatus, direction: CallDirection, duration: Long) {
        val current = _activeCallFlow.value
        val hasRecording = current.recordingDurationSeconds > 0 || current.isRecording
        val finalRecordingDuration = if (hasRecording && current.recordingDurationSeconds == 0L) duration else current.recordingDurationSeconds
        val record = CallRecord(
            id = "call_" + UUID.randomUUID().toString().substring(0, 10),
            remotePhoneNumber = current.remotePhoneNumber,
            remoteName = current.remoteName,
            direction = direction,
            durationSeconds = duration,
            status = status,
            timestamp = System.currentTimeMillis(),
            twilioCallSid = null,
            isRecorded = hasRecording,
            recordingDurationSeconds = finalRecordingDuration,
            recordingUrl = if (hasRecording) "https://recordings.bizvoice.io/rec_${current.callId}.wav" else null
        )

        scope.launch {
            repository.recordCompletedCall(record)
        }
    }

    private fun restoreAudioSettings() {
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
        } catch (_: Exception) {}
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            primaryToneGen?.startTone(toneType, durationMs)
            voiceToneGen?.startTone(toneType, durationMs)
        } catch (_: Exception) {}
    }

    private fun resetCallState(state: CallState) {
        _activeCallFlow.value = _activeCallFlow.value.copy(state = state)
        if (state == CallState.IDLE || state == CallState.ENDED) {
            restoreAudioSettings()
        }
    }
}
