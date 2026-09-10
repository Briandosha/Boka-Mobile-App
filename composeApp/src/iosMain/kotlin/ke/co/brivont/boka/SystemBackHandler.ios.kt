package ke.co.brivont.boka

import androidx.compose.runtime.Composable

/** iOS has no hardware back button; in-app back controls handle navigation. */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
