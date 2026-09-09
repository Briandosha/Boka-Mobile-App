package ke.co.brivont.boka

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/**
 * In-app video: Media3 ExoPlayer in a PlayerView. The player's built-in
 * fullscreen button opens a full-screen [Dialog] that auto-rotates to landscape
 * (sensor) and hides the system bars; tapping it again (or Back) returns inline.
 * One ExoPlayer is shared between the inline and fullscreen surfaces, so playback
 * position is preserved across the switch.
 */
@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val exo = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true
        }
    }
    var fullscreen by remember { mutableStateOf(false) }
    DisposableEffect(exo) { onDispose { exo.release() } }

    // Landscape + immersive while fullscreen; restore orientation and bars on exit.
    DisposableEffect(fullscreen) {
        val window = activity?.window
        if (fullscreen && activity != null && window != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Inline surface (hands the player to the fullscreen dialog while active).
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                setShowNextButton(false); setShowPreviousButton(false)
                setFullscreenButtonClickListener { fs -> fullscreen = fs }
            }
        },
        update = { it.player = if (fullscreen) null else exo },
        modifier = modifier,
    )

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        setShowNextButton(false); setShowPreviousButton(false)
                        setFullscreenButtonState(true)   // show the "collapse" icon
                        setFullscreenButtonClickListener { fs -> fullscreen = fs }
                    }
                },
                update = { it.player = exo },
                modifier = Modifier.fillMaxSize().background(Color.Black),
            )
        }
    }
}
