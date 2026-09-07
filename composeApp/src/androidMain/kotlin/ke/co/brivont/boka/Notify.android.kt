package ke.co.brivont.boka

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ke.co.brivont.boka.core.androidAppContext

private const val CHANNEL_ID = "boka_moves"
private var channelReady = false

private fun ensureChannel(nm: NotificationManager) {
    if (channelReady) return
    val ch = NotificationChannel(CHANNEL_ID, "Game alerts", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Your move & opponent updates"
    }
    nm.createNotificationChannel(ch)
    channelReady = true
}

actual fun postLocalNotification(title: String, body: String) {
    try {
        val ctx = androidAppContext
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        // Tapping the notification re-opens the app.
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = launch?.let {
            PendingIntent.getActivity(ctx, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val n = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(ctx.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .apply { if (pi != null) setContentIntent(pi) }
            .build()
        nm.notify(1001, n)
    } catch (e: Throwable) {
        // never let a notification failure affect gameplay
    }
}
