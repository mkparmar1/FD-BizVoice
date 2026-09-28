package com.example.telephony

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.ui.IncomingCallActivity

/**
 * Presents an inbound call as a full-screen notification while the app is backgrounded.
 *
 * The Twilio CallInvite itself stays in CallManager; this notification only surfaces the
 * caller and routes the user into [IncomingCallActivity], which renders the same
 * accept/decline UI used in the foreground.
 */
object IncomingCallNotifier {

    private const val TAG = "INCOMING_CALL_NOTIFIER"
    const val CHANNEL_ID = "incoming_calls"
    private const val NOTIFICATION_ID = 4201

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for incoming VoIP calls"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                ringtoneUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            enableLights(true)
            setBypassDnd(false)
        }
        manager.createNotificationChannel(channel)
        Log.i(TAG, "Created incoming call notification channel")
    }

    fun show(context: Context, phoneNumber: String, callerName: String?) {
        ensureChannel(context)

        val label = when {
            !callerName.isNullOrBlank() -> callerName
            phoneNumber.isNotBlank() -> phoneNumber
            else -> "Unknown caller"
        }

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_DECLINE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 14+ only honours the full-screen intent when the user has granted it, so
        // the heads-up notification is often all they see; it must be answerable as-is.
        val answerIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ANSWER_CALL, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Incoming call")
            .setContentText(label)
            .setSubText(phoneNumber.takeIf { it.isNotBlank() && it != label })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declineIntent)
            .addAction(android.R.drawable.sym_action_call, "Answer", answerIntent)

        val notification: Notification = builder.build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "Posted incoming call notification for $label")
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS can be denied; the call still rings via the UI overlay.
            Log.e(TAG, "Cannot post notification, permission denied: ${e.message}")
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
