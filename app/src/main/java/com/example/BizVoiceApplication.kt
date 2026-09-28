package com.example

import android.app.Application
import android.content.Context

/**
 * Holds the single [BizVoiceAppContainer] for the process.
 *
 * Twilio registration and the UI must observe the same CallManager. A CallInvite can
 * arrive over FCM while the app is backgrounded or has just been launched, and if
 * MainActivity built its own container the invite would land on an instance that no
 * screen is observing, so the call would never ring.
 */
class BizVoiceApplication : Application() {

    lateinit var container: BizVoiceAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = BizVoiceAppContainer(this)
    }

    companion object {
        lateinit var instance: BizVoiceApplication
            private set

        fun container(context: Context): BizVoiceAppContainer {
            val app = context.applicationContext
            return if (app is BizVoiceApplication) {
                app.container
            } else {
                // Fallback keeps Robolectric and JVM tests working without the real Application.
                BizVoiceAppContainer(app)
            }
        }
    }
}
