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
 * Boka design system — slate-navy ink + brass, from the official brand package:
 * Ink #0F172A canvas, Slate #1B2740 cards, Steel #334765 borders, Ivory #F5F2EA
 * text, Brass #C8A96B primary accent, Jade #4FB6A5 functional (legal/live/OK).
 * An ivory-paper light variant mirrors the same hues; user-switchable, persisted.
 * Type: the Helvetica-Neue system sans (Android's system face is the licence-free
 * substitute the brand README specifies — Helvetica Neue itself ships only on
 * Apple platforms).
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

// Default — the Boka identity: slate-navy ink with brass.
private val DarkPalette = Palette(
    ground = Color(0xFF0F172A),        // Ink
    surface = Color(0xFF1B2740),       // Slate
    surfaceAlt = Color(0xFF243352),
    bgInset = Color(0xFF16213C),
    border = Color(0xFF2C3A57),
    borderStrong = Color(0xFF334765),  // Steel
    text = Color(0xFFF5F2EA),          // Ivory
    textMuted = Color(0xFFA7B2C8),
    textFaint = Color(0xFF68779A),
    accent = Color(0xFFC8A96B),        // Brass
    accentBright = Color(0xFFD9C391),
    accentSoft = Color(0x1FC8A96B),
    success = Color(0xFF4FB6A5),       // Jade
    warning = Color(0xFFE2B458),
    danger = Color(0xFFF0635E),
    lightSquare = Color(0xFFEDE6D4),   // ivory board squares
    darkSquare = Color(0xFF3D4F76),    // steel-navy board squares
)

// Light variant — ivory paper with the same brass identity.
private val LightPalette = Palette(
    ground = Color(0xFFF5F2EA),        // Ivory paper
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF3EFE6),
    bgInset = Color(0xFFECE7DB),
    border = Color(0xFFE2DDD0),
    borderStrong = Color(0xFFCFC8B6),
    text = Color(0xFF0F172A),          // Ink
    textMuted = Color(0xFF4C5A74),
    textFaint = Color(0xFF8391AB),
    accent = Color(0xFF334765),        // Steel — light-mode primary (brass reserved for dark)
    accentBright = Color(0xFF26324B),
    accentSoft = Color(0x22334765),
    success = Color(0xFF2E8A7B),
    warning = Color(0xFFA06B1E),
    danger = Color(0xFFC2453F),
    lightSquare = Color(0xFFE6EBF2),   // cool steel-blue board — matches the light-mode
    darkSquare = Color(0xFF7C8FB0),    // Steel accent (dark UI keeps the navy board)
)

/** Theme tokens. Same property names as before so every screen adapts unchanged. */
object Boka {
    // Dark (ink+brass) is the default — the Boka identity.
    var isDark by mutableStateOf(
        runCatching { (provideStore().get("theme2") ?: "dark") == "dark" }.getOrDefault(true)
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

    // Legacy names kept: "gold" = the brass primary, "goldBright" = light-brass emphasis.
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

/** The brass hero gradient — the Boka CTA finish. */
object BokaGradient {
    // Brass hero in dark (the loved look); steel-blue hero in light so it matches
    // the light-mode Steel accent instead of clashing brass-on-ivory.
    val hero: Brush
        get() = if (Boka.isDark) Brush.linearGradient(
            0.0f to Color(0xFFD8BC82), 1.0f to Color(0xFFA8894E),
        ) else Brush.linearGradient(
            0.0f to Color(0xFF3E547A), 1.0f to Color(0xFF2B3D5F),
        )
    /** Ink on the brass gradient (dark); white on the steel gradient (light). */
    val onHero: Color get() = if (Boka.isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
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
        onPrimary = Color(0xFF0F172A),     // ink text on brass
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
