package ke.co.brivont.boka

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar
import ke.co.brivont.boka.core.androidAppContext
import ke.co.brivont.boka.core.provideStore

private const val REQUEST_CODE = 4711
private const val REMINDER_HOUR = 18            // 6 pm local: a nudge before the day ends
private const val ACTION_REMIND = "ke.co.brivont.boka.STREAK_REMINDER"

/** Arms an inexact daily alarm (no special permission needed). Safe to call on every launch. */
actual fun scheduleStreakReminder() {
    runCatching { scheduleAlarm(androidAppContext) }
}

private fun scheduleAlarm(ctx: Context) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, StreakReminderReceiver::class.java).setAction(ACTION_REMIND)
    val pi = PendingIntent.getBroadcast(ctx, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val next = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, REMINDER_HOUR); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
}

/**
 * Fires daily (and on boot, to re-arm the alarm). Posts a reminder only if no
 * puzzle has been solved since local midnight — PuzzleScreen stores the last solve.
 */
class StreakReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        androidAppContext = app                        // the store needs it; we may be a cold process
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) { runCatching { scheduleAlarm(app) }; return }

        val store = provideStore()
        val lastSolve = store.get("puzzle_last_solve_epoch")?.toLongOrNull() ?: 0L
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        if (lastSolve >= midnight) return              // already solved today — no nag

        val streak = store.get("puzzle_streak")?.toIntOrNull() ?: 0
        val body = if (streak > 0)
            "Your $streak-day puzzle streak is at risk — solve one puzzle to keep it."
        else
            "A quick puzzle keeps your tactics sharp — try today's."
        postLocalNotification("Boka ♟", body)
    }
}
