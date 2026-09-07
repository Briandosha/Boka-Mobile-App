package ke.co.brivont.boka

/**
 * Lightweight local notifications driven by our own WebSocket events (no Firebase).
 * We post a system notification when a game event arrives while the app is in the
 * background — e.g. the opponent moved and it's now your turn. This covers the
 * common "tabbed away mid-game" case; waking a fully-killed app would still need
 * FCM or a foreground service holding the socket.
 */
expect fun postLocalNotification(title: String, body: String)

/** Whether the app is currently in the foreground (updated from the platform lifecycle). */
object AppForeground {
    var value: Boolean = true
}
