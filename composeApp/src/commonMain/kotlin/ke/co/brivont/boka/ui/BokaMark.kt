package ke.co.brivont.boka.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ke.co.brivont.boka.ui.theme.Boka

/**
 * The EXACT Boka brand mark from the official logo package (boka-mark.svg):
 * a full ring (ivory #F5F2EA on dark, ink #0F172A on light) with two brass
 * #C8A96B pie-wedges laid over the upper-left and lower-right quadrants.
 * Authored in the package's 120x120 space (ring r=50, stroke-width=21) and
 * scaled to the target size. Do not redraw or approximate — this is the
 * shipped design; only the ring colour adapts to the theme (the wedges stay
 * brass in both, per the on-dark / on-light package variants).
 */
private const val WEDGE_D = "M 120.5 60 A 60.5 60.5 0 0 1 60 120.5 L 60 99.5 A 39.5 39.5 0 0 0 99.5 60 Z"

@Composable
fun BokaMark(size: Dp, modifier: Modifier = Modifier) {
    val ring = if (Boka.isDark) Color(0xFFF5F2EA) else Color(0xFF0F172A)
    val brass = Color(0xFFC8A96B)

    // Lower-right wedge (as authored) and the upper-left wedge (same path
    // rotated 180° about the centre 60,60). Parsed once.
    val wedgeLR = remember { PathParser().parsePathString(WEDGE_D).toPath() }
    val wedgeUL = remember {
        val p = PathParser().parsePathString(WEDGE_D).toPath()
        val m = Matrix().apply { translate(60f, 60f); rotateZ(180f); translate(-60f, -60f) }
        p.transform(m)
        p
    }

    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 120f
        scale(s, s, pivot = Offset.Zero) {
            drawCircle(ring, radius = 50f, center = Offset(60f, 60f), style = Stroke(width = 21f))
            drawPath(wedgeLR, brass)
            drawPath(wedgeUL, brass)
        }
    }
}
