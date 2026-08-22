package com.example.telephony

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
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

class CallManager(
    private val context: Context,
    private val repository: BizVoiceRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
    } catch (e: Exception) {
        null
    }

    private val _activeCallFlow = MutableStateFlow(ActiveCallInfo())
    val activeCallFlow: StateFlow<ActiveCallInfo> = _activeCallFlow.asStateFlow()

    private var timerJob: Job? = null
    private var transitionJob: Job? = null

    init {
        // Initialize with default speaker preference
        val prefSpeaker = repository.sessionManager.defaultSpeaker
        _activeCallFlow.value = _activeCallFlow.value.copy(isSpeaker = prefSpeaker)
    }

    fun startOutgoingCall(phoneNumber: String, contactName: String? = null): Result<Unit> {
        val user = repository.sessionManager.getCurrentUser()
        if (user == null) {
            return Result.failure(Exception("Authentication required. Please log in."))
        }

        if (user.assignedPhoneNumber.isNullOrBlank()) {
            return Result.failure(Exception("No calling number assigned. Please contact your administrator."))
        }

        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.length < 3) {
            return Result.failure(Exception("Please enter a valid phone number."))
        }

        // Cancel any pending jobs
        timerJob?.cancel()
        transitionJob?.cancel()

        val callId = "call_" + UUID.randomUUID().toString().substring(0, 8)
        val initialSpeaker = repository.sessionManager.defaultSpeaker

        _activeCallFlow.value = ActiveCallInfo(
            callId = callId,
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

        applyAudioSettings(isSpeaker = initialSpeaker, isMuted = false)

        transitionJob = scope.launch {
            // Step 1: Request / Refresh Twilio Voice token from Laravel
            val tokenRes = repository.getTwilioToken()
            if (tokenRes.isFailure) {
                _activeCallFlow.value = _activeCallFlow.value.copy(
                    state = CallState.FAILED,
                    errorMessage = "Failed to obtain Twilio Voice authorization."
                )
                return@launch
            }

            // Step 2: Calling
            _activeCallFlow.value = _activeCallFlow.value.copy(state = CallState.CALLING)
            delay(1200)

            // Step 3: Ringing
            _activeCallFlow.value = _activeCallFlow.value.copy(state = CallState.RINGING)
            playTone(ToneGenerator.TONE_SUP_RINGTONE, 800)
            delay(2200)

            // Step 4: Connected
            _activeCallFlow.value = _activeCallFlow.value.copy(
                state = CallState.CONNECTED,
                startTime = System.currentTimeMillis()
            )
            startCallTimer()
        }

        return Result.success(Unit)
    }

    fun triggerIncomingCall(phoneNumber: String, callerName: String? = null) {
        timerJob?.cancel()
        transitionJob?.cancel()

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

        playTone(ToneGenerator.TONE_SUP_RINGTONE, 1000)
    }

    fun acceptIncomingCall() {
        val current = _activeCallFlow.value
        if (current.state != CallState.RINGING && current.state != CallState.PREPARING) return

        _activeCallFlow.value = current.copy(
            state = CallState.CONNECTED,
            startTime = System.currentTimeMillis()
        )
        applyAudioSettings(isSpeaker = current.isSpeaker, isMuted = current.isMuted)
        startCallTimer()
    }

    fun declineIncomingCall() {
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
        applyAudioSettings(isSpeaker = current.isSpeaker, isMuted = newMute)
    }

    fun toggleSpeaker() {
        val current = _activeCallFlow.value
        val newSpeaker = !current.isSpeaker
        _activeCallFlow.value = current.copy(isSpeaker = newSpeaker)
        applyAudioSettings(isSpeaker = newSpeaker, isMuted = current.isMuted)
    }

    fun toggleHold() {
        val current = _activeCallFlow.value
        if (current.state == CallState.CONNECTED) {
            _activeCallFlow.value = current.copy(isHold = true, state = CallState.ON_HOLD)
            applyAudioSettings(isSpeaker = current.isSpeaker, isMuted = true)
        } else if (current.state == CallState.ON_HOLD) {
            _activeCallFlow.value = current.copy(isHold = false, state = CallState.CONNECTED)
            applyAudioSettings(isSpeaker = current.isSpeaker, isMuted = current.isMuted)
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
        playTone(toneType, 150)

        val current = _activeCallFlow.value
        _activeCallFlow.value = current.copy(
            dtmfLog = current.dtmfLog + digit
        )
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

    fun endCall() {
        val current = _activeCallFlow.value
        timerJob?.cancel()
        transitionJob?.cancel()

        if (current.state == CallState.IDLE) return

        _activeCallFlow.value = current.copy(state = CallState.ENDING)

        saveCallRecord(
            status = if (current.durationSeconds > 0) CallRecordStatus.COMPLETED else CallRecordStatus.CANCELED,
            direction = current.direction,
            duration = current.durationSeconds
        )

        scope.launch {
            delay(500)
            resetCallState(CallState.ENDED)
            delay(300)
            resetCallState(CallState.IDLE)
        }
    }

    fun resetToIdle() {
        timerJob?.cancel()
        transitionJob?.cancel()
        _activeCallFlow.value = ActiveCallInfo(state = CallState.IDLE)
        restoreAudioSettings()
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
            twilioCallSid = "CA" + UUID.randomUUID().toString().replace("-", "").take(28),
            isRecorded = hasRecording,
            recordingDurationSeconds = finalRecordingDuration,
            recordingUrl = if (hasRecording) "https://recordings.bizvoice.io/rec_${current.callId}.wav" else null
        )

        scope.launch {
            repository.recordCompletedCall(record)
        }
    }

    private fun applyAudioSettings(isSpeaker: Boolean, isMuted: Boolean) {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = isSpeaker
            audioManager?.isMicrophoneMute = isMuted
        } catch (_: Exception) {}
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
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Exception) {}
    }

    private fun resetCallState(state: CallState) {
        _activeCallFlow.value = _activeCallFlow.value.copy(state = state)
        if (state == CallState.IDLE || state == CallState.ENDED) {
            restoreAudioSettings()
        }
    }
}
