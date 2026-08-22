package com.example.ui.screens.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager
import com.example.telephony.CallState
import com.example.ui.components.AssignedNumberBanner
import com.example.ui.components.BizAvatar
import com.example.ui.components.MainTabHeader
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed

data class KeypadKey(val digit: String, val letters: String)

@Composable
fun DialerScreen(
    repository: BizVoiceRepository,
    callManager: CallManager,
    onNavigateToContact: (String) -> Unit
) {
    val currentUser by repository.currentUserFlow.collectAsState()
    val allContacts by repository.allContactsFlow.collectAsState()
    val activeCall by callManager.activeCallFlow.collectAsState()

    var enteredNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Automatically clear dial pad when a call connects, ends, or returns to idle
    LaunchedEffect(activeCall.state) {
        if (activeCall.state == CallState.CALLING ||
            activeCall.state == CallState.CONNECTED ||
            activeCall.state == CallState.ENDED ||
            activeCall.state == CallState.IDLE
        ) {
            if (activeCall.remotePhoneNumber.isNotEmpty() && enteredNumber.isNotEmpty()) {
                enteredNumber = ""
                errorMessage = null
            }
        }
    }

    // Quick contact match based on entered number
    val matchingContacts = remember(enteredNumber, allContacts) {
        if (enteredNumber.length >= 2) {
            val cleanQuery = enteredNumber.filter { it.isDigit() }
            allContacts.filter { contact ->
                val cleanPhone = contact.phoneNumber.filter { it.isDigit() }
                cleanPhone.contains(cleanQuery) || contact.name.contains(enteredNumber, ignoreCase = true)
            }.take(4)
        } else {
            emptyList()
        }
    }

    val keys = listOf(
        KeypadKey("1", ""),
        KeypadKey("2", "A B C"),
        KeypadKey("3", "D E F"),
        KeypadKey("4", "G H I"),
        KeypadKey("5", "J K L"),
        KeypadKey("6", "M N O"),
        KeypadKey("7", "P Q R S"),
        KeypadKey("8", "T U V"),
        KeypadKey("9", "W X Y Z"),
        KeypadKey("*", ""),
        KeypadKey("0", "+"),
        KeypadKey("#", "")
    )

    val assignedPhone = currentUser?.assignedPhoneNumber
    val isNumberAvailable = !assignedPhone.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dialer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Uniform Top Header
        MainTabHeader(
            title = "Keypad",
            subtitle = if (isNumberAvailable) "Ready for calls" else "No phone line"
        )

        // Top Assigned Number Display
        AssignedNumberBanner(
            phoneNumber = assignedPhone,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // Error / Alert Banner
        AnimatedVisibility(visible = errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Quick matching contacts bar
        if (matchingContacts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matchingContacts) { contact ->
                    Surface(
                        onClick = {
                            enteredNumber = contact.phoneNumber
                            errorMessage = null
                            callManager.startOutgoingCall(contact.phoneNumber, contact.name)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            BizAvatar(name = contact.name, size = 24)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Entered Number Display & Backspace
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (enteredNumber.isEmpty()) "Enter Number" else enteredNumber,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = if (enteredNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .testTag("dialer_number_display")
            )

            if (enteredNumber.isNotEmpty()) {
                IconButton(
                    onClick = {
                        if (enteredNumber.isNotEmpty()) {
                            enteredNumber = enteredNumber.dropLast(1)
                            callManager.playKeypadTone('9')
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("dialer_backspace_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // 3x4 Keypad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (rowIndex in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (colIndex in 0..2) {
                        val key = keys[rowIndex * 3 + colIndex]
                        KeypadButton(
                            key = key,
                            onClick = {
                                enteredNumber += key.digit
                                callManager.sendDtmf(key.digit.first())
                                errorMessage = null
                            },
                            onLongClick = {
                                if (key.digit == "0") {
                                    enteredNumber += "+"
                                    callManager.sendDtmf('0')
                                    errorMessage = null
                                } else if (key.digit == "1") {
                                    enteredNumber = ""
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Call Action & Test Incoming Simulation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Test Incoming Call Trigger
            Surface(
                onClick = {
                    val sampleName = if (allContacts.isNotEmpty()) allContacts.random().name else "Sarah Jenkins"
                    val samplePhone = if (allContacts.isNotEmpty()) allContacts.random().phoneNumber else "+1 (415) 555-0102"
                    callManager.triggerIncomingCall(samplePhone, sampleName)
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("test_incoming_call_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = "Simulate Incoming Call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Main Green Call Button
            Surface(
                onClick = {
                    if (!isNumberAvailable) {
                        errorMessage = "Your calling number is currently unavailable."
                    } else if (enteredNumber.isBlank()) {
                        errorMessage = "Please enter a phone number to call."
                    } else {
                        errorMessage = null
                        val numberToCall = enteredNumber
                        val matched = allContacts.firstOrNull { it.phoneNumber == numberToCall }
                        enteredNumber = ""
                        val result = callManager.startOutgoingCall(numberToCall, matched?.name)
                        if (result.isFailure) {
                            errorMessage = result.exceptionOrNull()?.message
                        }
                    }
                },
                shape = CircleShape,
                color = if (isNumberAvailable) CallGreen else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (isNumberAvailable) 6.dp else 0.dp,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("dialer_call_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isNumberAvailable) Icons.Default.Call else Icons.Default.PhoneDisabled,
                        contentDescription = "Call",
                        tint = if (isNumberAvailable) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Clear All Button
            Surface(
                onClick = {
                    enteredNumber = ""
                    errorMessage = null
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("dialer_clear_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "C",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_key_${key.digit}"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
