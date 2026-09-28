package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.BizVoiceDatabase
import com.example.data.local.SessionManager
import com.example.data.remote.ApiClient
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager
import com.example.telephony.IncomingCallRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BizVoiceAppContainer(val context: Context) {
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val database: BizVoiceDatabase by lazy {
        BizVoiceDatabase.getDatabase(context)
    }

    val apiClient: ApiClient by lazy {
        ApiClient(sessionManager)
    }

    val repository: BizVoiceRepository by lazy {
        BizVoiceRepository(context, sessionManager, database, apiClient)
    }

    // Process-wide scope: incoming calls arrive through a FirebaseMessagingService that
    // may run while no Activity exists, so this cannot be tied to an Activity lifecycle.
    val appScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    /**
     * Whether any Activity of this process is currently in the foreground.
     *
     * MainActivity maintains it. The FCM service uses it to decide between letting
     * CallManager play the ringtone in-process and posting a ringing notification, so a
     * backgrounded call is not announced twice.
     */
    @Volatile
    var isUiVisible: Boolean = false

    val callManager: CallManager by lazy {
        CallManager(context, repository, appScope)
    }

    val incomingCallRegistrar: IncomingCallRegistrar by lazy {
        IncomingCallRegistrar(
            context = context.applicationContext,
            repository = repository,
            scope = appScope,
            onIncomingCall = { invite -> callManager.onIncomingCallInvite(invite) },
            onCancelledCall = { cancelled -> callManager.onIncomingCallCancelled(cancelled) }
        )
    }
}
