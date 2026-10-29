package com.campusswap.app.model.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.model.auth.SessionState
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Checks the server for new notifications (alert matches, orders) every ~15 minutes, even with the
 * app closed, and shows them in the phone's notification shade.
 */
class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CampusSwapApplication).container
        val session = container.sessionManager.state.first { it !is SessionState.Restoring }
        val userId = (session as? SessionState.SignedIn)?.session?.userId ?: return Result.success()
        val feed = container.notificationRepository.load(userId)
        if (!feed.isLive) return Result.retry()
        SystemNotifier(applicationContext).show(userId, feed.items)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "notification-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
