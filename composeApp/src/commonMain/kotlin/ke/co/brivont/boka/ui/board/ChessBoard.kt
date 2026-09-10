package ke.co.brivont.boka.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.chess.Position
import ke.co.brivont.boka.chess.squareName as engineSquareName
import ke.co.brivont.boka.ui.theme.Boka

const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

/** pieces[r][f]: r=0 is rank 8 (top), f=0 is file a. Space = empty. */
private fun parseFen(fen: String): Array<CharArray> {
    val board = Array(8) { CharArray(8) { ' ' } }
    val placement = fen.substringBefore(' ')
    var r = 0
    for (rankStr in placement.split('/')) {
        if (r > 7) break
        var f = 0
        for (c in rankStr) {
            if (c.isDigit()) f += c - '0'
            else { if (f < 8) board[r][f] = c; f++ }
        }
        r++
    }
    return board
}

private fun squareName(r: Int, f: Int): String = "${('a' + f)}${8 - r}"

/** Rendered (row, col) of a square name, honouring board orientation. Row 0 = top. */
private fun renderRC(sq: String, orientation: String): Pair<Int, Int> {
    val file = sq[0] - 'a'
    val rank = sq[1] - '1'
    val r = 7 - rank
    val rr = if (orientation == "black") 7 - r else r
    val rc = if (orientation == "black") 7 - file else file
    return rr to rc
}

/** Square of the side-to-move's king if it's in check (for the red highlight), else null. */
private fun checkSquareOf(fen: String): String? = try {
    val pos = Position.fromFen(fen)
    if (pos.inCheck()) {
        val ks = pos.kingSquare(pos.whiteToMove)
        if (ks >= 0) engineSquareName(ks) else null
    } else null
} catch (e: Exception) { null }

private data class Slide(val toSq: String, val piece: Char, val dx: Float, val dy: Float)

/**
 * Chess board drawn with TWO canvases for jank-free animation:
 *  - a static canvas (squares, tints, dots, coordinates, resting pieces) that
 *    redraws only when the position/selection changes, and
 *  - a tiny overlay canvas that redraws per animation frame but paints ONLY the
 *    sliding piece (the animation progress is read in the DRAW phase, so no
 *    recomposition or layout happens per frame).
 * Taps are handled by a single pointerInput (no per-square ripples).
 */
