package ke.co.brivont.boka.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.AppForeground
import ke.co.brivont.boka.chess.Position
import ke.co.brivont.boka.chess.nameToSquare
import ke.co.brivont.boka.chess.squareName
import ke.co.brivont.boka.data.GameSocket
import ke.co.brivont.boka.data.extractFen
import ke.co.brivont.boka.data.typeOf
import ke.co.brivont.boka.playSfx
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.serialization.json.contentOrNull
import ke.co.brivont.boka.postLocalNotification
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.board.START_FEN
import ke.co.brivont.boka.ui.board.drawChessPiece
import ke.co.brivont.boka.ui.theme.Boka
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TimeControl(val initial: Int, val inc: Int, val label: String, val cat: String)

/** How the game ended: [won] true/false from my perspective, null = draw. */
data class EndInfo(val reason: String, val won: Boolean?)

private val TIME_CONTROLS = listOf(
    TimeControl(60, 0, "1+0", "Bullet"),
    TimeControl(120, 1, "2+1", "Bullet"),
    TimeControl(180, 0, "3+0", "Blitz"),
    TimeControl(180, 2, "3+2", "Blitz"),
    TimeControl(300, 0, "5+0", "Blitz"),
    TimeControl(600, 0, "10+0", "Rapid"),
    TimeControl(900, 10, "15+10", "Rapid"),
)

private fun pieceCount(fen: String): Int = fen.substringBefore(' ').count { it.isLetter() }
/** Placement + side-to-move (first two FEN fields) — used to match our optimistic move to the server echo. */
private fun fenKey(fen: String): String = fen.split(' ').take(2).joinToString(" ")

