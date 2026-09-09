package ke.co.brivont.boka

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * In-app video player for the Lesson Videos screen.
 * Android: Media3 ExoPlayer inside a PlayerView (see VideoPlayer.android.kt).
 * iOS: not wired yet — falls back to opening the URL (see VideoPlayer.ios.kt).
 */
@Composable
expect fun VideoPlayer(url: String, modifier: Modifier)
