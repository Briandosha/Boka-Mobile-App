package ke.co.brivont.boka.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.chess.Position
import ke.co.brivont.boka.chess.nameToSquare
import ke.co.brivont.boka.chess.squareName
import ke.co.brivont.boka.core.Session
import ke.co.brivont.boka.data.GameSocket
import ke.co.brivont.boka.data.typeOf
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.theme.Boka
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun CoachScreen(onBack: () -> Unit, elo: Int = 1500) {
    val scope = rememberCoroutineScope()
    val socket = remember { GameSocket(scope) }

    // Full game tracked locally by the on-device engine (player is White).
    var history by remember { mutableStateOf(listOf(Position.start())) }
    var lastMoves by remember { mutableStateOf(listOf<Pair<String, String>?>(null)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var eval by remember { mutableStateOf(0) }
    var hint by remember { mutableStateOf<Pair<String, String>?>(null) }
    var hintLoading by remember { mutableStateOf(false) }
    // "Change opponent's move" training mode: rewind the engine's reply and let
    // the player choose a Black move to rehearse their response against.
    var editOpponent by remember { mutableStateOf(false) }
    var savedEngineMove by remember { mutableStateOf<Pair<Position, Pair<String, String>?>?>(null) }
    var premiumNeeded by remember { mutableStateOf(false) }
    var viewPly by remember { mutableStateOf<Int?>(null) }

    val live: Position = history.last()
    val maxIndex = history.lastIndex
    val currentIndex = (viewPly ?: maxIndex).coerceIn(0, maxIndex)
    val reviewing = currentIndex < maxIndex
    val shown = history[currentIndex]

    fun applyLive(next: Position, lm: Pair<String, String>) {
        val capture = next.board.count { it != ' ' } < history.last().board.count { it != ' ' }
        ke.co.brivont.boka.playSfx(if (next.inCheck()) "check" else if (capture) "capture" else "move")
        history = history + next
        lastMoves = lastMoves + lm
        viewPly = null
        selected = null
        hint = null
        hintLoading = false
        if (next.isGameOver()) {
            gameOver = when {
                next.isCheckmate() && next.whiteToMove -> "Checkmate — you lost"
                next.isCheckmate() -> "Checkmate — you won! 🎉"
                else -> "Draw"
            }
        }
    }

    // The engine request we're waiting on (fen after our move); survives drops.
    var pendingAiFen by remember { mutableStateOf<String?>(null) }
    val isConnected by socket.connected.collectAsState()

    LaunchedEffect(Unit) {
        socket.connect(onOpen = {
            socket.sendType("start_training", "elo" to elo, "token" to Session.accessToken)
            // Resume a request that was in flight when the connection dropped.
            pendingAiFen?.let {
                socket.sendType("request_ai_move", "fen" to it, "elo" to elo, "token" to Session.accessToken)
            }
        })
    }

    // Keep-alive: coach sessions idle long enough for sockets to die, and the
    // very first dial can fail if the network isn't up yet. Poll instead of
    // collecting `connected` — a StateFlow won't re-emit false after a failed
    // retry, which would strand us. reconnect() no-ops while connected.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            if (!socket.connected.value) socket.reconnect()
        }
    }

    // Watchdog: if the engine reply hasn't arrived, ask again (covers silent drops).
    LaunchedEffect(pendingAiFen) {
        val fen = pendingAiFen ?: return@LaunchedEffect
        kotlinx.coroutines.delay(12_000)
        if (pendingAiFen == fen) {
            socket.sendType("request_ai_move", "fen" to fen, "elo" to elo, "token" to Session.accessToken)
        }
    }
    LaunchedEffect(Unit) {
        socket.messages.collect { msg ->
            when (msg.typeOf()) {
                "premium_required" -> premiumNeeded = true
                "ai_move_response" -> {
                    isAiThinking = false
                    pendingAiFen = null
                    val mv = msg["move"]?.jsonObject
                    (mv?.get("score") as? JsonPrimitive)?.intOrNull?.let { eval = it }
                    val uci = mv?.get("bestMove")?.jsonPrimitive?.content
                    if (uci != null && uci.length >= 4) {
                        // Read the CURRENT position from state — `live` is captured at
                        // first composition and would reject every reply as illegal.
                        val cur = history.last()
                        val np = cur.makeMove(nameToSquare(uci.substring(0, 2)), nameToSquare(uci.substring(2, 4)), if (uci.length > 4) uci[4] else null)
                        if (np != null) applyLive(np, uci.substring(0, 2) to uci.substring(2, 4))
                    }
                }
                "coach_feedback" -> {
                    feedback = msg["reason"]?.jsonPrimitive?.content
                    (msg["evalAfter"] as? JsonPrimitive)?.intOrNull?.let { eval = it }
                }
                "coach_hint" -> {
                    hintLoading = false
                    val uci = msg["bestMove"]?.jsonPrimitive?.content
                    if (uci != null && uci.length >= 4) hint = uci.substring(0, 2) to uci.substring(2, 4)
                    (msg["score"] as? JsonPrimitive)?.intOrNull?.let { eval = it }
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { scope.launch { socket.disconnect() } } }

    fun tap(sqName: String) {
        if (reviewing || isAiThinking || gameOver != null) return
        // In "change opponent" mode the human moves Black; otherwise White.
        val movingWhite = !editOpponent
        if (live.whiteToMove != movingWhite) return
        fun isMine(c: Char) = c != ' ' && (c.isUpperCase() == movingWhite)
        val idx = nameToSquare(sqName)
        val cur = selected
        if (cur == null) {
            if (isMine(live.board[idx])) selected = sqName
            return
        }
        if (cur == sqName) { selected = null; return }
        val fromIdx = nameToSquare(cur)
        val piece = live.board[fromIdx]
        val promo = when {
            piece == 'P' && idx / 8 == 7 -> 'q'
            piece == 'p' && idx / 8 == 0 -> 'q'
            else -> null
        }
        val before = live.toFen()
        val np = live.makeMove(fromIdx, idx, promo)
        if (np != null) {
            feedback = null
            if (editOpponent) {
                // Player set the opponent's reply — resume as White, no engine call.
                applyLive(np, cur to sqName)
                editOpponent = false
                savedEngineMove = null
            } else {
                applyLive(np, cur to sqName)
                if (gameOver == null) {
                    isAiThinking = true
                    val after = np.toFen()
                    pendingAiFen = after
                    socket.sendType("request_ai_move", "fen" to after, "elo" to elo, "token" to Session.accessToken)
                    socket.sendType("coach_analyze", "fenBefore" to before, "fenAfter" to after,
                        "playerMove" to (cur + sqName + (promo ?: "")), "token" to Session.accessToken)
                }
            }
        } else {
            selected = if (isMine(live.board[idx])) sqName else null
        }
    }

    fun requestHint() {
        if (isAiThinking || reviewing || gameOver != null || !live.whiteToMove) return
        hint = null
        hintLoading = true
        socket.sendType("coach_hint", "fen" to live.toFen(), "token" to Session.accessToken)
    }

    fun startEditOpponent() {
        if (isAiThinking || reviewing || gameOver != null || !live.whiteToMove || history.size < 2) return
        // Rewind the engine's last reply; the player will choose Black's move instead.
        savedEngineMove = history.last() to lastMoves.last()
        history = history.dropLast(1)
        lastMoves = lastMoves.dropLast(1)
        viewPly = null
        selected = null
        hint = null
        hintLoading = false
        feedback = null
        editOpponent = true
    }

    fun cancelEditOpponent() {
        val saved = savedEngineMove
        if (saved != null) {
            history = history + saved.first
            lastMoves = lastMoves + saved.second
        }
        savedEngineMove = null
        editOpponent = false
        selected = null
    }

    val targets: Set<String> = if (!reviewing && selected != null) {
        val f = nameToSquare(selected!!)
        live.legalMoves().filter { it.from == f }.map { squareName(it.to) }.toSet()
    } else emptySet()

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "🤖 Coach · $elo", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (premiumNeeded) {
                Text("Coach is a Premium feature", color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Subscribe to play and be coached by Stockfish.", color = Boka.textMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
            }
            Text(
                if (!isConnected) "Reconnecting…"
                else if (editOpponent) "Choose your opponent’s move"
                else if (isAiThinking) "Stockfish is thinking…"
                else if (gameOver != null) "Game over"
                else if (live.whiteToMove) "Your move" else "Opponent to move",
                color = Boka.goldBright, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
            EvalBar(eval)
            Spacer(Modifier.height(8.dp))
            ChessBoard(
                fen = shown.toFen(),
                orientation = "white",
                selected = if (reviewing) null else selected,
                lastMove = lastMoves[currentIndex],
                targets = targets,
                hint = if (reviewing) null else hint,
                enabled = !reviewing && !isAiThinking && gameOver == null && (live.whiteToMove != editOpponent),
                onSquareTap = ::tap,
            )
            Spacer(Modifier.height(10.dp))
            if (gameOver == null && !premiumNeeded) {
                SecondaryButton(
                    text = if (hintLoading) "💡 Thinking…" else "💡 Hint — show the best move",
                    onClick = ::requestHint,
                    enabled = isConnected && !isAiThinking && !hintLoading && !reviewing && !editOpponent && live.whiteToMove,
                    modifier = Modifier.fillMaxWidth(),
                )
                hint?.let {
                    Text(
                        "Best move: ${it.first} → ${it.second}",
                        color = Boka.goldBright, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (editOpponent) {
                    Text(
                        "Pick a move for your opponent, then respond to it.",
                        color = Color(0xFF34D07A), fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    SecondaryButton(
                        text = "✖ Cancel — restore the engine’s move",
                        onClick = ::cancelEditOpponent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SecondaryButton(
                        text = "🔀 Change opponent’s move",
                        onClick = ::startEditOpponent,
                        enabled = isConnected && !isAiThinking && !reviewing && gameOver == null && live.whiteToMove && maxIndex >= 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            gameOver?.let { Text(it, color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            feedback?.let {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Boka.surface).padding(12.dp)) {
                    Text("Coach", color = Boka.goldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(it, color = Boka.textMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (maxIndex > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("⏮", { viewPly = 0 }, Modifier.weight(1f))
                    SecondaryButton("‹", { viewPly = (currentIndex - 1).coerceAtLeast(0) }, Modifier.weight(1f))
                    SecondaryButton("›", { val n = currentIndex + 1; viewPly = if (n >= maxIndex) null else n }, Modifier.weight(1f))
                    SecondaryButton("Live", { viewPly = null }, Modifier.weight(1f))
                }
                if (reviewing) Text("Reviewing move $currentIndex of $maxIndex — go live to keep playing.",
                    color = Boka.textFaint, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/**
 * Lichess-style evaluation bar. [cp] is the engine score in centipawns (from
 * White's view). White's share of the bar grows with White's advantage; the
 * numeric eval sits on the leading side. Mate scores saturate the bar via tanh.
 */
@Composable
private fun EvalBar(cp: Int) {
    val target = (0.5 + 0.5 * kotlin.math.tanh(cp / 400.0)).toFloat().coerceIn(0.03f, 0.97f)
    val frac by animateFloatAsState(target, tween(350), label = "eval")
    val label = (if (cp >= 0) "+" else "") + (cp / 100.0)
    // Brand hues: White's share = off-white, Black's = site charcoal.
    Box(
        Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2B2E34)),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Color(0xFFF5F8FA)))
        Text(
            label,
            modifier = Modifier
                .align(if (cp >= 0) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 10.dp),
            color = if (cp >= 0) Color(0xFF2B2E34) else Color(0xFFF5F8FA),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}