private fun fmtClock(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

private fun JsonObject.intField(k: String): Int? = this[k]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()

/** Extract (whiteTime, blackTime) seconds from any server message that carries them. */
private fun JsonObject.clocks(): Pair<Int, Int>? {
    val gs = (this["gameState"] as? JsonObject) ?: ((this["result"] as? JsonObject)?.get("gameState") as? JsonObject)
    val w = intField("whiteTime") ?: gs?.intField("whiteTime")
    val b = intField("blackTime") ?: gs?.intField("blackTime")
    return if (w != null || b != null) (w ?: 0) to (b ?: 0) else null
}

@Composable
fun GameScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val socket = remember { GameSocket(scope) }

    var mode by remember { mutableStateOf<TimeControl?>(null) }   // null = still choosing
    var opponentJoined by remember { mutableStateOf(false) }
    var myColor by remember { mutableStateOf<String?>(null) }
    var gameId by remember { mutableStateOf<String?>(null) }
    var resumableGameId by remember { mutableStateOf<String?>(null) }
    var checkingResume by remember { mutableStateOf(true) }
    var pendingNewGame by remember { mutableStateOf<TimeControl?>(null) }
    var forfeitThenStart by remember { mutableStateOf<TimeControl?>(null) }
    var resumablePlies by remember { mutableStateOf(0) }                       // moves in the active game
    var pendingEndAction by remember { mutableStateOf<Pair<String, TimeControl>?>(null) } // (abort_game|resign) -> new TC
    var pendingPick by remember { mutableStateOf<TimeControl?>(null) } // picked while still checking
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    var history by remember { mutableStateOf(listOf(START_FEN)) }
    var lastMoves by remember { mutableStateOf(listOf<Pair<String, String>?>(null)) }
    var viewPly by remember { mutableStateOf<Int?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var ended by remember { mutableStateOf<EndInfo?>(null) }
    var whiteTime by remember { mutableStateOf(0) }
    var blackTime by remember { mutableStateOf(0) }

    val maxIndex = history.lastIndex
    val currentIndex = (viewPly ?: maxIndex).coerceIn(0, maxIndex)
    val reviewing = currentIndex < maxIndex
    val liveFen = history.last()
    val shownFen = history[currentIndex]

    // Connect as soon as the screen opens so the server's `authenticated` message
    // (which carries any resumable game) arrives and we can offer to rejoin. onOpen
    // re-establishes the current intent (resume the live game, or re-create the new
    // one) after a token-refresh reconnect.
    LaunchedEffect(Unit) {
        socket.connect(onOpen = {
            val g = gameId
            val m = mode
            when {
                g != null && ended == null -> socket.sendType("resume_game", "gameId" to g)
                m != null && ended == null -> socket.sendType("create_game", "timeControl" to buildJsonObject {
                    put("initial", JsonPrimitive(m.initial)); put("increment", JsonPrimitive(m.inc))
                })
            }
        })
    }

    // Stop the "checking" shimmer even if the server never sends `authenticated`.
    LaunchedEffect(Unit) { delay(5000); checkingResume = false }

    // A time control picked while still checking resolves once the check completes.
    LaunchedEffect(checkingResume, pendingPick, resumableGameId) {
        val tc = pendingPick ?: return@LaunchedEffect
        if (!checkingResume) {
            pendingPick = null
            if (resumableGameId != null) pendingNewGame = tc else mode = tc
        }
    }

    // Create a new game once a time control is chosen (the socket is already up from
    // mount; wait for it if the dial is still completing).
    LaunchedEffect(mode) {
        val m = mode ?: return@LaunchedEffect
        if (gameId != null) return@LaunchedEffect   // resuming, or already playing
        whiteTime = m.initial; blackTime = m.initial
        if (!socket.connected.value) socket.connected.first { it }
        socket.sendType("create_game", "timeControl" to buildJsonObject {
            put("initial", JsonPrimitive(m.initial)); put("increment", JsonPrimitive(m.inc))
        })
    }

    LaunchedEffect(Unit) {
        socket.messages.collect { msg ->
            when (msg.typeOf()) {
                "authenticated" -> {
                    val ag = msg["activeGame"]?.jsonPrimitive?.contentOrNull
                    if (!ag.isNullOrBlank() && gameId == null) resumableGameId = ag
                    resumablePlies = msg["activeGamePlies"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    checkingResume = false
                    refreshing = false
                }
                "error" -> {
                    errorMsg = msg["message"]?.jsonPrimitive?.contentOrNull ?: "Something went wrong."
                    // A failed resume/abort/forfeit means the stored game is gone or not ours — recover.
                    pendingEndAction = null; forfeitThenStart = null
                    if (gameId == null) resumableGameId = null
                    checkingResume = false; refreshing = false
                }
                "game_resumed" -> {
                    val g = msg["gameId"]?.jsonPrimitive?.content
                    val end = pendingEndAction
                    if (end != null && g != null) {
                        // We resumed only to END this game (abort/forfeit), not to play it.
                        // Now attached (player.ws = us), so the server accepts the action and
                        // the game_ended it broadcasts reaches us -> forfeitThenStart creates the new game.
                        pendingEndAction = null
                        forfeitThenStart = end.second
                        resumableGameId = null
                        socket.sendType(end.first, "gameId" to g)
                    } else {
                        gameId = g
                        myColor = msg["color"]?.jsonPrimitive?.content
                        msg.extractFen()?.let { history = listOf(it); lastMoves = listOf(null) }
                        msg.clocks()?.let { whiteTime = it.first; blackTime = it.second }
                        viewPly = null; selected = null; ended = null
                        opponentJoined = true
                        resumableGameId = null
                        if (mode == null) mode = TimeControl(whiteTime.coerceAtLeast(1), 0, "", "")
                    }
                }
                "game_created" -> {
                    gameId = msg["gameId"]?.jsonPrimitive?.content; myColor = "white"
                    msg.extractFen()?.let { history = listOf(it); lastMoves = listOf(null) }
                    msg.clocks()?.let { whiteTime = it.first; blackTime = it.second }
                }
                "game_joined" -> {
                    gameId = msg["gameId"]?.jsonPrimitive?.content; myColor = "black"
                    msg.extractFen()?.let { history = listOf(it); lastMoves = listOf(null) }
                    msg.clocks()?.let { whiteTime = it.first; blackTime = it.second }
                    opponentJoined = true
                    playSfx("notify")
                }
                "player_joined" -> {
                    msg.extractFen()?.let { if (history.size <= 1) history = listOf(it) }
                    msg.clocks()?.let { whiteTime = it.first; blackTime = it.second }
                    opponentJoined = true
                    playSfx("notify")
                    if (!AppForeground.value) postLocalNotification("Boka ♟", "An opponent joined — your game is on!")
                }
                "move_made" -> {
                    val newFen = msg.extractFen()
                    msg.clocks()?.let { whiteTime = it.first; blackTime = it.second }
                    if (newFen != null) {
                        if (fenKey(newFen) == fenKey(history.last())) {
                            // Our own optimistic move, confirmed by the server — reconcile the
                            // authoritative FEN (exact counters) without adding a ply or
                            // replaying the sound (already played when the piece was tapped).
                            history = history.dropLast(1) + newFen
                            selected = null
                        } else {
                            // Opponent's move (or a correction) — append and react.
                            val capture = pieceCount(newFen) < pieceCount(history.last())
                            var lm: Pair<String, String>? = null
                            (msg["lastMove"] as? JsonObject)?.let { j ->
                                val f = j["from"]?.jsonPrimitive?.content; val t = j["to"]?.jsonPrimitive?.content
                                if (f != null && t != null) lm = f to t
                            }
                            history = history + newFen
                            lastMoves = lastMoves + lm
                            viewPly = null
                            selected = null
                            opponentJoined = true
                            val pos = runCatching { Position.fromFen(newFen) }.getOrNull()
                            val inCheck = pos?.inCheck() ?: false
                            playSfx(if (inCheck) "check" else if (capture) "capture" else "move")
                            val myTurn = pos != null && pos.whiteToMove == (myColor == "white")
                            if (myTurn && !AppForeground.value) {
                                postLocalNotification("Boka ♟", if (inCheck) "Check! It's your move." else "Your opponent moved — your turn!")
                            }
                        }
                    }
                }
                "time_update" -> msg.clocks()?.let { whiteTime = it.first; blackTime = it.second }
                "game_ended" -> {
                    val restart = forfeitThenStart
                    if (restart != null) {
                        // We forfeited the previous game to start a new one — reset, then create.
                        forfeitThenStart = null
                        gameId = null; ended = null; opponentJoined = false; selected = null
                        history = listOf(START_FEN); lastMoves = listOf(null); viewPly = null
                        mode = restart
                    } else {
                        val reason = msg["reason"]?.jsonPrimitive?.content ?: "over"
                        val winner = msg["winner"]?.jsonPrimitive?.content
                        val info = EndInfo(reason, won = winner?.let { it == myColor })
                        ended = info
                        playSfx("notify")
                        if (!AppForeground.value) {
                            val headline = when (info.won) {
                                true -> "You won — $reason!"
                                false -> if (reason == "timeout") "Time's up — you lost on time." else "You lost — $reason."
                                null -> "Draw ($reason)."
                            }
                            postLocalNotification("Boka ♟", headline)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { scope.launch { socket.disconnect() } } }

    fun cancel() {
        socket.sendType("cancel_matchmaking")
        scope.launch { socket.disconnect() }
        mode = null; gameId = null; opponentJoined = false
        history = listOf(START_FEN); lastMoves = listOf(null); viewPly = null
    }

    fun tap(sq: String) {
        if (reviewing || ended != null || gameId == null || myColor == null) return
        val cur = selected
        if (cur == null) { selected = sq; return }
        selected = null
        if (cur == sq) return

        val curFen = history.last()
        val pos = runCatching { Position.fromFen(curFen) }.getOrNull()
        val myTurn = pos != null && pos.whiteToMove == (myColor == "white")
        val legal = if (myTurn) pos!!.legalMoves()
            .firstOrNull { it.from == nameToSquare(cur) && it.to == nameToSquare(sq) } else null

        if (legal != null) {
            // Optimistic move: apply locally the instant it's tapped so the piece moves
            // with ZERO network latency (this was the "laggy" feel — the board used to
            // wait for the server to echo the move back). The server's move_made is
            // deduped against this. Promotions default to queen to match the server.
            val promo = if (legal.promo != null) 'q' else null
            val next = pos!!.makeMove(legal.from, legal.to, promo)
            if (next != null) {
                val newFen = next.toFen()
                val capture = pieceCount(newFen) < pieceCount(curFen)
                history = history + newFen
                lastMoves = lastMoves + (cur to sq)
                viewPly = null
                playSfx(if (next.inCheck()) "check" else if (capture) "capture" else "move")
            }
            socket.sendType("make_move", "gameId" to gameId,
                "move" to buildJsonObject { put("from", JsonPrimitive(cur)); put("to", JsonPrimitive(sq)) })
        } else if (myTurn && pos!!.legalMoves().any { it.from == nameToSquare(sq) }) {
            // Tapped another of my own movable pieces — reselect it.
            selected = sq
        }
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "♟ Play Online", onBack = { if (mode != null && !opponentJoined && ended == null) cancel() else onBack() })
        errorMsg?.let { em ->
            Box(Modifier.fillMaxWidth().background(Boka.danger.copy(alpha = 0.15f)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(em, color = Boka.danger, fontSize = 13.sp)
            }
            LaunchedEffect(em) { delay(4000); errorMsg = null }
        }

        when {
            mode == null -> ModePicker(
                checking = checkingResume,
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true; checkingResume = true; resumableGameId = null
                    scope.launch { socket.disconnect(); socket.reconnect() }
                },
                resumableGameId = resumableGameId,
                onResume = {
                    val g = resumableGameId
                    if (g != null) { pendingEndAction = null; gameId = g; socket.sendType("resume_game", "gameId" to g) }
                },
                onPick = { tc ->
                    when {
                        checkingResume -> pendingPick = tc            // wait for the check to finish
                        resumableGameId != null -> pendingNewGame = tc
                        else -> mode = tc
                    }
                },
            )
            !opponentJoined && ended == null -> WaitingView(mode!!, onCancel = ::cancel)
            else -> PlayingView(
                shownFen, liveFen, myColor, selected, lastMoves[currentIndex], reviewing, ended, gameId != null,
                whiteTime, blackTime, maxIndex, currentIndex,
                onTap = ::tap,
                onFirst = { viewPly = 0 },
                onPrev = { viewPly = (currentIndex - 1).coerceAtLeast(0) },
                onNext = { val n = currentIndex + 1; viewPly = if (n >= maxIndex) null else n },
                onLive = { viewPly = null },
                onResign = { socket.sendType("resign", "gameId" to gameId); ended = EndInfo("resignation", won = false) },
                onDone = onBack,
            )
        }

        // Guard: user tried to start a new game while one is still unfinished.
        pendingNewGame?.let { tc ->
            val abortable = resumablePlies <= 1   // no real moves yet -> end without penalty
            AlertDialog(
                onDismissRequest = { pendingNewGame = null },
                title = { Text(if (abortable) "Game not started" else "Game in progress", color = Boka.text, fontWeight = FontWeight.Bold) },
                text = { Text(
                    if (abortable) "You have a game you haven't played yet. End it (no result) and start a new one, or rejoin it."
                    else "You already have a game in progress. Forfeit it to start a new one, or rejoin it.",
                    color = Boka.textMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        val g = resumableGameId
                        pendingNewGame = null
                        if (g != null) { pendingEndAction = null; gameId = g; socket.sendType("resume_game", "gameId" to g) }
                    }) { Text("Rejoin", color = Boka.gold, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val g = resumableGameId
                        pendingNewGame = null
                        if (g != null) {
                            pendingEndAction = (if (abortable) "abort_game" else "resign") to tc
                            socket.sendType("resume_game", "gameId" to g)   // attach first, then end in game_resumed
                        }
                    }) { Text(if (abortable) "End & start new" else "Forfeit & start new", color = Boka.danger) }
                },
                containerColor = Boka.surface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Time-control picker
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModePicker(
    checking: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    resumableGameId: String? = null,
    onResume: () -> Unit = {},
    onPick: (TimeControl) -> Unit,
) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmer.animateFloat(
        0.35f, 0.8f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "shimmerA")
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (checking) {
            // Checking for an unfinished game — placeholder shimmer so nothing pops in.
            Box(
                Modifier.fillMaxWidth().padding(bottom = 14.dp).height(66.dp)
                    .graphicsLayer { alpha = shimmerAlpha }
                    .clip(RoundedCornerShape(14.dp)).background(Boka.surfaceAlt),
            )
        } else if (resumableGameId != null) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Boka.goldSoft)
                    .border(1.dp, Boka.gold, RoundedCornerShape(14.dp))
                    .clickable(onClick = onResume)
                    .padding(16.dp),
            ) {
                Text("Game in progress", color = Boka.gold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("Tap to rejoin your unfinished game.", color = Boka.textMuted, fontSize = 12.sp)
            }
        }
        Text("Choose a time control", color = Boka.text,
            fontFamily = ke.co.brivont.boka.ui.theme.BokaType.serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("You'll be matched with an opponent of similar strength.", color = Boka.textFaint, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        TIME_CONTROLS.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { tc -> ModeCard(tc, Modifier.weight(1f)) { onPick(tc) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    }
}

@Composable
private fun ModeCard(tc: TimeControl, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f)
    val emoji = when (tc.cat) { "Bullet" -> "⚡"; "Blitz" -> "🔥"; else -> "⏱" }
    Column(
        modifier.scale(scale).clip(RoundedCornerShape(14.dp)).background(Boka.surface)
            .border(1.dp, Boka.border, RoundedCornerShape(14.dp))
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(tc.label, color = Boka.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(tc.cat, color = Boka.textFaint, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// Matchmaking / waiting screen with a fading chess piece
// ---------------------------------------------------------------------------
@Composable
private fun WaitingView(tc: TimeControl, onCancel: () -> Unit) {
    // Mirrors the web loader: the ♟️ emoji (3D glossy artwork) bouncing, with a
    // ground shadow that squashes in counterpoint for depth. All animated values
    // are read in graphicsLayer blocks (layer phase) — zero per-frame redraws.
    val t = rememberInfiniteTransition(label = "wait")
    val hop by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hop",
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.BottomCenter) {
            // Ground shadow: widest + darkest when the piece lands.
            Box(
                Modifier
                    .size(width = 70.dp, height = 14.dp)
                    .graphicsLayer {
                        scaleX = 1.15f - hop * 0.45f
                        alpha = 0.35f - hop * 0.22f
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF000000)),
            )
            Text(
                "♟️", fontSize = 72.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = -hop * 34.dp.toPx() - 14.dp.toPx()
                    scaleY = 1f + hop * 0.06f   // slight stretch at the top of the hop
                },
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Finding an opponent…", color = Boka.goldBright, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text("${tc.label} · ${tc.cat}", color = Boka.textFaint, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        SecondaryButton("Cancel", onCancel, Modifier.fillMaxWidth(0.6f))
    }
}

// ---------------------------------------------------------------------------
// Playing board with clocks
// ---------------------------------------------------------------------------
@Composable
private fun PlayingView(
    shownFen: String, liveFen: String, myColor: String?, selected: String?,
    lastMove: Pair<String, String>?, reviewing: Boolean, ended: EndInfo?, hasGame: Boolean,
    whiteTime: Int, blackTime: Int, maxIndex: Int, currentIndex: Int,
    onTap: (String) -> Unit, onFirst: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    onLive: () -> Unit, onResign: () -> Unit, onDone: () -> Unit,
) {
    val whiteToMove = remember(liveFen) { runCatching { Position.fromFen(liveFen).whiteToMove }.getOrDefault(true) }
    val iAmWhite = myColor != "black"
    val myTime = if (iAmWhite) whiteTime else blackTime
    val oppTime = if (iAmWhite) blackTime else whiteTime
    val gameLive = ended == null && !reviewing
    val myTurn = whiteToMove == iAmWhite

    val targets: Set<String> = remember(liveFen, selected, reviewing) {
        val sel = selected
        if (sel == null || reviewing) emptySet()
        else runCatching {
            val pos = Position.fromFen(liveFen)
            val f = nameToSquare(sel)
            pos.legalMoves().filter { it.from == f }.map { squareName(it.to) }.toSet()
        }.getOrDefault(emptySet())
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        PlayerBar("Opponent", if (iAmWhite) "Black" else "White", oppTime, active = gameLive && !myTurn)
        Spacer(Modifier.height(8.dp))
        Box {
            ChessBoard(
                fen = shownFen,
                orientation = if (myColor == "black") "black" else "white",
                selected = if (reviewing) null else selected,
                lastMove = lastMove,
                targets = targets,
                enabled = !reviewing && ended == null && hasGame,
                onSquareTap = onTap,
            )
            if (ended != null) GameOverOverlay(ended, onDone)
        }
        Spacer(Modifier.height(8.dp))
        PlayerBar("You", if (iAmWhite) "White" else "Black", myTime, active = gameLive && myTurn)

        Spacer(Modifier.height(10.dp))
        if (maxIndex > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("⏮", onFirst, Modifier.weight(1f))
                SecondaryButton("‹", onPrev, Modifier.weight(1f))
                SecondaryButton("›", onNext, Modifier.weight(1f))
                SecondaryButton("Live", onLive, Modifier.weight(1f))
            }
        }
        if (ended == null && hasGame) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton("🏳 Resign", onResign, Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------
// Animated game-over alert (timeout gets a pulsing clock, chess.com-style)
// ---------------------------------------------------------------------------
@Composable
private fun androidx.compose.foundation.layout.BoxScope.GameOverOverlay(info: EndInfo, onDone: () -> Unit) {
    // Card pops in with a spring; the scrim fades in.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
    }
    // Timeout icon pulses to make the flag unmissable.
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        1f, 1.22f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "ps",
    )

    val (emoji, title) = when {
        info.reason == "timeout" && info.won == false -> "⏰" to "Time's up!"
        info.reason == "timeout" -> "⏰" to "Opponent flagged!"
        info.reason == "checkmate" -> "♛" to "Checkmate"
        info.reason == "resignation" -> "🏳" to "Resignation"
        info.won == null -> "🤝" to "Draw"
        else -> "🏁" to "Game over"
    }
    val subtitle = when (info.won) {
        true -> if (info.reason == "timeout") "You win — your opponent ran out of time." else "You won — ${info.reason}."
        false -> if (info.reason == "timeout") "You lost on time." else "You lost — ${info.reason}."
        null -> "Game drawn (${info.reason})."
    }
    val accent = when (info.won) { true -> Boka.success; false -> Boka.danger; null -> Boka.textMuted }

    Box(
        Modifier.matchParentSize()
            .graphicsLayer { alpha = appear.value }
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.82f)
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                .clip(RoundedCornerShape(16.dp))
                .background(Boka.surface)
                .border(2.dp, accent, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                emoji, fontSize = 44.sp,
                modifier = if (info.reason == "timeout") Modifier.graphicsLayer {
                    scaleX = pulseScale; scaleY = pulseScale
                } else Modifier,
            )
            Spacer(Modifier.height(10.dp))
            Text(title, color = Boka.text, fontFamily = ke.co.brivont.boka.ui.theme.BokaType.serif,
                fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Boka.textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Done", onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlayerBar(name: String, color: String, seconds: Int, active: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("$name · $color", color = Boka.textMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Box(
            Modifier.clip(RoundedCornerShape(10.dp))
                .background(if (active) Boka.goldSoft else Boka.surface)
                .border(1.dp, if (active) Boka.gold else Boka.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                fmtClock(seconds),
                color = if (active) Boka.goldBright else Boka.text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            )
        }
    }
}
