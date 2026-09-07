package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.chess.Position
import ke.co.brivont.boka.chess.squareName
import ke.co.brivont.boka.data.COACHING
import ke.co.brivont.boka.data.ENDGAMES
import ke.co.brivont.boka.data.MIDDLEGAME
import ke.co.brivont.boka.data.OPENINGS
import ke.co.brivont.boka.data.TACTICS
import ke.co.brivont.boka.ui.board.ChessBoard
import ke.co.brivont.boka.ui.theme.Boka
import ke.co.brivont.boka.ui.theme.BokaType

private val CardShape = RoundedCornerShape(10.dp)

/** One replayable branch of a study item. */
private data class StudyLine(val name: String, val moves: String, val note: String)

/** One studyable item — an opening (with variations) or an endgame technique. */
private data class StudyItem(
    val id: String,
    val name: String,
    val tag: String,          // "C50 · for White" | "Technique"
    val kind: String,         // "Opening" | "Endgame"
    val summary: String,
    val ideas: List<String>,
    val lines: List<StudyLine>,
    val startFen: String?,    // endgames start from a custom position
    val orientation: String,
)

private fun allStudyItems(): List<StudyItem> =
    COACHING.map { l ->
        StudyItem(
            id = "c_${l.id}", name = l.title, tag = "${l.group} · for ${l.forWhom}", kind = "Coaching",
            summary = l.summary, ideas = l.why,
            lines = listOf(
                StudyLine("✓ Best plan", l.bestLine, "Play instead of ${l.commonMove}: ${l.bestMove}"),
                StudyLine("✗ Common mistake", l.commonLine, "What most players do (${l.commonMove}) and why it disappoints."),
            ),
            startFen = null,
            orientation = if (l.forWhom == "Black") "black" else "white",
        )
    } + OPENINGS.map { o ->
        StudyItem(
            id = "o_${o.id}", name = o.name, tag = "${o.eco} · for ${o.forWhom}", kind = "Opening",
            summary = o.summary, ideas = o.ideas,
            lines = buildList {
                add(StudyLine("Main line", o.mainline, ""))
                o.variations.forEach { add(StudyLine(it.name, it.moves, it.note)) }
                o.sampleGames.forEach {
                    add(StudyLine("Famous: ${it.white} vs ${it.black}", it.moves, "${it.event}, ${it.year} · ${it.result}"))
                }
            },
            startFen = null,
            orientation = if (o.forWhom == "Black") "black" else "white",
        )
    } + MIDDLEGAME.map { m ->
        StudyItem(
            id = "m_${m.id}", name = m.name, tag = m.theme, kind = "Middlegame",
            summary = m.summary, ideas = m.ideas,
            lines = listOf(StudyLine("The line", m.line, "")),
            startFen = null, orientation = "white",
        )
    } + TACTICS.map { x ->
        StudyItem(
            id = "t_${x.id}", name = x.name, tag = x.motif, kind = "Tactic",
            summary = x.summary, ideas = x.ideas,
            lines = listOf(StudyLine("Solution", x.line, "")),
            startFen = x.fen, orientation = if (x.fen.split(" ").getOrNull(1) == "b") "black" else "white",
        )
    } + ENDGAMES.map { e ->
        StudyItem(
            id = "e_${e.id}", name = e.name, tag = "Technique", kind = "Endgame",
            summary = e.summary, ideas = e.ideas,
            lines = listOf(StudyLine("Demonstration", e.line, "")),
            startFen = e.fen, orientation = "white",
        )
    }

private class Line(
    val fens: List<String>,
    val lastMoves: List<Pair<String, String>?>,
    val sans: List<String>,
    val whiteFirst: Boolean,
)

/** Replay a SAN movetext (from [startFen] or the initial position) via the on-device engine. */
private fun buildLine(movetext: String, startFen: String?): Line {
    var pos = runCatching { startFen?.let { Position.fromFen(it) } ?: Position.start() }
        .getOrDefault(Position.start())
    val whiteFirst = pos.whiteToMove
    val tokens = movetext.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val fens = mutableListOf(pos.toFen())
    val lms = mutableListOf<Pair<String, String>?>(null)
    val sans = mutableListOf<String>()
    for (tok in tokens) {
        val want = tok.trimEnd('+', '#', '!', '?')
        val mv = pos.legalMoves().firstOrNull { pos.sanOf(it).trimEnd('+', '#') == want } ?: break
        val next = pos.makeMove(mv.from, mv.to, mv.promo) ?: break
        sans.add(pos.sanOf(mv))
        pos = next
        fens.add(pos.toFen())
        lms.add(squareName(mv.from) to squareName(mv.to))
    }
    return Line(fens, lms, sans, whiteFirst)
}

