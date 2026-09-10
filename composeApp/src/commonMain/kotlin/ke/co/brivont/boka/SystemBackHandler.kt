package ke.co.brivont.boka

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform "back" gesture (Android hardware / gesture back).
 *
 * When [enabled] is true, [onBack] runs instead of the system default — which for
 * Android would finish the activity and drop the user out of the app. We enable it
 * everywhere except the root of the in-app hierarchy (the Home tab with nothing
 * open) so that back walks our screen stack and only exits from Home.
 *
 * No-op on iOS, which has no hardware back button (in-app back controls handle it).
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
