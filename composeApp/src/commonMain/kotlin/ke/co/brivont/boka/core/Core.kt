package ke.co.brivont.boka.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json

/** Single source of truth for backend hosts — mirrors the web app's utils/config.ts. */
object Config {
    const val API = "https://brivont.co.ke/chess-api"          // game/analysis REST
    const val WS = "wss://brivont.co.ke/chess-socket/"         // live game socket
    const val AUTH = "https://brivont.co.ke/chessapp/processor/" // trailing slash; append api/auth/...
}

val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
}

/**
 * Ktor client. The engine (OkHttp on Android, Darwin on iOS) is auto-selected from
 * the per-platform dependency, so no expect/actual is needed here.
 */
val httpClient: HttpClient by lazy {
    HttpClient {
        install(ContentNegotiation) { json(json) }
        install(WebSockets)
    }
}

/** Tiny key-value store, backed by SharedPreferences (Android) / NSUserDefaults (iOS). */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

expect fun provideStore(): KeyValueStore

/** Wall-clock seconds since the Unix epoch (for JWT expiry checks). */
expect fun nowEpochSeconds(): Long

/** Auth session tokens, persisted across launches. */
object Session {
    private val store: KeyValueStore by lazy { provideStore() }
    var accessToken: String?
        get() = store.get("accessToken")
        set(v) = store.put("accessToken", v)
    var refreshToken: String?
        get() = store.get("refreshToken")
        set(v) = store.put("refreshToken", v)

    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()
    fun clear() { accessToken = null; refreshToken = null }

    // Emits when the session is no longer usable (a refresh was definitively
    // rejected) so the root composable can drop back to the sign-in screen.
    // Buffered with no replay, so an emit that happens before a collector is
    // attached (e.g. a cold-start refresh) is still delivered once.
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    /** Force logout: wipe tokens and notify the UI to return to sign-in. */
    fun expire() { clear(); _sessionExpired.tryEmit(Unit) }
}
