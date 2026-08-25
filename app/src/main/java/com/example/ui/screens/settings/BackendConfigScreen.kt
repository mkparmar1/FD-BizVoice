package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SessionManager
import com.example.data.repository.BizVoiceRepository
import com.example.ui.components.BizTopAppBar
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallGreenContainer
import kotlinx.coroutines.launch

@Composable
fun BackendConfigScreen(
    sessionManager: SessionManager,
    repository: BizVoiceRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiUrl by remember { mutableStateOf(sessionManager.baseApiUrl) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BizTopAppBar(
                title = "Backend Server Configuration",
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
                .padding(20.dp)
                .testTag("backend_config_screen")
        ) {
            Text(
                text = "Laravel Backend & Twilio Integration",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "BizVoice connects to your corporate Laravel REST API to manage extensions, fetch Twilio Voice tokens, and sync call history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Live URL Section
            Text(
                text = "LARAVEL API ENDPOINT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiUrl,
                onValueChange = {
                    apiUrl = it
                    testResult = null
                },
                label = { Text("Base API URL") },
                placeholder = { Text("https://api.yourcompany.com/api/v1.0") },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_url_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Test Connection Result Banner
            AnimatedVisibility(visible = testResult != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) CallGreenContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isSuccess) CallGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = testResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Test Connection Button
            Button(
                onClick = {
                    isTesting = true
                    testResult = null
                    scope.launch {
                        sessionManager.baseApiUrl = apiUrl.trim()
                        val res = repository.getTwilioToken(forceRefresh = true)
                        if (res.isSuccess) {
                            isSuccess = true
                            testResult = "Successfully authenticated with Laravel API and received Twilio Voice token."
                        } else {
                            isSuccess = false
                            testResult = "Unable to connect: ${res.exceptionOrNull()?.message ?: "Server unreachable"}"
                        }
                        isTesting = false
                    }
                },
                enabled = !isTesting,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("test_backend_button")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Testing Connection...")
                } else {
                    Text("Save & Test Connection", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
