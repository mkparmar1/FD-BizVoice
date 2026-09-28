package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BizVoiceApplication

/**
 * Handles notification actions that must run without opening the call UI, such as
 * declining straight from the notification shade.
 */
class CallActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CALL_ACTION_RECEIVER"
        const val ACTION_DECLINE = "com.example.telephony.action.DECLINE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE -> {
                Log.i(TAG, "Decline requested from notification")
                val container = BizVoiceApplication.container(context.applicationContext)
                val callManager = container.callManager
                callManager.declineIncomingCall()
                IncomingCallNotifier.cancel(context.applicationContext)
            }
            else -> Log.w(TAG, "Unhandled action: ${intent.action}")
        }
    }
}
