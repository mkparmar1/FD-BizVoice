package com.example.ui.screens.recents

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.model.Contact
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager
import com.example.ui.components.BizAvatar
import com.example.ui.components.BizTopAppBar
import com.example.ui.components.CallDirectionIcon
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallDetailScreen(
    callId: String,
    repository: BizVoiceRepository,
    callManager: CallManager,
    onNavigateBack: () -> Unit,
    onNavigateToAddContact: (String) -> Unit,
    onNavigateToContactDetail: (String) -> Unit
) {
    var callRecord by remember { mutableStateOf<CallRecord?>(null) }
    var existingContact by remember { mutableStateOf<Contact?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(callId) {
        val record = repository.getCallDetail(callId)
        callRecord = record
        if (record != null) {
            existingContact = repository.checkContactByPhone(record.remotePhoneNumber)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            BizTopAppBar(
                title = "Call Details",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (callRecord == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Call details not found")
            }
        } else {
            val call = callRecord!!
            val formattedTime = remember(call.timestamp) {
                val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                sdf.format(Date(call.timestamp))
            }

            val formattedDuration = remember(call.durationSeconds) {
                val mins = call.durationSeconds / 60
                val secs = call.durationSeconds % 60
                "${mins}m ${secs}s"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar & Name
                BizAvatar(
                    name = call.remoteName ?: call.remotePhoneNumber,
                    size = 80
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = call.remoteName ?: call.remotePhoneNumber,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (call.remoteName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = call.remotePhoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            callManager.startOutgoingCall(call.remotePhoneNumber, call.remoteName)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CallGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("call_detail_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Again", fontWeight = FontWeight.SemiBold)
                    }

                    if (existingContact != null) {
                        OutlinedButton(
                            onClick = { onNavigateToContactDetail(existingContact!!.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("call_detail_view_contact_button")
                        ) {
                            Text("View Contact", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onNavigateToAddContact(call.remotePhoneNumber) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("call_detail_add_contact_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Contact", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Call Recording Player Card (if call is recorded)
                if (call.isRecorded || call.recordingDurationSeconds > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CallRecordingPlayerCard(
                        recordingDurationSeconds = if (call.recordingDurationSeconds > 0) call.recordingDurationSeconds else call.durationSeconds,
                        recordingUrl = call.recordingUrl
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Metadata Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "CALL INFORMATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        DetailRow(
                            icon = Icons.Default.Phone,
                            label = "Direction",
                            value = when (call.direction) {
                                CallDirection.INCOMING -> "Incoming Call"
                                CallDirection.OUTGOING -> "Outgoing Call"
                                CallDirection.MISSED -> "Missed Call"
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        DetailRow(
                            icon = Icons.Default.Info,
                            label = "Call Status",
                            value = when (call.status) {
                                CallRecordStatus.NO_ANSWER -> "No Answer"
                                CallRecordStatus.BUSY -> "Line Busy"
                                CallRecordStatus.FAILED -> "Call Failed"
                                CallRecordStatus.CANCELED -> "Canceled / Declined"
                                CallRecordStatus.COMPLETED -> "Completed"
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        DetailRow(
                            icon = Icons.Default.AccessTime,
                            label = "Date & Time",
                            value = formattedTime
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        DetailRow(
                            icon = Icons.Default.Timer,
                            label = "Duration",
                            value = if (call.direction == CallDirection.MISSED || call.durationSeconds == 0L) "0s (Unanswered)" else formattedDuration
                        )

                        if (call.isRecorded) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DetailRow(
                                icon = Icons.Default.FiberManualRecord,
                                label = "Call Recording",
                                value = "Audio Recorded (${if (call.recordingDurationSeconds > 0) call.recordingDurationSeconds else call.durationSeconds}s)"
                            )
                        }

                        if (call.twilioCallSid != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            DetailRow(
                                icon = Icons.Default.Fingerprint,
                                label = "VoIP Call SID",
                                value = call.twilioCallSid
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRecordingPlayerCard(
    recordingDurationSeconds: Long,
    recordingUrl: String?
) {
    val totalSeconds = if (recordingDurationSeconds > 0) recordingDurationSeconds else 30L
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    // Playback timer simulation
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (isPlaying && currentProgressSeconds < totalSeconds) {
                delay(100)
                currentProgressSeconds += 0.1f * playbackSpeed
                if (currentProgressSeconds >= totalSeconds) {
                    currentProgressSeconds = totalSeconds.toFloat()
                    isPlaying = false
                }
            }
        }
    }

    val elapsedMinutes = (currentProgressSeconds.toLong()) / 60
    val elapsedSecs = (currentProgressSeconds.toLong()) % 60
    val totalMinutes = totalSeconds / 60
    val totalSecs = totalSeconds % 60
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", elapsedMinutes, elapsedSecs, totalMinutes, totalSecs)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("call_recording_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALL RECORDING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFEF4444)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "HD Voice • WAV",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Audio Waveform Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val waveHeights = listOf(
                    0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f, 0.7f, 1.0f, 0.6f, 0.4f,
                    0.85f, 0.5f, 0.95f, 0.7f, 0.3f, 0.65f, 0.9f, 0.4f, 0.8f, 0.55f,
                    0.75f, 0.35f, 0.6f, 0.85f, 0.45f, 0.9f, 0.5f, 0.7f, 0.4f, 0.6f
                )

                val progressFraction = (currentProgressSeconds / totalSeconds).coerceIn(0f, 1f)

                waveHeights.forEachIndexed { i, factor ->
                    val barProgress = i.toFloat() / waveHeights.size
                    val isPast = barProgress <= progressFraction
                    val dynamicHeight = if (isPlaying) {
                        (factor * (if (i % 2 == 0) waveOffset else (1.4f - waveOffset))).coerceIn(0.2f, 1.0f)
                    } else {
                        factor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp)
                            .height((36 * dynamicHeight).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Slider
            Slider(
                value = currentProgressSeconds,
                onValueChange = { newValue ->
                    currentProgressSeconds = newValue
                },
                valueRange = 0f..totalSeconds.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recording_playback_slider")
            )

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Playback Speed Button
                    Surface(
                        onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("recording_speed_button")
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Play / Pause Button
                    Surface(
                        onClick = {
                            if (currentProgressSeconds >= totalSeconds) {
                                currentProgressSeconds = 0f
                            }
                            isPlaying = !isPlaying
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("recording_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Recording" else "Play Recording",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

