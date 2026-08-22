package com.example.ui.screens.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telephony.CallManager
import com.example.telephony.CallState
import com.example.ui.components.BizAvatar
import com.example.ui.theme.CallHoldAmber
import com.example.ui.theme.CallRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCallScreen(
    callManager: CallManager
) {
    val activeCall by callManager.activeCallFlow.collectAsState()
    var showInCallKeypad by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (activeCall.state == CallState.CONNECTED) 1.08f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val formattedDuration = remember(activeCall.durationSeconds) {
        val minutes = activeCall.durationSeconds / 60
        val seconds = activeCall.durationSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    val stateLabel = when (activeCall.state) {
        CallState.PREPARING -> "Authorizing VoIP Line..."
        CallState.CALLING -> "Calling..."
        CallState.RINGING -> "Ringing..."
        CallState.CONNECTED -> formattedDuration
        CallState.ON_HOLD -> "Call On Hold"
        CallState.RECONNECTING -> "Reconnecting..."
        CallState.ENDING -> "Ending Call..."
        CallState.ENDED -> "Call Ended"
        CallState.FAILED -> "Call Failed"
        CallState.IDLE -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("active_call_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Caller info & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Animated Caller Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    if (activeCall.state == CallState.CONNECTED || activeCall.state == CallState.RINGING) {
                        Surface(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale),
                            shape = CircleShape,
                            color = if (activeCall.state == CallState.ON_HOLD) CallHoldAmber.copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f)
                        ) {}
                    }
                    BizAvatar(
                        name = activeCall.remoteName ?: activeCall.remotePhoneNumber,
                        size = 88
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = activeCall.remoteName ?: activeCall.remotePhoneNumber,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                if (activeCall.remoteName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeCall.remotePhoneNumber,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Call State / Duration Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (activeCall.state) {
                        CallState.ON_HOLD -> CallHoldAmber.copy(alpha = 0.3f)
                        CallState.FAILED -> CallRed.copy(alpha = 0.3f)
                        else -> Color.White.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = stateLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (activeCall.state) {
                            CallState.ON_HOLD -> Color(0xFFFCD34D)
                            CallState.FAILED -> Color(0xFFFCA5A5)
                            else -> Color(0xFF38BDF8)
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Active Recording Status Indicator
                if (activeCall.isRecording) {
                    val recMinutes = activeCall.recordingDurationSeconds / 60
                    val recSeconds = activeCall.recordingDurationSeconds % 60
                    val formattedRec = String.format(Locale.getDefault(), "%02d:%02d", recMinutes, recSeconds)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REC $formattedRec",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                if (activeCall.dtmfLog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DTMF Digits: ${activeCall.dtmfLog}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Error Recovery Options (If Call Failed)
            if (activeCall.state == CallState.FAILED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeCall.errorMessage ?: "Unable to connect the call.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { callManager.resetToIdle() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Close")
                            }
                            Button(
                                onClick = {
                                    val phone = activeCall.remotePhoneNumber
                                    val name = activeCall.remoteName
                                    callManager.startOutgoingCall(phone, name)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            } else {
                // Controls Grid (Mute, Speaker, Record, Hold, Keypad)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (activeCall.isMuted) "Muted" else "Mute",
                            isActive = activeCall.isMuted,
                            onClick = { callManager.toggleMute() },
                            testTag = "call_control_mute"
                        )

                        InCallControlButton(
                            icon = if (activeCall.isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label = if (activeCall.isSpeaker) "Speaker ON" else "Speaker",
                            isActive = activeCall.isSpeaker,
                            onClick = { callManager.toggleSpeaker() },
                            testTag = "call_control_speaker"
                        )

                        InCallControlButton(
                            icon = Icons.Default.FiberManualRecord,
                            label = if (activeCall.isRecording) "Recording" else "Record",
                            isActive = activeCall.isRecording,
                            activeColor = Color(0xFFEF4444),
                            onClick = { callManager.toggleRecording() },
                            testTag = "call_control_record"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (activeCall.isHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (activeCall.isHold) "Resume" else "Hold",
                            isActive = activeCall.isHold,
                            activeColor = CallHoldAmber,
                            onClick = { callManager.toggleHold() },
                            testTag = "call_control_hold"
                        )

                        InCallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = showInCallKeypad,
                            onClick = { showInCallKeypad = true },
                            testTag = "call_control_keypad"
                        )
                    }
                }
            }

            // Bottom End Call Button
            Surface(
                onClick = { callManager.endCall() },
                shape = CircleShape,
                color = CallRed,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("end_call_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }

    // In-Call DTMF Keypad Bottom Sheet
    if (showInCallKeypad) {
        ModalBottomSheet(
            onDismissRequest = { showInCallKeypad = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DTMF Keypad",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                val dtmfDigits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (row in dtmfDigits) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (d in row) {
                                Surface(
                                    onClick = { callManager.sendDtmf(d.first()) },
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = d,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showInCallKeypad = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hide Keypad")
                }
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = Color(0xFF0284C7),
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(62.dp)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
