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
    private var autoDismissJob: Job? = null
    private val savedSessionIds = mutableSetOf<String>()

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
                autoDismissJob?.cancel()
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    callId = sessionId,
                    state = CallState.CALLING,
                    statusTitle = "Calling...",
                    statusSubtitle = null,
                    errorMessage = null
                )
                playKeypadTone('1')
            }

            override fun onRinging(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Ringing session $sessionId")
                autoDismissJob?.cancel()
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    callId = sessionId,
                    state = CallState.RINGING,
                    statusTitle = "Ringing...",
                    statusSubtitle = null
                )
                startOutgoingRingbackLoop()
            }

            override fun onConnected(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Connected session $sessionId")
                autoDismissJob?.cancel()
                stopAllRingingAndTones()
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    callId = sessionId,
                    state = CallState.CONNECTED,
                    statusTitle = null,
                    statusSubtitle = null,
                    errorMessage = null,
                    startTime = System.currentTimeMillis()
                )
                startCallTimer()
            }

            override fun onReconnecting(sessionId: String) {
                Log.i(TAG, "Voice Client Event: Reconnecting session $sessionId")
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    state = CallState.RECONNECTING,
                    statusTitle = "Reconnecting VoIP..."
                )
            }

            override fun onDisconnected(sessionId: String, durationSeconds: Long, reason: DisconnectReason) {
                Log.i(TAG, "Voice Client Event: Disconnected session $sessionId (duration: ${durationSeconds}s, reason: ${reason.title} - ${reason.userFriendlyMessage})")
                handleCallDisconnected(durationSeconds, reason)
            }

            override fun onConnectFailure(sessionId: String, reason: DisconnectReason) {
                Log.e(TAG, "Voice Client Event: Connect failure session $sessionId: ${reason.title} - ${reason.userFriendlyMessage}")
                handleConnectFailure(reason)
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
     */
    fun startOutgoingCall(phoneNumber: String, contactName: String? = null): Result<Unit> {
        val formattedNumber = PhoneNumberFormatter.formatToE164(phoneNumber, context = context)
        val cleanNumber = formattedNumber.trim()
        if (cleanNumber.length < 3) {
            return Result.failure(Exception("Please enter a valid phone number."))
        }

        // Cancel any pending tones & timer
        stopAllRingingAndTones()
        timerJob?.cancel()
        autoDismissJob?.cancel()

        val initialSpeaker = repository.sessionManager.defaultSpeaker
        val sessionId = "call_out_" + UUID.randomUUID().toString().substring(0, 8)

        _activeCallFlow.value = ActiveCallInfo(
            callId = sessionId,
            remotePhoneNumber = cleanNumber,
            remoteName = contactName,
            direction = CallDirection.OUTGOING,
            state = CallState.PREPARING,
            durationSeconds = 0,
            isMuted = false,
            isSpeaker = initialSpeaker,
            isHold = false,
            dtmfLog = "",
            statusTitle = "Authorizing VoIP Line...",
            statusSubtitle = null,
            errorMessage = null,
            startTime = System.currentTimeMillis()
        )

        voiceClientManager.setSpeakerphoneOn(initialSpeaker)
        voiceClientManager.setMuted(false)

        // Connect through client leg
        voiceClientManager.connectOutbound(cleanNumber) { result ->
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unable to connect call"
                stopAllRingingAndTones()
                val friendlyReason = DisconnectReason(
                    title = "Unable to Place Call",
                    userFriendlyMessage = errorMsg
                )
                handleConnectFailure(friendlyReason)
            }
        }

        return Result.success(Unit)
    }

    private fun handleCallDisconnected(finalDuration: Long, reason: DisconnectReason) {
        stopAllRingingAndTones()
        timerJob?.cancel()
        autoDismissJob?.cancel()

        val current = _activeCallFlow.value
        if (current.state == CallState.IDLE) return

        val duration = maxOf(current.durationSeconds, finalDuration)

        val (nextState, recordStatus) = when {
            reason.isNoAnswer -> {
                playTone(ToneGenerator.TONE_PROP_PROMPT, 300)
                Pair(CallState.NO_ANSWER, CallRecordStatus.NO_ANSWER)
            }
            reason.isBusy -> {
                playTone(ToneGenerator.TONE_SUP_BUSY, 800)
                Pair(CallState.BUSY, CallRecordStatus.BUSY)
            }
            reason.isRejected -> {
                playTone(ToneGenerator.TONE_PROP_NACK, 400)
                Pair(CallState.REJECTED, CallRecordStatus.CANCELED)
            }
            duration > 0 -> {
                playTone(ToneGenerator.TONE_PROP_PROMPT, 300)
                Pair(CallState.ENDED, CallRecordStatus.COMPLETED)
            }
            reason.isNormalHangup -> {
                playTone(ToneGenerator.TONE_PROP_PROMPT, 300)
                Pair(CallState.ENDED, CallRecordStatus.CANCELED)
            }
            else -> {
                playTone(ToneGenerator.TONE_PROP_NACK, 400)
                Pair(CallState.FAILED, CallRecordStatus.FAILED)
            }
        }

        saveCallRecord(
            status = recordStatus,
            direction = current.direction,
            duration = duration
        )

        _activeCallFlow.value = current.copy(
            state = nextState,
            durationSeconds = duration,
            statusTitle = reason.title,
            statusSubtitle = reason.userFriendlyMessage,
            errorMessage = if (nextState == CallState.FAILED) reason.userFriendlyMessage else null
        )

        // Automatically dismiss the call screen after allowing the user to view the friendly result
        autoDismissJob = scope.launch {
            val dismissDelay = when (nextState) {
                CallState.ENDED -> 1500L
                CallState.NO_ANSWER, CallState.BUSY, CallState.REJECTED -> 4000L
                else -> 4500L
            }
            delay(dismissDelay)
            resetCallState(CallState.IDLE)
        }
    }

    private fun handleConnectFailure(reason: DisconnectReason) {
        stopAllRingingAndTones()
        timerJob?.cancel()
        autoDismissJob?.cancel()

        val current = _activeCallFlow.value
        playTone(ToneGenerator.TONE_PROP_NACK, 500)

        val nextState = when {
            reason.isNoAnswer -> CallState.NO_ANSWER
            reason.isBusy -> CallState.BUSY
            reason.isRejected -> CallState.REJECTED
            else -> CallState.FAILED
        }

        val recordStatus = when (nextState) {
            CallState.NO_ANSWER -> CallRecordStatus.NO_ANSWER
            CallState.BUSY -> CallRecordStatus.BUSY
            CallState.REJECTED -> CallRecordStatus.CANCELED
            else -> CallRecordStatus.FAILED
        }

        saveCallRecord(
            status = recordStatus,
            direction = current.direction,
            duration = 0
        )

        _activeCallFlow.value = current.copy(
            state = nextState,
            statusTitle = reason.title,
            statusSubtitle = reason.userFriendlyMessage,
            errorMessage = reason.userFriendlyMessage
        )

        autoDismissJob = scope.launch {
            delay(4000)
            resetCallState(CallState.IDLE)
        }
    }

    fun triggerIncomingCall(phoneNumber: String, callerName: String? = null) {
        stopAllRingingAndTones()
        timerJob?.cancel()
        autoDismissJob?.cancel()

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
            statusTitle = "Incoming Call...",
            statusSubtitle = null,
            errorMessage = null,
            startTime = System.currentTimeMillis()
        )

        startIncomingRingingLoop()
    }

    fun acceptIncomingCall() {
        val current = _activeCallFlow.value
        if (current.state != CallState.RINGING && current.state != CallState.PREPARING) return

        stopAllRingingAndTones()
        autoDismissJob?.cancel()

        _activeCallFlow.value = current.copy(
            state = CallState.CONNECTED,
            statusTitle = null,
            statusSubtitle = null,
            startTime = System.currentTimeMillis()
        )

        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            voiceClientManager.setSpeakerphoneOn(current.isSpeaker)
        } catch (_: Exception) {}

        startCallTimer()
    }

    fun declineIncomingCall() {
        val current = _activeCallFlow.value
        stopAllRingingAndTones()
        autoDismissJob?.cancel()

        saveCallRecord(
            status = CallRecordStatus.NO_ANSWER,
            direction = CallDirection.MISSED,
            duration = 0
        )

        _activeCallFlow.value = current.copy(
            state = CallState.ENDED,
            statusTitle = "Call Declined",
            statusSubtitle = null
        )

        scope.launch {
            delay(600)
            resetCallState(CallState.IDLE)
        }
    }

    fun toggleMute(): Boolean {
        val current = _activeCallFlow.value
        val newMute = !current.isMuted
        _activeCallFlow.value = current.copy(isMuted = newMute)
        voiceClientManager.setMuted(newMute)
        try {
            audioManager?.isMicrophoneMute = newMute
        } catch (_: Exception) {}
        return newMute
    }

    fun toggleSpeaker(): Boolean {
        val current = _activeCallFlow.value
        val newSpeaker = !current.isSpeaker
        _activeCallFlow.value = current.copy(isSpeaker = newSpeaker)
        voiceClientManager.setSpeakerphoneOn(newSpeaker)
        return newSpeaker
    }

    fun toggleHold(): Boolean {
        val current = _activeCallFlow.value
        val newHold = !current.isHold
        val newState = if (newHold) CallState.ON_HOLD else CallState.CONNECTED
        _activeCallFlow.value = current.copy(isHold = newHold, state = newState)
        voiceClientManager.setMuted(newHold)

        if (newHold) {
            playTone(ToneGenerator.TONE_SUP_CONFIRM, 200)
        } else {
            playTone(ToneGenerator.TONE_SUP_CONFIRM, 200)
        }
        return newHold
    }

    fun toggleRecording(): Boolean {
        val current = _activeCallFlow.value
        val newRec = !current.isRecording
        _activeCallFlow.value = current.copy(isRecording = newRec)
        if (newRec) {
            playTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        }
        return newRec
    }

    fun sendDtmf(digit: Char) {
        val current = _activeCallFlow.value
        val updatedLog = current.dtmfLog + digit
        _activeCallFlow.value = current.copy(dtmfLog = updatedLog)
        playKeypadTone(digit)
    }

    fun playKeypadTone(digit: Char) {
        val toneType = when (digit) {
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
        playTone(toneType, 120)
    }

    fun endActiveCall() {
        stopAllRingingAndTones()
        autoDismissJob?.cancel()
        val current = _activeCallFlow.value
        timerJob?.cancel()

        if (current.state == CallState.IDLE) return

        _activeCallFlow.value = current.copy(
            state = CallState.ENDING,
            statusTitle = "Ending Call..."
        )
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

    fun endCall() = endActiveCall()

    fun retryLastCall() {
        autoDismissJob?.cancel()
        val current = _activeCallFlow.value
        val phone = current.remotePhoneNumber
        val name = current.remoteName
        if (phone.isNotBlank()) {
            resetToIdle()
            startOutgoingCall(phone, name)
        }
    }

    fun resetToIdle() {
        stopAllRingingAndTones()
        timerJob?.cancel()
        autoDismissJob?.cancel()
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
        if (current.remotePhoneNumber.isBlank()) return

        val sessionId = current.callId
        if (sessionId.isNotBlank() && savedSessionIds.contains(sessionId)) {
            Log.d(TAG, "Call session $sessionId already recorded, skipping duplicate.")
            return
        }
        if (sessionId.isNotBlank()) {
            savedSessionIds.add(sessionId)
        }

        val recordId = if (sessionId.isNotBlank()) sessionId else "call_" + UUID.randomUUID().toString().substring(0, 10)
        val hasRecording = current.recordingDurationSeconds > 0 || current.isRecording
        val finalRecordingDuration = if (hasRecording && current.recordingDurationSeconds == 0L) duration else current.recordingDurationSeconds
        val record = CallRecord(
            id = recordId,
            remotePhoneNumber = current.remotePhoneNumber,
            remoteName = current.remoteName,
            direction = direction,
            durationSeconds = duration,
            status = status,
            timestamp = if (current.startTime > 0) current.startTime else System.currentTimeMillis(),
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
