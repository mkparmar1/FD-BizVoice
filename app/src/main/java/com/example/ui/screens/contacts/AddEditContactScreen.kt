package com.example.ui.screens.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Contact
import com.example.data.repository.BizVoiceRepository
import com.example.ui.components.BizTopAppBar
import com.example.ui.theme.CallHoldAmber
import com.example.ui.theme.CallHoldAmberContainer
import kotlinx.coroutines.launch

@Composable
fun AddEditContactScreen(
    contactId: String? = null,
    initialPhoneNumber: String? = null,
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allContacts by repository.allContactsFlow.collectAsState(initial = emptyList())

    val existingContact = remember(allContacts, contactId) {
        if (contactId != null) allContacts.firstOrNull { it.id == contactId } else null
    }

    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(existingContact?.phoneNumber ?: initialPhoneNumber ?: "") }
    var email by remember { mutableStateOf(existingContact?.email ?: "") }
    var organization by remember { mutableStateOf(existingContact?.organization ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingContact) {
        if (existingContact != null) {
            name = existingContact.name
            phoneNumber = existingContact.phoneNumber
            email = existingContact.email ?: ""
            organization = existingContact.organization ?: ""
        }
    }

    // Duplicate detection
    val isDuplicateNumber = remember(phoneNumber, allContacts, contactId) {
        if (phoneNumber.isNotBlank()) {
            val cleanCurrent = phoneNumber.filter { it.isDigit() }
            allContacts.any {
                it.id != contactId && it.phoneNumber.filter { c -> c.isDigit() } == cleanCurrent
            }
        } else false
    }

    Scaffold(
        topBar = {
            BizTopAppBar(
                title = if (contactId == null) "New Contact" else "Edit Contact",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error Card
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Duplicate Number Warning
            AnimatedVisibility(visible = isDuplicateNumber) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CallHoldAmberContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Warning: This phone number already exists in your contacts list.",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. John Smith") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_name_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    errorMessage = null
                },
                label = { Text("Phone Number *") },
                placeholder = { Text("+1 (415) 555-0102") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_phone_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email (Optional)") },
                placeholder = { Text("john@company.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_email_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Organization Field
            OutlinedTextField(
                value = organization,
                onValueChange = {
                    organization = it
                    errorMessage = null
                },
                label = { Text("Company / Organization (Optional)") },
                placeholder = { Text("Acme Corp") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_org_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter contact name"
                    } else if (phoneNumber.isBlank()) {
                        errorMessage = "Please enter contact phone number"
                    } else {
                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            val res = if (contactId == null) {
                                repository.createContact(
                                    name = name.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    email = email.trim().ifEmpty { null },
                                    organization = organization.trim().ifEmpty { null }
                                )
                            } else {
                                repository.updateContact(
                                    id = contactId,
                                    name = name.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    email = email.trim().ifEmpty { null },
                                    organization = organization.trim().ifEmpty { null }
                                )
                            }
                            isSaving = false
                            if (res.isSuccess) {
                                onNavigateBack()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Failed to save contact"
                            }
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_contact_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Contact", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
