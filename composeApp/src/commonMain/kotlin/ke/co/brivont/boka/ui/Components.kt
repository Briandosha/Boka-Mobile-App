package ke.co.brivont.boka.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.ui.theme.Boka
import ke.co.brivont.boka.ui.theme.BokaGradient
import ke.co.brivont.boka.ui.theme.BokaType

// Gates-style geometry: crisp, minimal radius.
private val ButtonShape = RoundedCornerShape(6.dp)

/** Primary CTA — the gold hero gradient with dark ink (the original Boka finish). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f)
    val ink = BokaGradient.onHero
    Box(
        modifier
            .height(52.dp)
            .scale(scale)
            .clip(ButtonShape)
            .then(
                if (enabled) Modifier.background(BokaGradient.hero)
                else Modifier.background(Boka.surfaceAlt)
            )
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (enabled) ink else Boka.textFaint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = if (enabled) ink else Boka.textFaint,
                fontFamily = BokaType.sans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** Bordered, quiet secondary action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f)
    Box(
        modifier
            .height(52.dp)
            .scale(scale)
            .clip(ButtonShape)
            .background(Boka.surface)
            .border(1.dp, Boka.borderStrong, ButtonShape)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Boka.text, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Boka.text, fontFamily = BokaType.sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** Circular, bordered back affordance. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f)
    Box(
        modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Boka.surface)
            .border(1.dp, Boka.borderStrong, CircleShape)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Boka.text, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Boka.text)
    }
}

/** App bar: status-bar inset, centered serif title, circular back, trailing slot. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Box(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp).height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) BackButton(onBack, Modifier.align(Alignment.CenterStart))
        Text(
            title, color = Boka.text,
            fontFamily = BokaType.serif, fontWeight = FontWeight.Bold, fontSize = 19.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(Modifier.align(Alignment.CenterEnd)) { trailing?.invoke() }
    }
}
