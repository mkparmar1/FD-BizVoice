package com.example.ui.screens.dialer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Contact
import com.example.data.model.DialingCountry
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager
import com.example.telephony.CallState
import com.example.telephony.PhoneNumberFormatter
import com.example.ui.components.AssignedNumberBanner
import com.example.ui.components.BizAvatar
import com.example.ui.components.MainTabHeader
import com.example.ui.theme.CallGreen

data class KeypadKey(val digit: String, val letters: String)

@Composable
fun DialerScreen(
    repository: BizVoiceRepository,
    callManager: CallManager,
    onNavigateToContact: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by repository.currentUserFlow.collectAsState()
    val allContacts by repository.allContactsFlow.collectAsState()
    val activeCall by callManager.activeCallFlow.collectAsState()

    val dialingCountries by repository.dialingCountriesFlow.collectAsState()
    val selectedCountry by repository.selectedDialerCountryFlow.collectAsState()

    var enteredNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }

    var pendingNumberToCall by remember { mutableStateOf<String?>(null) }
    var pendingContactNameToCall by remember { mutableStateOf<String?>(null) }

    // Task 4: The "+" Rule
    // If typed input starts with '+', auto-detect country and update picker
    LaunchedEffect(enteredNumber) {
        val cleaned = PhoneNumberFormatter.cleanRawInput(enteredNumber)
        if (cleaned.startsWith("+") && cleaned.length >= 2) {
            val detected = PhoneNumberFormatter.detectCountryFromE164(cleaned, context, dialingCountries)
            if (detected != null && detected.isoCode != selectedCountry.isoCode) {
                repository.setSelectedDialerCountry(detected)
            }
        }
    }

    // Task 6: Validate assembled number before dialing
    val validationResult = remember(enteredNumber, selectedCountry) {
        PhoneNumberFormatter.validateNumber(enteredNumber, selectedCountry, context)
    }

    val assignedPhone = currentUser?.assignedPhoneNumber
    val isNumberAvailable = !assignedPhone.isNullOrBlank()
    val isCallAllowed = isNumberAvailable && validationResult.isValid && selectedCountry.enabled

    fun executeCall(numberToCall: String, contactName: String?) {
        errorMessage = null
        enteredNumber = ""
        val result = callManager.startOutgoingCall(numberToCall, contactName)
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingNumberToCall?.let { num ->
                executeCall(num, pendingContactNameToCall)
            }
        } else {
            errorMessage = "Microphone permission is required for voice calls."
        }
        pendingNumberToCall = null
        pendingContactNameToCall = null
    }

    fun initiateCallWithPermission(numberToCall: String, contactName: String?) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            executeCall(numberToCall, contactName)
        } else {
            pendingNumberToCall = numberToCall
            pendingContactNameToCall = contactName
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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

    // Quick contact match based on entered digits or assembled number
    val matchingContacts = remember(enteredNumber, allContacts, validationResult.assembledE164) {
        if (enteredNumber.length >= 2) {
            val cleanQuery = enteredNumber.filter { it.isDigit() }
            val assembledDigits = validationResult.assembledE164.filter { it.isDigit() }
            allContacts.filter { contact ->
                val cleanPhone = contact.phoneNumber.filter { it.isDigit() }
                cleanPhone.contains(cleanQuery) ||
                (assembledDigits.isNotEmpty() && cleanPhone.contains(assembledDigits)) ||
                contact.name.contains(enteredNumber, ignoreCase = true)
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
                            initiateCallWithPermission(contact.phoneNumber, contact.name)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.weight(0.4f))

        // Number Input Row with Country Selector (Left), Number Display (Center), and Backspace (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country Selector Button (Task 3 & 4)
            Surface(
                onClick = { showCountryPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .testTag("country_picker_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = selectedCountry.flagEmoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCountry.callingCode,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select country",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Number Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (enteredNumber.isEmpty()) "Enter Number" else enteredNumber,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = if (enteredNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialer_number_display")
                )
            }

            // Backspace Button
            if (enteredNumber.isNotEmpty()) {
                IconButton(
                    onClick = {
                        if (enteredNumber.isNotEmpty()) {
                            enteredNumber = enteredNumber.dropLast(1)
                            callManager.playKeypadTone('9')
                        }
                    },
                    modifier = Modifier.testTag("dialer_backspace_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Inline Helper / Validation Text (Task 6)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp)
                .height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!selectedCountry.enabled) {
                Text(
                    text = "Calling ${selectedCountry.name} is not enabled on this account",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("dialer_helper_text")
                )
            } else if (enteredNumber.isNotEmpty()) {
                if (!validationResult.isValid && validationResult.helperText != null) {
                    Text(
                        text = validationResult.helperText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dialer_helper_text")
                    )
                } else if (validationResult.isValid) {
                    Text(
                        text = "Dial: ${validationResult.assembledE164}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = CallGreen,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dialer_helper_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Call Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Invisible placeholder for balanced symmetrical layout
            Spacer(modifier = Modifier.size(52.dp))

            // Main Call Button (Enabled only if valid number, enabled country, & line available)
            Surface(
                onClick = {
                    if (!isNumberAvailable) {
                        errorMessage = "Your calling number is currently unavailable."
                    } else if (!selectedCountry.enabled) {
                        errorMessage = "Calling ${selectedCountry.name} is not enabled on this account."
                    } else if (enteredNumber.isBlank()) {
                        errorMessage = "Please enter a phone number to call."
                    } else if (!validationResult.isValid) {
                        errorMessage = validationResult.helperText ?: "Please enter a valid phone number for ${selectedCountry.name}."
                    } else {
                        val numberToCall = validationResult.assembledE164
                        val matched = allContacts.firstOrNull {
                            it.phoneNumber == numberToCall ||
                            it.phoneNumber.filter { d -> d.isDigit() } == enteredNumber.filter { d -> d.isDigit() }
                        }
                        initiateCallWithPermission(numberToCall, matched?.name)
                    }
                },
                shape = CircleShape,
                color = if (isCallAllowed) CallGreen else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (isCallAllowed) 6.dp else 0.dp,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("dialer_call_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCallAllowed) Icons.Default.Call else Icons.Default.PhoneDisabled,
                        contentDescription = "Call",
                        tint = if (isCallAllowed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Clear All Button (or spacer if empty)
            if (enteredNumber.isNotEmpty()) {
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
            } else {
                Spacer(modifier = Modifier.size(52.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Country Picker Bottom Sheet (Task 3)
    if (showCountryPicker) {
        CountryPickerBottomSheet(
            countries = dialingCountries,
            selectedCountry = selectedCountry,
            onCountrySelected = { country ->
                repository.setSelectedDialerCountry(country)
            },
            onDismiss = { showCountryPicker = false }
        )
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
