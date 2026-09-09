package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.call.body
import io.ktor.client.request.get
import ke.co.brivont.boka.VideoPlayer
import ke.co.brivont.boka.core.httpClient
import ke.co.brivont.boka.ui.theme.Boka
import kotlinx.serialization.Serializable

/**
 * Lesson Videos — narrated walk-throughs of the Coaching lessons and Tactics,
 * rendered offline from the app's own Learn data and hosted at /boka-media/.
 * Driven by manifest.json so new videos appear without an app update.
 * Videos play IN-APP (Media3 ExoPlayer on Android via [VideoPlayer]).
 *
 * Media URLs carry `?v=<manifest.generated>` so a re-render (new audio, same
 * file name) is never served stale from the CDN/browser cache.
 */
private const val MEDIA_BASE = "https://brivont.co.ke/boka-media/"

@Serializable
private data class MediaVideo(
    val id: String,
    val category: String = "",
    val title: String = "",
    val tag: String = "",
    val summary: String = "",
    val file: String = "",
    val poster: String = "",
    val duration: Double = 0.0,
)

@Serializable
private data class MediaManifest(val generated: String = "", val voice: String = "", val videos: List<MediaVideo> = emptyList())

private fun fmt(s: Double): String {
    val total = s.toInt(); return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable
fun MediaScreen(onBack: () -> Unit) {
    var videos by remember { mutableStateOf<List<MediaVideo>>(emptyList()) }
    var version by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") }
    var playing by remember { mutableStateOf<MediaVideo?>(null) }

    LaunchedEffect(Unit) {
        try {
            // Cache-bust the manifest itself so a fresh render is picked up immediately.
            val m = httpClient.get(MEDIA_BASE + "manifest.json?cb=" + kotlin.random.Random.nextInt(1_000_000)).body<MediaManifest>()
            videos = m.videos; version = m.generated
        } catch (e: Throwable) {
            error = "Couldn’t load the video list — please try again later."
        } finally { loading = false }
    }
    val shown = videos.filter { filter == "all" || it.category == filter }
    val current = playing
    fun mediaUrl(file: String) = MEDIA_BASE + file + (if (version.isNotEmpty()) "?v=$version" else "")

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        // Back from a playing video returns to the list first; from the list, leaves the screen.
        TopBar(title = if (current != null) "🎬 Now playing" else "🎬 Lesson Videos",
            onBack = { if (current != null) playing = null else onBack() })

        if (current != null) {
            // ---------- In-app player ----------
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                VideoPlayer(
                    url = mediaUrl(current.file),
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Boka.text.copy(alpha = 0.06f)),
                )
                Column(Modifier.padding(16.dp)) {
                    Text("${current.category.uppercase()} · ${current.tag} · ${fmt(current.duration)}",
                        color = Boka.textFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(current.title, color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.padding(top = 4.dp))
                    Text(current.summary, color = Boka.textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(16.dp))
                    SecondaryButton("‹ All videos", { playing = null }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                }
            }
            return@Column
        }

        // ---------- List ----------
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Narrated walk-throughs of the Coaching lessons and tactical patterns.",
                color = Boka.textMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip("All (${videos.size})", filter == "all") { filter = "all" }
                CategoryChip("Coaching", filter == "coaching") { filter = "coaching" }
                CategoryChip("Tactics", filter == "tactics") { filter = "tactics" }
            }
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Text("Loading videos…", color = Boka.textFaint, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                error != null -> Text(error!!, color = Boka.danger, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                shown.isEmpty() -> Text("No videos yet — check back soon.", color = Boka.textFaint, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { v ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Boka.surface)
                                .clickable { playing = v }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(Boka.gold), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PlayArrow, "Play", tint = Boka.ground)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${v.category.uppercase()} · ${v.tag}", color = Boka.textFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(v.title, color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(v.summary, color = Boka.textMuted, fontSize = 12.sp, maxLines = 2)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(fmt(v.duration), color = Boka.goldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
