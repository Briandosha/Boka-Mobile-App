package ke.co.brivont.boka

// iOS local notifications are deferred (UNUserNotificationCenter + a foreground
// presentation delegate). No-op for now so the shared module builds cleanly.
actual fun postLocalNotification(title: String, body: String) {
}
