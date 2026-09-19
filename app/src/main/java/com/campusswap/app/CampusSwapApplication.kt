package com.campusswap.app

import android.app.Application
import com.campusswap.app.analytics.Analytics

class CampusSwapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Analytics.init(this)
    }
}
