package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.GameSocket
import ke.co.brivont.boka.data.extractFen
import ke.co.brivont.boka.data.typeOf
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.board.START_FEN
import ke.co.brivont.boka.ui.theme.Boka
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private data class LiveGame(val id: String, val white: String, val black: String, val moves: Int, val watchers: Int)

private fun cleanName(n: String?, fallback: String): String =
    if (n.isNullOrBlank() || n == "null") fallback else if (n.contains('@')) n.substringBefore('@') else n

private fun parseLobby(msg: JsonObject): List<LiveGame> {
    val arr = msg["spectatableGames"] as? JsonArray ?: return emptyList()
    return arr.mapNotNull { it as? JsonObject }.map { o ->
        val names = (o["playerNames"] as? JsonArray)?.map { (it as? JsonPrimitive)?.content } ?: emptyList()
        LiveGame(
            id = (o["id"] as? JsonPrimitive)?.content ?: "",
            white = cleanName(names.getOrNull(0), "White"),
            black = cleanName(names.getOrNull(1), "Black"),
            moves = o["moveCount"]?.jsonPrimitive?.intOrNull ?: 0,
            watchers = o["spectatorCount"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }.filter { it.id.isNotBlank() }
}

private fun playerName(msg: JsonObject, color: String): String? {
    val ps = (msg["players"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: return null
    return ps.firstOrNull { (it["color"] as? JsonPrimitive)?.content == color }
        ?.let { (it["name"] as? JsonPrimitive)?.content }
}

@Composable
fun SpectateScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val socket = remember { GameSocket(scope) }

    var games by remember { mutableStateOf<List<LiveGame>?>(null) }
    var watching by remember { mutableStateOf<String?>(null) }
    var fen by remember { mutableStateOf(START_FEN) }
    var lastMove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var whiteName by remember { mutableStateOf("White") }
    var blackName by remember { mutableStateOf("Black") }
    var ended by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        socket.connect(onOpen = { socket.sendType("get_games") })
    }
    LaunchedEffect(Unit) {
        socket.messages.collect { msg ->
            when (msg.typeOf()) {
                "lobby_update" -> if (watching == null) games = parseLobby(msg)
                "spectating" -> {
                    msg.extractFen()?.let { fen = it }
                    playerName(msg, "white")?.let { whiteName = it }
                    playerName(msg, "black")?.let { blackName = it }
                    lastMove = null
                    ended = null
                }
                "move_made" -> {
                    msg.extractFen()?.let { fen = it }
                    val lm = (msg["lastMove"] as? JsonObject) ?: (msg["move"] as? JsonObject)
                    val f = lm?.get("from")?.jsonPrimitive?.content
                    val t = lm?.get("to")?.jsonPrimitive?.content
                    if (f != null && t != null) lastMove = f to t
                }
                "game_ended" -> {
                    val reason = msg["reason"]?.jsonPrimitive?.content ?: "over"
                    val winner = msg["winner"]?.jsonPrimitive?.contentOrNull
                    ended = if (winner == null) "Draw ($reason)" else "$winner wins — $reason"
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { scope.launch { socket.disconnect() } } }

    fun watch(id: String) {
        watching = id; fen = START_FEN; lastMove = null; ended = null
        socket.sendType("spectate", "gameId" to id)
    }
    fun stop() {
        socket.sendType("leave_spectating")
        watching = null; ended = null
        socket.sendType("get_games")
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "👀 Watch Live", onBack = { if (watching != null) stop() else onBack() })

        if (watching == null) {
            when {
                games == null -> Loading()
                games!!.isEmpty() -> Text(
                    "No live games right now — check back in a moment.",
                    color = Boka.textMuted, modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(games!!) { g -> LiveGameRow(g) { watch(g.id) } }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(blackName, color = Boka.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                ChessBoard(fen = fen, orientation = "white", lastMove = lastMove, enabled = false)
                Spacer(Modifier.height(8.dp))
                Text(whiteName, color = Boka.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                ended?.let { Text(it, color = Boka.goldBright, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Stop watching", { stop() }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LiveGameRow(g: LiveGame, onWatch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(Boka.surface).border(1.dp, Boka.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onWatch).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${g.white}  vs  ${g.black}", color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Move ${g.moves}" + if (g.watchers > 0) " · ${g.watchers} watching" else "",
                color = Boka.textFaint, fontSize = 11.sp)
        }
        Text("Watch ›", color = Boka.goldBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
