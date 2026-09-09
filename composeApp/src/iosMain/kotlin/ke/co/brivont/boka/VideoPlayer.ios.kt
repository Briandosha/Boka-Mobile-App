package ke.co.brivont.boka

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler

/** iOS: native AVPlayer embedding is not wired yet — hand the MP4 to the system. */
@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    Box(modifier, contentAlignment = Alignment.Center) {
        TextButton(onClick = { uriHandler.openUri(url) }) { Text("Open video") }
    }
}
