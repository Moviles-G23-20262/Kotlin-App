package com.campusswap.app

import android.app.Application
import com.campusswap.app.model.analytics.Analytics
import com.campusswap.app.model.notifications.NotificationWorker

class CampusSwapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.sessionManager.launchRestore()
        Analytics.init(this, container.eventSink)
        NotificationWorker.schedule(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) container.imageCache.trimMemory()
    }
}
