package com.campusswap.app.model.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.campusswap.app.MainActivity
import com.campusswap.app.R
import com.campusswap.app.model.AppNotification

/** Shows the server's notifications in the phone's notification shade, each one only once. */
class SystemNotifier(private val context: Context) {
    private val prefs = context.getSharedPreferences("system-notifications", Context.MODE_PRIVATE)

    /** Posts the unread notifications this phone hasn't shown yet. The first run only records them. */
    fun show(userId: String, items: List<AppNotification>) {
        val seenKey = "seen-$userId"
        val seen = prefs.getStringSet(seenKey, null)
        val unread = items.filter { !it.isRead }
        if (seen == null) {
            prefs.edit().putStringSet(seenKey, items.map { it.id }.toSet()).apply()
            return
        }
        val fresh = unread.filter { it.id !in seen }
        if (fresh.isNotEmpty() && canPost()) {
            createChannel()
            fresh.forEach(::post)
        }
        prefs.edit().putStringSet(seenKey, seen + items.map { it.id }).apply()
    }

    private fun canPost() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Alerts and updates", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun post(item: AppNotification) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "alerts"
    }
}
