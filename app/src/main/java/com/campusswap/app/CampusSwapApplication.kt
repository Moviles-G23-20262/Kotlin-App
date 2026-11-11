package com.campusswap.app

import android.app.Application
import com.campusswap.app.model.analytics.Analytics
import com.campusswap.app.model.analytics.CrashTelemetry
import com.campusswap.app.model.auth.SessionState
import com.campusswap.app.model.notifications.NotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CampusSwapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashTelemetry.init(this)
        container = AppContainer(this)
        container.sessionManager.launchRestore()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            container.sessionManager.state.collect { state ->
                CrashTelemetry.user((state as? SessionState.SignedIn)?.session?.userId)
            }
        }
        Analytics.init(this, container.eventSink)
        NotificationWorker.schedule(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) container.imageCache.trimMemory()
    }
}
