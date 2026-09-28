package com.example.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.BizVoiceApplication
import com.example.data.model.CallDirection
import com.example.telephony.CallState
import com.example.telephony.IncomingCallNotifier
import com.example.ui.screens.calls.IncomingCallScreen
import com.example.ui.theme.BizVoiceTheme

/**
 * Hosts the incoming call UI when the app is not in the foreground.
 *
 * Launched via the notification's full-screen intent, so it must be able to appear over
 * the lock screen. It renders the same [IncomingCallScreen] the foreground overlay uses,
 * which keeps the accept and decline behaviour identical in both cases.
 */
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showOverLockScreen()

        val container = BizVoiceApplication.container(applicationContext)
        val callManager = container.callManager
        val sessionManager = container.sessionManager

        setContent {
            val themeMode by sessionManager.themeModeFlow.collectAsState(initial = sessionManager.themeMode)
            val activeCall by callManager.activeCallFlow.collectAsState()

            // Dismiss as soon as the call stops being an unanswered inbound call, whether
            // the user answered, declined, or the caller cancelled.
            LaunchedEffect(activeCall.state, activeCall.direction) {
                val stillRinging = activeCall.state == CallState.RINGING &&
                    activeCall.direction == CallDirection.INCOMING
                if (!stillRinging) {
                    IncomingCallNotifier.cancel(applicationContext)
                    finish()
                }
            }

            BizVoiceTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IncomingCallScreen(callManager = callManager)
                }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
