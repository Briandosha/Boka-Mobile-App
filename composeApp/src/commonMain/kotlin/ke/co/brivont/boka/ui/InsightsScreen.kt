package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.chess.Position
import ke.co.brivont.boka.chess.squareName
import ke.co.brivont.boka.data.ExampleDto
import ke.co.brivont.boka.data.InsightsApi
import ke.co.brivont.boka.data.InsightsDto
import ke.co.brivont.boka.data.OpeningRowDto
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.theme.Boka

/**
 * Personalized weakness report — reads /api/insights and renders it as coaching.
 * Deep sections (openings, mistakes, replay) are Premium: the server sends
 * { locked, teaser } and we show a Go Premium card instead.
 */
@Composable
fun InsightsScreen(onBack: () -> Unit, onPractice: () -> Unit, onUpsell: () -> Unit) {
    var state by remember { mutableStateOf("loading") }   // loading | ready | error
    var data by remember { mutableStateOf<InsightsDto?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        state = "loading"
        val d = InsightsApi.insights()
        if (d == null) state = "error" else { data = d; state = "ready" }
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "◆ Insights", onBack = onBack)

        when (state) {
            "loading" -> Box(Modifier.fillMaxWidth().padding(24.dp)) { Text("Reading your games and puzzles…", color = Boka.textMuted) }
            "error" -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Couldn't load your insights right now.", color = Boka.textMuted)
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Retry", { reloadKey++ }, Modifier.fillMaxWidth())
            }
            "ready" -> {
                val d = data!!
                val g = d.games; val op = d.openings; val mk = d.mistakes
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    // Headline
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Boka.surface).padding(18.dp)) {
                        Label("YOUR COACH SAYS", Boka.gold)
                        Spacer(Modifier.height(6.dp))
                        Text(d.headline, color = Boka.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                    }

                    // Personal recap status (rendered slowly in the background; watch on the web)
                    d.recap?.let { r ->
                        Spacer(Modifier.height(10.dp))
                        Card {
                            Text(
                                when (r.status) {
                                    "done" -> "Your personal video recap is ready — open Insights on the web to watch it."
                                    "rendering" -> "Your personal video recap is being rendered in the background."
                                    else -> "Your personal video recap is queued — it's built when the server is quiet, so it may take a while."
                                },
                                color = Boka.textMuted, fontSize = 13.sp, lineHeight = 18.sp,
                            )
                        }
                    }

                    // Games
                    if (g.total > 0) {
                        SectionHeader("YOUR GAMES (${g.total})")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatBox("Win rate", "${g.winRate}%", Modifier.weight(1f), Boka.gold)
                            StatBox("W/L/D", "${g.wins}/${g.losses}/${g.draws}", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatBox("As White", if (g.byColor.white.games > 0) "${g.byColor.white.winRate}%" else "—", Modifier.weight(1f))
                            StatBox("As Black", if (g.byColor.black.games > 0) "${g.byColor.black.winRate}%" else "—", Modifier.weight(1f))
                        }
                        Bullets(g.insights)
                    }

                    // Openings
                    if (op.locked) {
                        SectionHeader("YOUR OPENINGS")
                        LockedCard(op.teaser, onUpsell)
                    } else if (op.weakest.isNotEmpty() || op.asBlackVs.isNotEmpty()) {
                        SectionHeader("YOUR OPENINGS")
                        if (op.weakest.isNotEmpty()) Card { Label("STRUGGLING IN", Boka.danger); op.weakest.forEach { OpeningLine(it, Boka.danger) } }
                        if (op.strongest.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Card { Label("YOUR BEST", Boka.success); op.strongest.forEach { OpeningLine(it, Boka.success) } } }
                        if (op.asBlackVs.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Card {
                                Label("AS BLACK, AGAINST…", Boka.gold)
                                op.asBlackVs.forEach { r ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(r.family, color = Boka.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${r.games} games", color = Boka.textMuted, fontSize = 12.sp)
                                        Text("${r.winRate}%", color = if (r.winRate < 45) Boka.danger else Boka.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Bullets(op.insights)
                    }

                    // Mistakes
                    if (mk.locked) {
                        SectionHeader("WHERE YOU GO WRONG")
                        LockedCard(mk.teaser, onUpsell)
                    } else if (mk.total > 0 || mk.pendingGames > 0) {
                        SectionHeader("WHERE YOU GO WRONG")
                        if (mk.total > 0) {
                            Card {
                                mk.byCategory.forEach { c ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(c.label, color = Boka.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("${c.count} · ${c.share}%", color = Boka.textMuted, fontSize = 12.sp)
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Boka.border)) {
                                            Box(Modifier.fillMaxWidth(c.share / 100f).height(5.dp).background(Boka.gold))
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Text(c.advice, color = Boka.textMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
                                    }
                                }
                                val phases = mk.byPhase.filter { it.count > 0 }.joinToString(" · ") { "${phaseName(it.phase)} ${it.share}%" }
                                if (phases.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text("By phase: $phases", color = Boka.textMuted, fontSize = 12.5.sp) }
                                if (mk.errorProneOpenings.isNotEmpty()) {
                                    Text("Most slips in: " + mk.errorProneOpenings.joinToString(", ") { "${it.name} (${it.count})" }, color = Boka.textMuted, fontSize = 12.5.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Analyzed ${mk.analyzedGames} recent game${if (mk.analyzedGames == 1) "" else "s"}" +
                                (if (mk.pendingGames > 0) " · analyzing ${mk.pendingGames} more in the background — pull back in a few minutes" else "") + ".",
                            color = Boka.textMuted, fontSize = 11.5.sp,
                        )
                        Bullets(mk.insights)
                    }

                    // Replay your mistakes (find the better move)
                    if (!mk.locked && mk.examples.isNotEmpty()) {
                        SectionHeader("REPLAY YOUR MISTAKES")
                        Text("Real positions from your games where you went wrong. Tap a piece, then a square, to find the stronger move.", color = Boka.textMuted, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        mk.examples.forEach { ex -> ReplayCard(ex); Spacer(Modifier.height(12.dp)) }
                    }

                    // Puzzle weaknesses
                    if (d.puzzleWeaknesses.isNotEmpty()) {
                        SectionHeader("TACTICS TO DRILL")
                        d.puzzleWeaknesses.forEach { w ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(Boka.surface).padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(w.label, color = Boka.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("miss ${w.failRate}%", color = Boka.danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(w.tip, color = Boka.textMuted, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Practice puzzles", onPractice, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** A real position from the user's game: tap a piece then a square to find the stronger move. */
@Composable
private fun ReplayCard(ex: ExampleDto) {
    var selected by remember { mutableStateOf<String?>(null) }
    var tried by remember { mutableStateOf<Pair<String, String>?>(null) }
    var solved by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val done = solved || revealed
    val pos = remember(ex.fen) { runCatching { Position.fromFen(ex.fen) }.getOrNull() }
    val bestFrom = ex.best.take(2)
    val bestTo = if (ex.best.length >= 4) ex.best.substring(2, 4) else ""

    val targets: Set<String> = remember(selected, done, ex.fen) {
        val cur = selected
        if (cur == null || done || pos == null) emptySet()
        else pos.legalMoves().filter { squareName(it.from) == cur }.map { squareName(it.to) }.toSet()
    }

    fun tap(sq: String) {
        if (done || pos == null) return
        val cur = selected
        if (cur == null) { if (pos.legalMoves().any { squareName(it.from) == sq }) selected = sq; return }
        if (cur == sq) { selected = null; return }
        val legal = pos.legalMoves().any { squareName(it.from) == cur && squareName(it.to) == sq }
        if (!legal) { selected = if (pos.legalMoves().any { squareName(it.from) == sq }) sq else null; return }
        selected = null
        if (cur == bestFrom && sq == bestTo) solved = true else tried = cur to sq
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Boka.surface).padding(12.dp)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            ChessBoard(
                fen = ex.fen,
                orientation = if (ex.color == "b") "black" else "white",
                selected = selected,
                lastMove = if (!solved) tried else null,
                hint = if (done && bestTo.isNotEmpty()) bestFrom to bestTo else null,
                targets = targets,
                enabled = !done,
                showCoordinates = false,
                onSquareTap = ::tap,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Move ${ex.moveNumber} · ${phaseName(ex.phase)}", color = Boka.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("−${ex.loss / 100}.${(ex.loss % 100) / 10}", color = Boka.danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        val bestLabel = ex.bestSan.ifEmpty { ex.best }
        val feedback = when {
            solved -> "That's it — $bestLabel was best."
            revealed -> "Best was $bestLabel."
            tried != null -> "Not it — try again."
            else -> "Find the stronger move."
        }
        Text("You played ${ex.played}. $feedback", color = when { solved -> Boka.success; tried != null && !done -> Boka.danger; else -> Boka.textMuted }, fontSize = 13.5.sp, lineHeight = 19.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ex.categoryLabel + (if (ex.opening.isNotEmpty()) " · ${ex.opening}" else ""), color = Boka.textMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        }
        if (!done) {
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Show answer", { revealed = true }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LockedCard(teaser: String, onUpsell: () -> Unit) {
    Card {
        Text("🔒 " + teaser.ifEmpty { "This section is part of Premium." }, color = Boka.text, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Go Premium", onUpsell, modifier = Modifier.fillMaxWidth())
    }
}

private fun phaseName(p: String) = when (p) { "opening" -> "Opening"; "middlegame" -> "Middlegame"; "endgame" -> "Endgame"; else -> p }

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Boka.surface).padding(14.dp)) { content() }
}

@Composable
private fun Label(text: String, color: Color) {
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, color = Boka.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun OpeningLine(row: OpeningRowDto, tone: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${row.name}  ${row.eco}", color = Boka.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("${row.games}g", color = Boka.textMuted, fontSize = 12.sp)
        Text("  ${row.winRate}%", color = tone, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Bullets(items: List<String>) {
    items.forEach { line ->
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Boka.surface).padding(14.dp)) {
            Text("• ", color = Boka.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(line, color = Boka.text, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, accent: Color = Boka.text) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Boka.surface).padding(vertical = 12.dp, horizontal = 14.dp)) {
        Text(label.uppercase(), color = Boka.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