@Composable
fun OpeningsScreen() {
    val items = remember { allStudyItems() }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Coaching") }
    var coachPage by remember { mutableStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var lineIndex by remember { mutableStateOf(0) }
    var ply by remember { mutableStateOf(-1) }

    val selected = items.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(
            title = "📖 Learn",
            onBack = if (selected != null) ({ selectedId = null; ply = -1; lineIndex = 0 }) else null,
        )

        if (selected == null) {
            // ---------- Browse & search ----------
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(4.dp))
                SearchBox(query, { query = it })
                Spacer(Modifier.height(12.dp))

                val q = query.trim().lowercase()
                val list = if (q.isNotEmpty())
                    items.filter { it.name.lowercase().contains(q) || it.tag.lowercase().contains(q) || it.summary.lowercase().contains(q) }
                else items.filter { it.kind == category }

                if (q.isEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryChip("Coaching", category == "Coaching") { category = "Coaching"; coachPage = 0 }
                        CategoryChip("Openings", category == "Opening") { category = "Opening" }
                        CategoryChip("Middlegame", category == "Middlegame") { category = "Middlegame" }
                        CategoryChip("Tactics", category == "Tactic") { category = "Tactic" }
                        CategoryChip("Endgames", category == "Endgame") { category = "Endgame" }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (list.isEmpty()) {
                    Text("No matches for \"$query\".", color = Boka.textMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp))
                }
                // Coaching is paginated (it grows large); other categories scroll.
                val paged = category == "Coaching" && q.isEmpty()
                val pageSize = 6
                val pages = ((list.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                val page = coachPage.coerceIn(0, pages - 1)
                val shown = if (paged) list.drop(page * pageSize).take(pageSize) else list
                for (item in shown) {
                    StudyRow(item, showKind = q.isNotEmpty()) { selectedId = item.id; ply = -1; lineIndex = 0 }
                }
                if (paged && pages > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("‹ Prev", { if (page > 0) coachPage = page - 1 }, Modifier.weight(1f))
                        Text("Page ${page + 1} of $pages", color = Boka.textFaint, fontSize = 12.sp)
                        SecondaryButton("Next ›", { if (page < pages - 1) coachPage = page + 1 }, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        } else {
            // ---------- Study detail with board replay + line picker ----------
            val safeLineIndex = lineIndex.coerceIn(0, selected.lines.lastIndex)
            val activeLine = selected.lines[safeLineIndex]
            val line = remember(selected.id, safeLineIndex) { buildLine(activeLine.moves, selected.startFen) }
            val maxPly = line.sans.size - 1
            val safePly = ply.coerceIn(-1, maxPly)
            val fen = line.fens[safePly + 1]
            val lastMove = line.lastMoves[safePly + 1]

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(selected.name, color = Boka.text, fontFamily = BokaType.serif,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("${selected.kind} · ${selected.tag}", color = Boka.textFaint, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))

                // Line picker: main line, "if opponent plays…" variations, famous games.
                if (selected.lines.size > 1) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selected.lines.forEachIndexed { i, l ->
                            CategoryChip(l.name, i == safeLineIndex) { lineIndex = i; ply = -1 }
                        }
                    }
                    if (activeLine.note.isNotBlank()) {
                        Text(activeLine.note, color = Boka.textMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ChessBoard(fen = fen, orientation = selected.orientation, lastMove = lastMove, enabled = false)

                Spacer(Modifier.height(8.dp))
                val moveLabel = if (safePly < 0) "Starting position" else {
                    val isWhiteMove = if (line.whiteFirst) safePly % 2 == 0 else safePly % 2 == 1
                    "${safePly / 2 + 1}${if (isWhiteMove) "." else "..."} ${line.sans[safePly]}"
                }
                Text(moveLabel, color = Boka.goldBright, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Move ${safePly + 1} of ${maxPly + 1}", color = Boka.textFaint, fontSize = 11.sp)

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("⏮", { ply = -1 }, Modifier.weight(1f))
                    SecondaryButton("‹", { ply = (safePly - 1).coerceAtLeast(-1) }, Modifier.weight(1f))
                    SecondaryButton("›", { ply = (safePly + 1).coerceAtMost(maxPly) }, Modifier.weight(1f))
                    SecondaryButton("⏭", { ply = maxPly }, Modifier.weight(1f))
                }

                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().clip(CardShape).background(Boka.surface)
                    .border(1.dp, Boka.border, CardShape).padding(16.dp)) {
                    Text("KEY IDEAS", color = Boka.goldBright, fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(selected.summary, color = Boka.text, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    for (idea in selected.ideas) {
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("•  ", color = Boka.goldBright, fontSize = 13.sp)
                            Text(idea, color = Boka.textMuted, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(Boka.surface)
            .border(1.dp, Boka.border, CardShape).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, "Search", tint = Boka.textFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search coaching, openings, tactics…", color = Boka.textFaint, fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Boka.text, fontSize = 14.sp, fontFamily = BokaType.sans),
                cursorBrush = SolidColor(Boka.goldBright),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Text("Clear", color = Boka.goldBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onChange("") })
        }
    }
}

@Composable
private fun CategoryChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (active) Boka.gold else Boka.surface)
            .border(1.dp, if (active) Boka.gold else Boka.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (active) Boka.surface else Boka.textMuted,
            fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun StudyRow(item: StudyItem, showKind: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(CardShape)
            .background(Boka.surface).border(1.dp, Boka.border, CardShape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, color = Boka.text, fontFamily = BokaType.serif,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(item.summary, color = Boka.textFaint, fontSize = 11.5.sp, maxLines = 2)
            if (item.lines.size > 1) {
                Text("${item.lines.size} lines to study", color = Boka.goldBright,
                    fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.size(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (showKind) {
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp)).background(Boka.goldSoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(item.kind.uppercase(), color = Boka.goldBright, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(item.tag, color = Boka.textFaint, fontSize = 10.sp)
        }
    }
}
