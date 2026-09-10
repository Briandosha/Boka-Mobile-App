package ke.co.brivont.boka

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Backed by activity-compose's [BackHandler], which registers against the host
 * activity's OnBackPressedDispatcher (MainActivity is a ComponentActivity). When
 * disabled it lets the press fall through to the system default (exit the app).
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