@Composable
fun ChessBoard(
    fen: String,
    orientation: String = "white",   // "white" | "black"
    selected: String? = null,
    lastMove: Pair<String, String>? = null,
    targets: Set<String> = emptySet(),
    hint: Pair<String, String>? = null,   // best-move from/to (Coach hint)
    enabled: Boolean = true,
    showCoordinates: Boolean = true,
    onSquareTap: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val board = remember(fen) { parseFen(fen) }
    val checkSquare = remember(fen) { checkSquareOf(fen) }

    // Theme tints (read in composition; recolor on theme change). Highlights are
    // brass selection/last-move, jade legal-move target (brand palette).
    val lightSq = Boka.lightSquare
    val darkSq = Boka.darkSquare
    val selectedTint = Color(0x82C8A96B)  // brass
    val lastMoveTint = Color(0x45C8A96B)  // brass
    val targetTint = Color(0x8C4FB6A5)     // jade — legal move
    val checkTint = Boka.danger.copy(alpha = 0.55f)
    val hintTint = Color(0xAA22C55E)       // vivid green — best-move hint

    // ---- Slide animation state ----
    val prog = remember { Animatable(1f) }
    var slides by remember { mutableStateOf<List<Slide>>(emptyList()) }

    val measurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(darkSq)
            .border(1.dp, Boka.borderStrong, RoundedCornerShape(8.dp))
            .pointerInput(enabled, orientation) {
                if (enabled) detectTapGestures { off ->
                    val cell = size.width / 8f
                    val rc = (off.x / cell).toInt().coerceIn(0, 7)
                    val rr = (off.y / cell).toInt().coerceIn(0, 7)
                    val file = if (orientation == "black") 7 - rc else rc
                    val rowTop = if (orientation == "black") 7 - rr else rr
                    onSquareTap(squareName(rowTop, file))
                }
            },
    ) {
        val cellDp = maxWidth / 8
        val cellPx = with(LocalDensity.current) { cellDp.toPx() }
        LaunchedEffect(fen, orientation) {
            val lm = lastMove
            if (lm != null) {
                val (fr, fc) = renderRC(lm.first, orientation)
                val (tr, tc) = renderRC(lm.second, orientation)
                val toR = 7 - (lm.second[1] - '1')
                val toF = lm.second[0] - 'a'
                val piece = board[toR][toF]
                if (piece != ' ') {
                    val list = mutableListOf(Slide(lm.second, piece, (fc - tc).toFloat(), (fr - tr).toFloat()))
                    // Castling moves the rook too — slide it in tandem so it doesn't teleport.
                    if ((piece == 'K' || piece == 'k') &&
                        kotlin.math.abs((lm.second[0] - 'a') - (lm.first[0] - 'a')) == 2
                    ) {
                        val rankCh = lm.second[1]
                        val kingside = lm.second[0] == 'g'
                        val rookFrom = "${if (kingside) 'h' else 'a'}$rankCh"
                        val rookTo = "${if (kingside) 'f' else 'd'}$rankCh"
                        val (rfr, rfc) = renderRC(rookFrom, orientation)
                        val (rtr, rtc) = renderRC(rookTo, orientation)
                        val rookPiece = board[7 - (rookTo[1] - '1')][rookTo[0] - 'a']
                        if (rookPiece != ' ') list.add(Slide(rookTo, rookPiece, (rfc - rtc).toFloat(), (rfr - rtr).toFloat()))
                    }
                    slides = list
                    prog.snapTo(0f)
                    prog.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                    slides = emptyList()
                }
            }
        }

        // ---- Static layer: redraws only when position/selection/theme change ----
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / 8f
            val hintSq = hint
            val coordStyle = TextStyle(fontSize = (cell * 0.16f).toSp(), fontWeight = FontWeight.Bold)
            for (rr in 0..7) {
                for (rc in 0..7) {
                    val file = if (orientation == "black") 7 - rc else rc
                    val rowTop = if (orientation == "black") 7 - rr else rr
                    val sq = squareName(rowTop, file)
                    val isLight = (rowTop + file) % 2 == 0
                    val x = rc * cell
                    val y = rr * cell
                    drawRect(if (isLight) lightSq else darkSq, Offset(x, y), Size(cell, cell))

                    val p = board[rowTop][file]
                    val overlay = when {
                        sq == checkSquare && (p == 'K' || p == 'k') -> checkTint
                        hintSq != null && (sq == hintSq.first || sq == hintSq.second) -> hintTint
                        sq == selected -> selectedTint
                        lastMove != null && (sq == lastMove.first || sq == lastMove.second) -> lastMoveTint
                        else -> null
                    }
                    if (overlay != null) drawRect(overlay, Offset(x, y), Size(cell, cell))

                    if (sq in targets) {
                        if (p == ' ') drawCircle(targetTint, cell * 0.15f, Offset(x + cell / 2, y + cell / 2))
                        else drawCircle(targetTint, cell * 0.43f, Offset(x + cell / 2, y + cell / 2),
                            style = Stroke(width = cell * 0.07f))
                    }

                    // Pieces as cached raster blits (the sliding one is painted
                    // by the overlay layer instead).
                    if (p != ' ' && slides.none { it.toSq == sq }) {
                        drawImage(pieceImage(p, cell.toInt().coerceAtLeast(1)), topLeft = Offset(x, y))
                    }

                    if (showCoordinates) {
                        val ink = if (isLight) darkSq else lightSq
                        if (rc == 0) drawText(measurer, "${8 - rowTop}",
                            topLeft = Offset(x + 3f, y + 1f), style = coordStyle.copy(color = ink))
                        if (rr == 7) drawText(measurer, "${'a' + file}",
                            topLeft = Offset(x + cell - cell * 0.18f, y + cell - cell * 0.24f),
                            style = coordStyle.copy(color = ink))
                    }
                }
            }
        }

        // ---- Overlay: the moving piece is drawn ONCE into a cell-sized layer,
        //      which is then only TRANSLATED per frame (graphicsLayer lambda =
        //      layer phase). Like the web board's CSS-transform animation: no
        //      redraw, no layout — the compositor just moves a small texture. ----
        slides.forEach { s ->
            val (tr, tc) = renderRC(s.toSq, orientation)
            Box(
                Modifier
                    .size(cellDp)
                    .graphicsLayer {
                        val t = prog.value
                        translationX = (tc + s.dx * (1f - t)) * cellPx
                        translationY = (tr + s.dy * (1f - t)) * cellPx
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawImage(pieceImage(s.piece, size.minDimension.toInt().coerceAtLeast(1)))
                }
            }
        }
    }
}
