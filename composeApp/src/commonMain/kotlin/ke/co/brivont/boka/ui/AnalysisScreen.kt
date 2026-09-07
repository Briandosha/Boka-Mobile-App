package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.AnalysisMove
import ke.co.brivont.boka.data.GameApi
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.board.START_FEN
import ke.co.brivont.boka.ui.theme.Boka
import kotlinx.coroutines.delay

private fun badge(cls: String): Pair<String, androidx.compose.ui.graphics.Color> = when (cls) {
    "brilliant" -> "Brilliant !!" to Boka.success
    "great" -> "Great !" to Boka.goldBright
    "best" -> "Best ★" to Boka.success
    "good" -> "Good ✓" to Boka.textMuted
    "inaccuracy" -> "Inaccuracy ?!" to Boka.warning
    "mistake" -> "Mistake ?" to Boka.warning
    "blunder" -> "Blunder ??" to Boka.danger
    else -> "Book 📖" to Boka.textFaint
}

@Composable
fun AnalysisScreen(pgn: String, playerColor: String, onBack: () -> Unit, onUpsell: () -> Unit) {
    var report by remember { mutableStateOf<List<AnalysisMove>?>(null) }
    var status by remember { mutableStateOf("Requesting analysis…") }
    var limit by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var idx by remember { mutableStateOf(0) }

    LaunchedEffect(pgn) {
        try {
            val enq = GameApi.enqueueAnalysis(pgn)
            if (enq.error == "free_limit_reached") { limit = true; return@LaunchedEffect }
            if (enq.status == "done" && enq.report != null) { report = enq.report; return@LaunchedEffect }
            val jobId = enq.jobId ?: run { error = "Couldn't start analysis."; return@LaunchedEffect }
            repeat(120) {
                delay(1500)
                val p = GameApi.pollAnalysis(jobId)
                when (p.status) {
                    "queued" -> status = "You're #${p.position ?: 1} in line…"
                    "running" -> status = "Stockfish is analysing…"
                    "done" -> { report = p.report ?: emptyList(); return@LaunchedEffect }
                    "limit" -> { limit = true; return@LaunchedEffect }
                    "error" -> { error = p.message ?: "Analysis failed."; return@LaunchedEffect }
                }
            }
            error = "Analysis timed out — please try again."
        } catch (e: Throwable) { error = "We couldn't analyse this game." }
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "🔎 Game Review", onBack = onBack)
        when {
            limit -> Column(Modifier.padding(24.dp)) {
                Text("👑 Free reviews used up", color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("You've used your free game reviews. Subscribe to Premium for unlimited coached analysis.", color = Boka.textMuted)
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Go Premium", onUpsell)
            }
            error != null -> Text(error!!, color = Boka.textMuted, modifier = Modifier.padding(24.dp))
            report == null -> Column { Loading(); Text(status, color = Boka.textFaint, modifier = Modifier.padding(16.dp).fillMaxWidth(), fontSize = 13.sp) }
            report!!.isEmpty() -> Text("Nothing to analyse in this game.", color = Boka.textMuted, modifier = Modifier.padding(24.dp))
            else -> {
                val moves = report!!
                val cur = moves[idx.coerceIn(0, moves.lastIndex)]
                val lan = cur.move_lan
                val lastMove = if (lan.length >= 4) lan.substring(0, 2) to lan.substring(2, 4) else null
                Column(Modifier.padding(16.dp)) {
                    ChessBoard(fen = cur.fen.ifBlank { START_FEN }, orientation = if (playerColor == "b") "black" else "white",
                        lastMove = lastMove, enabled = false)
                    Spacer(Modifier.height(10.dp))
                    val (label, color) = badge(cur.classification)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(idx / 2) + 1}. ${cur.move_san}", color = Boka.text, fontWeight = FontWeight.Bold)
                        Text(label, color = color, fontWeight = FontWeight.Bold)
                    }
                    cur.reason?.let { Text(it, color = Boka.textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("⏮", { idx = 0 }, Modifier.weight(1f))
                        SecondaryButton("‹", { idx = (idx - 1).coerceAtLeast(0) }, Modifier.weight(1f))
                        SecondaryButton("›", { idx = (idx + 1).coerceAtMost(moves.lastIndex) }, Modifier.weight(1f))
                        SecondaryButton("⏭", { idx = moves.lastIndex }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Move ${idx + 1} of ${moves.size}", color = Boka.textFaint, fontSize = 12.sp)
                }
            }
        }
    }
}
