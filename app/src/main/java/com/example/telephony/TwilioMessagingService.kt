package com.example.telephony

import android.util.Log
import com.example.BizVoiceApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Twilio's VoIP pushes over FCM.
 *
 * Twilio sends the CallInvite as an FCM data payload; Voice.handleMessage() is what
 * actually validates and unwraps it into a CallInvite. Anything the SDK does not
 * recognise is left for other handlers, so unrelated pushes are not swallowed.
 */
class TwilioMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TWILIO_FCM"
    }

    /**
     * A rotated FCM token invalidates the previous device registration, so the device has
     * to be registered again or inbound calls silently stop arriving.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token rotated, re-registering device")
        val container = BizVoiceApplication.container(applicationContext)
        container.sessionManager.devicePushToken = token
        container.incomingCallRegistrar.registerForIncomingCalls(forceRefresh = true)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val container = BizVoiceApplication.container(applicationContext)
        val handled = container.incomingCallRegistrar.handlePushMessage(message.data)
        if (!handled) {
            Log.w(TAG, "FCM payload was not a Twilio VoIP message: ${message.data.keys}")
            return
        }

        val callManager = container.callManager

        // The invite has been dispatched to CallManager by this point, so the active call
        // flow already describes it.
        if (container.isUiVisible) {
            // Foreground: the MainContainerScreen overlay renders it, and CallManager
            // keeps playing the ringtone itself.
            Log.i(TAG, "Incoming call handled while UI is in the foreground")
            return
        }

        // Background: hand the sound to a full-screen notification and mute the local
        // ringtone so the call is not announced twice.
        callManager.setRingingAudible(false)
        val active = callManager.activeCallFlow.value
        IncomingCallNotifier.show(applicationContext, active.remotePhoneNumber, active.remoteName)
    }
}
