package ke.co.brivont.boka.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.core.provideStore

/**
 * Boka design system — "Boka Fresh" (2026-11): warm cream paper, fresh green
 * primary, sunny amber highlights, warm wood board. Light is the default; a
 * lifted green-charcoal dark theme is user-switchable and persisted. Same token
 * names as before, so every screen adapts unchanged.
 */
private data class Palette(
    val ground: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val bgInset: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,        // primary interactive — Brass
    val accentBright: Color,  // emphasis text — light brass
    val accentSoft: Color,    // tint fills
    val success: Color,       // Jade
    val warning: Color,
    val danger: Color,
    val lightSquare: Color,
    val darkSquare: Color,
)

// Default — "Boka Fresh": warm cream paper, fresh green primary, sunny amber
// highlights, warm wood board. Bright and friendly; the old navy+brass was dark.
private val LightPalette = Palette(
    ground = Color(0xFFFBF8F2),        // warm cream paper
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF6F2E9),
    bgInset = Color(0xFFF1ECE1),
    border = Color(0xFFE6E0D3),
    borderStrong = Color(0xFFCFC7B6),
    text = Color(0xFF1C2B2A),          // deep green-ink
    textMuted = Color(0xFF55666A),
    textFaint = Color(0xFF8A9A9D),
    accent = Color(0xFF2F9E6A),        // fresh green — primary
    accentBright = Color(0xFF1F7A50),  // emphasis text
    accentSoft = Color(0x1F2F9E6A),
    success = Color(0xFF1E9C84),       // teal-green — live/legal/OK (distinct from primary)
    warning = Color(0xFFE8A838),       // sunny amber — highlights, badges, streak flame
    danger = Color(0xFFD9534F),
    lightSquare = Color(0xFFEDEAD5),   // Boka Fresh board: cream + sage green (the brown wood
    darkSquare = Color(0xFF75A17B),    // board clashed with the cream/green/amber UI)
)

// Optional dark — lifted out of navy murk: deep green-charcoal, same green + amber.
private val DarkPalette = Palette(
    ground = Color(0xFF131C1B),
    surface = Color(0xFF1C2827),
    surfaceAlt = Color(0xFF243331),
    bgInset = Color(0xFF182322),
    border = Color(0xFF2B3A38),
    borderStrong = Color(0xFF3A4C49),
    text = Color(0xFFF2F5F2),
    textMuted = Color(0xFFA3B2AF),
    textFaint = Color(0xFF6F807D),
    accent = Color(0xFF3DBA7E),
    accentBright = Color(0xFF7FD9A8),
    accentSoft = Color(0x243DBA7E),
    success = Color(0xFF3FBFA6),
    warning = Color(0xFFF0B64C),
    danger = Color(0xFFF07A72),
    lightSquare = Color(0xFFEDEAD5),   // same board in dark mode (green-charcoal UI)
    darkSquare = Color(0xFF75A17B),
)

/** Theme tokens. Same property names as before so every screen adapts unchanged. */
object Boka {
    // Light ("Boka Fresh") is the default; dark stays available in Profile → Appearance.
    var isDark by mutableStateOf(
        runCatching { (provideStore().get("theme2") ?: "light") == "dark" }.getOrDefault(false)
    )
        private set

    fun setDarkTheme(dark: Boolean) {
        isDark = dark
        runCatching { provideStore().put("theme2", if (dark) "dark" else "light") }
    }

    private val p: Palette get() = if (isDark) DarkPalette else LightPalette

    val ground get() = p.ground
    val surface get() = p.surface
    val surfaceAlt get() = p.surfaceAlt
    val bgInset get() = p.bgInset
    val border get() = p.border
    val borderStrong get() = p.borderStrong
    val text get() = p.text
    val textMuted get() = p.textMuted
    val textFaint get() = p.textFaint

    // Legacy names kept: "gold" = the PRIMARY accent (now fresh green), "goldBright" = emphasis.
    // For the amber highlight colour use `warning` (or `amber`).
    val amber get() = p.warning
    val gold get() = p.accent
    val goldBright get() = p.accentBright
    val goldSoft get() = p.accentSoft

    val success get() = p.success
    val warning get() = p.warning
    val danger get() = p.danger

    val lightSquare get() = p.lightSquare
    val darkSquare get() = p.darkSquare
}

/** Brand font families, set by [BokaTheme]. Helvetica-Neue system sans. */
object BokaType {
    var serif: FontFamily = FontFamily.SansSerif   // display alias — kept so screens need no edits
    var sans: FontFamily = FontFamily.SansSerif
}

/** The green hero gradient — the Boka CTA finish. */
object BokaGradient {
    val hero: Brush
        get() = if (Boka.isDark) Brush.linearGradient(
            0.0f to Color(0xFF4CC98C), 1.0f to Color(0xFF2F9E6A),
        ) else Brush.linearGradient(
            0.0f to Color(0xFF3BB07A), 1.0f to Color(0xFF25865A),
        )
    /** White text on the green gradient in both themes. */
    val onHero: Color get() = Color(0xFFFFFFFF)
}

@Composable
fun BokaTheme(content: @Composable () -> Unit) {
    // Helvetica-Neue substitute: the platform system sans (Roboto on Android) is
    // the licence-free face the brand README specifies as the non-Apple fallback.
    val sans = FontFamily.SansSerif
    BokaType.serif = sans
    BokaType.sans = sans

    val typography = Typography(
        headlineLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp),
    )

    val scheme = if (Boka.isDark) darkColorScheme(
        primary = Boka.gold,
        onPrimary = Color(0xFFFFFFFF),
        secondary = Boka.goldBright,
        background = Boka.ground,
        onBackground = Boka.text,
        surface = Boka.surface,
        onSurface = Boka.text,
        surfaceVariant = Boka.surfaceAlt,
        onSurfaceVariant = Boka.textMuted,
        outline = Boka.border,
        error = Boka.danger,
    ) else lightColorScheme(
        primary = Boka.gold,
        onPrimary = Color(0xFFFFFFFF),
        secondary = Boka.goldBright,
        background = Boka.ground,
        onBackground = Boka.text,
        surface = Boka.surface,
        onSurface = Boka.text,
        surfaceVariant = Boka.surfaceAlt,
        onSurfaceVariant = Boka.textMuted,
        outline = Boka.border,
        error = Boka.danger,
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        content = content,
    )
}
