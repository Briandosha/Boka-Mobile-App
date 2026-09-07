package ke.co.brivont.boka.data

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.brivont.boka.core.Config
import ke.co.brivont.boka.core.Session
import ke.co.brivont.boka.core.accessTokenExpired
import ke.co.brivont.boka.core.json
import ke.co.brivont.boka.core.httpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Live game / coach WebSocket, mirroring the web client's protocol:
 *  → create_game, join_game, make_move, resign, start_training, request_ai_move …
 *  ← game_created, game_joined, player_joined, move_made, evaluation_update, game_ended, ai_move_response …
 */
class GameSocket(private val scope: CoroutineScope) {
    private var session: DefaultClientWebSocketSession? = null

    // replay so a collector that subscribes just after connect still sees the
    // first server messages (e.g. the immediate lobby_update after get_games).
    private val _messages = MutableSharedFlow<JsonObject>(replay = 16, extraBufferCapacity = 128)
    val messages: SharedFlow<JsonObject> = _messages

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private var onOpen: (suspend () -> Unit)? = null
    private var guest: String? = null

    /**
     * Open the socket. [onOpen] runs every time the connection is (re)established —
     * put the screen's initial message there (create_game / start_training) so it's
     * automatically re-sent after a token-refresh reconnect.
     */
    fun connect(guestName: String? = null, onOpen: (suspend () -> Unit)? = null) {
        this.onOpen = onOpen
        this.guest = guestName
        scope.launch {
            // Refresh a stale token BEFORE opening so we make a single healthy
            // connection (avoids a doomed socket that half-creates a game).
            if (!Session.accessToken.isNullOrBlank() && accessTokenExpired()) {
                // Renew a stale token before dialing. If refresh fails (and, when
                // the refresh token is dead, force-logs-out via Session.expire()),
                // don't open a doomed socket with an expired token.
                if (!AuthApi.refresh()) return@launch
            }
            openOnce(allowRefresh = true)
        }
    }

    /** Re-dial after a drop, reusing the original onOpen (which re-sends the
     *  screen's session-establishing message). No-op if already connected. */
    fun reconnect() {
        scope.launch {
            if (session != null) return@launch
            if (!Session.accessToken.isNullOrBlank() && accessTokenExpired()) {
                // Renew a stale token before dialing. If refresh fails (and, when
                // the refresh token is dead, force-logs-out via Session.expire()),
                // don't open a doomed socket with an expired token.
                if (!AuthApi.refresh()) return@launch
            }
            openOnce(allowRefresh = true)
        }
    }

    private suspend fun openOnce(allowRefresh: Boolean) {
        if (session != null) return   // already connected — never double-dial
        val token = Session.accessToken
        val url = when {
            token != null -> "${Config.WS}?token=$token"
            else -> "${Config.WS}?isGuest=true&name=${guest ?: "Guest"}"
        }
        var closeCode: Short? = null
        try {
            httpClient.webSocket(url) {
                session = this
                _connected.value = true
                onOpen?.invoke()
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        runCatching { json.parseToJsonElement(frame.readText()).jsonObject }
                            .getOrNull()?.let { _messages.emit(it) }
                    }
                }
                closeCode = runCatching { closeReason.await()?.code }.getOrNull()
            }
        } catch (_: Throwable) {
            // dropped / failed — surface via connected flag
        } finally {
            _connected.value = false
            session = null
        }
        // 4001 = access token rejected → refresh once and reconnect (safety net).
        if (closeCode?.toInt() == 4001 && allowRefresh && token != null && AuthApi.refresh()) {
            openOnce(allowRefresh = false)
        }
    }

    fun send(obj: JsonObject) {
        scope.launch { runCatching { session?.send(Frame.Text(obj.toString())) } }
    }

    fun sendType(type: String, vararg extras: Pair<String, Any?>) {
        send(buildJsonObject {
            put("type", JsonPrimitive(type))
            extras.forEach { (k, v) ->
                when (v) {
                    null -> {}
                    is String -> put(k, JsonPrimitive(v))
                    is Int -> put(k, JsonPrimitive(v))
                    is Boolean -> put(k, JsonPrimitive(v))
                    is JsonObject -> put(k, v)
                    else -> put(k, JsonPrimitive(v.toString()))
                }
            }
        })
    }

    suspend fun disconnect() {
        runCatching { session?.close() }
        session = null
        _connected.value = false
    }
}

/** Best-effort extraction of a FEN from any server message (game_created/joined/move_made). */
fun JsonObject.extractFen(): String? {
    (this["gameState"] as? JsonObject)?.get("fen")?.let { return it.jsonPrimitive.content }
    (this["result"] as? JsonObject)?.let { r ->
        (r["gameState"] as? JsonObject)?.get("fen")?.let { return it.jsonPrimitive.content }
    }
    this["fen"]?.let { return it.jsonPrimitive.content }
    return null
}

fun JsonObject.typeOf(): String? = this["type"]?.jsonPrimitive?.content
