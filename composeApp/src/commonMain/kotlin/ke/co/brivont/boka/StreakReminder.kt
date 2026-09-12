package ke.co.brivont.boka

/**
 * Daily "keep your puzzle streak" nudge, scheduled locally on the device (no push
 * service or server needed, works offline). Fires once a day in the early evening;
 * the platform receiver posts a notification only if today's puzzle isn't done yet,
 * using the last-solve time and streak that PuzzleScreen records in the key-value
 * store. iOS is a no-op for now.
 */
expect fun scheduleStreakReminder()
