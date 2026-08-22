package com.example.telephony

import com.example.data.model.CallDirection

enum class CallState {
    IDLE,
    PREPARING,
    CALLING,
    RINGING,
    CONNECTED,
    ON_HOLD,
    RECONNECTING,
    ENDING,
    ENDED,
    FAILED
}

data class ActiveCallInfo(
    val callId: String = "",
    val remotePhoneNumber: String = "",
    val remoteName: String? = null,
    val direction: CallDirection = CallDirection.OUTGOING,
    val state: CallState = CallState.IDLE,
    val durationSeconds: Long = 0,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isHold: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Long = 0,
    val dtmfLog: String = "",
    val errorMessage: String? = null,
    val startTime: Long = 0
)
