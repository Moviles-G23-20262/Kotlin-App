package com.campusswap.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.data.auth.SessionState
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background job that empties the outbox. WorkManager only starts it once there is a network,
 * keeps it across app restarts and retries it with exponential backoff, so a listing or message
 * queued offline is sent even if the user has closed the app by the time the signal comes back.
 */
class OutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CampusSwapApplication).container
        // Started with the app closed, the stored session may still be loading from DataStore.
        val session = container.sessionManager.state.first { it !is SessionState.Restoring }
        // Without a session the server would refuse everything; the items wait for the next log-in.
        val userId = (session as? SessionState.SignedIn)?.session?.userId ?: return Result.success()
        return when (container.outboxSync.flush(userId)) {
            SyncOutcome.DONE -> Result.success()
            SyncOutcome.RETRY_LATER -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "outbox-sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<OutboxWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            // APPEND_OR_REPLACE: if a sync is running, a second one follows it and picks up anything queued meanwhile.
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
