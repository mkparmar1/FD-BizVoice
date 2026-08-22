package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.BizVoiceDatabase
import com.example.data.local.SessionManager
import com.example.data.remote.ApiClient
import com.example.data.repository.BizVoiceRepository
import com.example.telephony.CallManager

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

    val callManager: CallManager by lazy {
        CallManager(context, repository)
    }
}
