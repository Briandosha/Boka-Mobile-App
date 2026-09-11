package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ke.co.brivont.boka.chess.Position
import ke.co.brivont.boka.chess.nameToSquare
import ke.co.brivont.boka.chess.squareName
import ke.co.brivont.boka.data.PuzzleApi
import ke.co.brivont.boka.data.PuzzleResult
import ke.co.brivont.boka.data.PuzzleDto
import ke.co.brivont.boka.data.AttemptBody
import ke.co.brivont.boka.data.OfflinePuzzles
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.theme.Boka

private const val PUZZLE_START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
private const val RUSH_STRIKES = 3
private const val RUSH_BATCH = 40

/**
 * Tactics trainer (mobile). Mirrors the web PuzzleModule: it loads a Lichess CC0
 * puzzle, auto-plays the setup move, validates the solver's tap-moves against the
 * solution using the on-device engine, auto-replies for the opponent, and reports
 * the result to the server (Glicko rating + daily streak).
 */
@Composable
fun PuzzleScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("practice") }         // practice | daily
    var status by remember { mutableStateOf("loading") }        // loading|solving|solved|failed|empty|limit
    var puzzleId by remember { mutableStateOf<String?>(null) }
    var solution by remember { mutableStateOf<List<String>>(emptyList()) }
    var idx by remember { mutableStateOf(1) }
    var fen by remember { mutableStateOf(PUZZLE_START) }
    var orientation by remember { mutableStateOf("white") }
    var solverWhite by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<String?>(null) }
    var lastMove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var puzzleRatingLabel by remember { mutableStateOf<Int?>(null) }

    var rating by remember { mutableStateOf<Int?>(null) }
    var streak by remember { mutableStateOf(0) }
    var bestStreak by remember { mutableStateOf(0) }
    var delta by remember { mutableStateOf<Int?>(null) }
    var offline by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }
    var queued by remember { mutableStateOf(0) }
    var rushScore by remember { mutableStateOf(0) }
    var rushStrikes by remember { mutableStateOf(0) }
    var rushBest by remember { mutableStateOf(0) }
    var rushIsBest by remember { mutableStateOf(false) }
    var rushQueue by remember { mutableStateOf<List<PuzzleDto>>(emptyList()) }
    var rushPos by remember { mutableStateOf(0) }

    fun submit(solved: Boolean) {
        val pid = puzzleId ?: return
        scope.launch {
            val r = PuzzleApi.attempt(pid, solved)
            if (r != null) {
                rating = r.puzzleRating; delta = r.delta
                streak = r.streak; bestStreak = r.bestStreak
            } else {
                // Offline (or a server hiccup): keep the result and sync it later.
                OfflinePuzzles.enqueue(AttemptBody(pid, solved))
                queued = OfflinePuzzles.pendingCount()
            }
        }
    }

    // Set the board up from a puzzle. Returns false if the puzzle is malformed.
    fun applyPuzzle(p: PuzzleDto, m: String): Boolean {
        val moves = p.moves.trim().split(" ").filter { it.isNotEmpty() }
        val start = runCatching { Position.fromFen(p.fen) }.getOrNull() ?: return false
        if (moves.isEmpty()) return false
        val setup = moves[0]
        val after = start.makeMove(nameToSquare(setup.substring(0, 2)), nameToSquare(setup.substring(2, 4)), setup.getOrNull(4)) ?: return false
        puzzleId = p.id; solution = moves; idx = 1
        fen = after.toFen(); solverWhite = after.whiteToMove
        orientation = if (after.whiteToMove) "white" else "black"
        lastMove = setup.substring(0, 2) to setup.substring(2, 4)
        puzzleRatingLabel = p.rating
        p.puzzleRating?.let { rating = it }
        p.streak?.let { streak = it }
        status = if (m == "daily" && p.solvedToday == true) "solved" else "solving"
        return true
    }

    fun startRush() {
        status = "loading"; selected = null; delta = null
        rushScore = 0; rushStrikes = 0; rushIsBest = false; rushPos = 0
        scope.launch {
            val list = PuzzleApi.batch(RUSH_BATCH) ?: emptyList()
            if (list.isEmpty()) { status = "empty"; return@launch }
            rushQueue = list
            if (!applyPuzzle(list[0], "rush")) status = "empty"
        }
    }

    fun endRush() {
        status = "rushover"
        scope.launch { PuzzleApi.rushResult(rushScore)?.let { rushBest = it.rushBest; rushIsBest = it.isBest } }
    }

    // Next Rush puzzle after a short beat; tops the queue up if it runs dry.
    fun advanceRush() {
        scope.launch {
            delay(if (status == "solved") 450L else 700L)
            val next = rushPos + 1
            if (next >= rushQueue.size) {
                val more = PuzzleApi.batch(RUSH_BATCH) ?: emptyList()
                val have = rushQueue.map { it.id }.toHashSet()
                rushQueue = rushQueue + more.filter { it.id !in have }
                if (next >= rushQueue.size) { endRush(); return@launch }
            }
            rushPos = next
            if (!applyPuzzle(rushQueue[next], "rush")) endRush()
        }
    }

    fun load(m: String) {
        if (m == "rush") { startRush(); return }
        status = "loading"; selected = null; delta = null
        scope.launch {
            when (val r = if (m == "daily") PuzzleApi.daily() else PuzzleApi.next()) {
                is PuzzleResult.Limit -> status = "limit"
                is PuzzleResult.Empty -> status = "empty"
                is PuzzleResult.Loaded -> {
                    offline = false
                    if (!applyPuzzle(r.p, m)) status = "empty"
                    // We're online: sync anything solved offline, then top up the offline cache.
                    OfflinePuzzles.flushPending()
                    OfflinePuzzles.refillIfLow()
                    queued = OfflinePuzzles.pendingCount()
                    savedCount = OfflinePuzzles.cachedCount()
                }
                is PuzzleResult.Error -> {
                    // No connection — fall back to puzzles saved for offline (Practice only;
                    // the shared Daily puzzle needs the server).
                    val cached = if (m == "practice") OfflinePuzzles.pop() else null
                    if (cached != null) {
                        offline = true
                        if (!applyPuzzle(cached, m)) status = "empty"
                        savedCount = OfflinePuzzles.cachedCount()
                    } else {
                        status = if (m == "daily") "offline_daily" else "offline_empty"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        savedCount = OfflinePuzzles.cachedCount(); queued = OfflinePuzzles.pendingCount()
        PuzzleApi.progress()?.let { rating = it.puzzleRating; streak = it.streak; bestStreak = it.bestStreak; rushBest = it.rushBest }
    }
    LaunchedEffect(mode) { load(mode) }

    val targets: Set<String> = remember(fen, selected, status) {
        val cur = selected
        if (cur == null || status != "solving") emptySet()
        else runCatching {
            Position.fromFen(fen).legalMoves()
                .filter { squareName(it.from) == cur }
                .map { squareName(it.to) }.toSet()
        }.getOrDefault(emptySet())
    }

    fun onWrong() {
        submit(false)
        if (mode == "rush") {
            rushStrikes += 1
            if (rushStrikes >= RUSH_STRIKES) { endRush(); return }
            status = "failed"; advanceRush()
        } else status = "failed"
    }

    fun onRight() {
        submit(true)
        if (mode == "rush") { rushScore += 1; status = "solved"; advanceRush() } else status = "solved"
    }

    fun tap(sq: String) {
        if (status != "solving") return
        val pos = runCatching { Position.fromFen(fen) }.getOrNull() ?: return
        val cur = selected
        if (cur == null) {
            if (pos.legalMoves().any { squareName(it.from) == sq }) selected = sq
            return
        }
        if (cur == sq) { selected = null; return }
        val legal = pos.legalMoves().firstOrNull { squareName(it.from) == cur && squareName(it.to) == sq }
        if (legal == null) {
            selected = if (pos.legalMoves().any { squareName(it.from) == sq }) sq else null
            return
        }
        selected = null
        val expected = solution.getOrNull(idx) ?: return
        val promo = if (expected.length == 5) expected[4] else null
        val uci = cur + sq + (promo?.toString() ?: "")
        if (uci != expected) { onWrong(); return }

        val after = pos.makeMove(nameToSquare(cur), nameToSquare(sq), promo) ?: return
        idx += 1
        fen = after.toFen(); lastMove = cur to sq
        if (idx >= solution.size) { onRight(); return }

        val reply = solution[idx]; idx += 1
        scope.launch {
            delay(300)
            val afterReply = runCatching { Position.fromFen(fen) }.getOrNull()
                ?.makeMove(nameToSquare(reply.substring(0, 2)), nameToSquare(reply.substring(2, 4)), reply.getOrNull(4)) ?: return@launch
            fen = afterReply.toFen(); lastMove = reply.substring(0, 2) to reply.substring(2, 4)
            if (idx >= solution.size) onRight()
        }
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "♟ Puzzles", onBack = onBack)

        // Mode toggle
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("Practice", mode == "practice") { if (mode != "practice") mode = "practice" else load("practice") }
            ModeChip("Daily", mode == "daily") { if (mode != "daily") mode = "daily" }
            ModeChip("Rush", mode == "rush") { if (mode != "rush") mode = "rush" else startRush() }
        }
        // Offline status: what's saved locally and what's waiting to sync.
        val offlineNote = when {
            offline -> "Offline \u00b7 playing saved puzzles" + (if (queued > 0) " \u00b7 $queued to sync" else "")
            savedCount > 0 || queued > 0 -> buildString {
                if (savedCount > 0) append("$savedCount saved for offline")
                if (queued > 0) { if (isNotEmpty()) append(" \u00b7 "); append("$queued to sync") }
            }
            else -> ""
        }
        if (offlineNote.isNotEmpty()) {
            Text(offlineNote, color = if (offline) Boka.gold else Boka.textMuted, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 18.dp))
        }

        // Stat strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mode == "rush") {
                StatCell("Score", rushScore.toString(), Modifier.weight(1f))
                StatCell("Strikes", "♥".repeat(RUSH_STRIKES - rushStrikes) + "♡".repeat(rushStrikes), Modifier.weight(1f))
                StatCell("Best rush", rushBest.toString(), Modifier.weight(1f))
            } else {
                StatCell("Rating", rating?.toString() ?: "—", Modifier.weight(1f))
                StatCell("Streak", streak.toString(), Modifier.weight(1f))
                StatCell("Best", bestStreak.toString(), Modifier.weight(1f))
            }
        }

        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            ChessBoard(
                fen = fen,
                orientation = orientation,
                selected = selected,
                lastMove = lastMove,
                targets = targets,
                enabled = status == "solving",
                onSquareTap = ::tap,
            )
        }

        // Prompt / result
        val banner: Triple<String, Color, Color> = when (status) {
            "loading" -> Triple("Loading…", Boka.textMuted, Boka.surface)
            "empty" -> Triple("Puzzles are still loading on the server. Try again soon.", Boka.textMuted, Boka.surface)
            "limit" -> Triple("That's your free puzzles for today. Go Premium for unlimited.", Boka.textMuted, Boka.surface)
            "offline_empty" -> Triple("You're offline and no puzzles are saved yet. Connect once and they'll download automatically.", Boka.textMuted, Boka.surface)
            "offline_daily" -> Triple("The daily puzzle needs a connection. Try Practice \u2014 saved puzzles work offline.", Boka.textMuted, Boka.surface)
            "solving" -> Triple("${if (solverWhite) "White" else "Black"} to move" + (puzzleRatingLabel?.let { " · rated $it" } ?: "") + ". Find the best move.", Boka.text, Boka.surface)
            "solved" -> if (mode == "rush") Triple("Correct! Next one…", Boka.success, Boka.surface)
                else Triple("Solved!" + (delta?.let { "  ${if (it >= 0) "+$it" else "$it"}" } ?: ""), Boka.success, Boka.surface)
            "failed" -> if (mode == "rush") Triple("Strike $rushStrikes of $RUSH_STRIKES. Keep going…", Boka.danger, Boka.surface)
                else Triple("Not the best move." + (delta?.let { "  $it" } ?: ""), Boka.danger, Boka.surface)
            "rushover" -> Triple("Run over — you scored $rushScore." + (if (rushIsBest) " New personal best!" else "  Best: $rushBest"), Boka.gold, Boka.surface)
            else -> Triple("", Boka.text, Boka.surface)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(banner.third).padding(14.dp)) {
                Text(banner.first, color = banner.second, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            when (status) {
                "solved", "failed" -> if (mode == "practice") PrimaryButton("Next puzzle", { load("practice") }, modifier = Modifier.fillMaxWidth())
                "empty", "limit", "offline_empty", "offline_daily" -> SecondaryButton("Retry", { load(mode) }, Modifier.fillMaxWidth())
                "solving" -> SecondaryButton(if (mode == "rush") "Skip (strike)" else "Skip", { onWrong() }, Modifier.fillMaxWidth())
                "rushover" -> PrimaryButton("Play again", { startRush() }, modifier = Modifier.fillMaxWidth())
                else -> {}
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) Boka.gold else Boka.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Boka.ground else Boka.textMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Boka.surface).padding(vertical = 10.dp, horizontal = 12.dp)) {
        Text(label.uppercase(), color = Boka.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Boka.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
