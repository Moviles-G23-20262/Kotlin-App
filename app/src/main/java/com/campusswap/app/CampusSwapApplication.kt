package com.campusswap.app

import android.app.Application
import com.campusswap.app.analytics.Analytics

class CampusSwapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.sessionManager.launchRestore()
        Analytics.init(this)
    }
}
