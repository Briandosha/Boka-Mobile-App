package ke.co.brivont.boka.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.core.Session
import ke.co.brivont.boka.core.accessTokenExpired
import ke.co.brivont.boka.data.AuthApi
import ke.co.brivont.boka.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ke.co.brivont.boka.ui.theme.Boka
import ke.co.brivont.boka.ui.theme.BokaGradient
import ke.co.brivont.boka.ui.theme.BokaType

private val CardShape = RoundedCornerShape(10.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: User,
    onPlay: () -> Unit,
    onCoach: () -> Unit,
    onWatch: () -> Unit,
    onLearn: () -> Unit,
    onRanks: () -> Unit,
    onGames: () -> Unit,
    onMedia: () -> Unit,
    onPuzzles: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BokaMark(26.dp)
            Spacer(Modifier.size(8.dp))
            Text("BOKA", color = Boka.text, fontFamily = BokaType.serif,
                fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp)
        }

        val scope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    // Re-check the session: renew a stale token, or — if the refresh
                    // token is dead — AuthApi.refresh() force-expires it and the root
                    // composable drops us back to sign-in.
                    runCatching { if (Session.isLoggedIn && accessTokenExpired()) AuthApi.refresh() }
                    delay(600)
                    refreshing = false
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("Welcome back,", color = Boka.textMuted, fontFamily = BokaType.sans, fontSize = 15.sp)
            Text(
                user.name,
                color = Boka.text, fontFamily = BokaType.serif,
                fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp,
            )
            Spacer(Modifier.height(24.dp))

            // Featured tile — full-width, ink.
            FeaturedTile(
                title = "Play Online",
                subtitle = "Get matched with a live opponent",
                icon = Icons.Filled.PlayArrow,
                onClick = onPlay,
            )

            Spacer(Modifier.height(12.dp))

            // Puzzles — the daily-habit engine (free).
            WideTile(
                title = "Puzzles",
                subtitle = "Daily tactics · rating & streak",
                icon = Icons.Filled.Extension,
                onClick = onPuzzles,
            )
            Spacer(Modifier.height(12.dp))

            // Editorial 2×2 grid.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GridTile("Coach", "Play Stockfish,\nget coached", Icons.Filled.SmartToy, Modifier.weight(1f), onCoach)
                GridTile("Watch Live", "Spectate games\nin progress", Icons.Filled.Visibility, Modifier.weight(1f), onWatch)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GridTile("Learn", "Openings &\nendgame theory", Icons.Filled.MenuBook, Modifier.weight(1f), onLearn)
                GridTile("Ranks", "Top players\n+ your rating", Icons.Filled.EmojiEvents, Modifier.weight(1f), onRanks)
            }

            Spacer(Modifier.height(12.dp))

            // Wide tile — narrated lesson videos.
            WideTile(
                title = "Lesson Videos",
                subtitle = "Narrated Coaching & Tactics walk-throughs",
                icon = Icons.Filled.OndemandVideo,
                onClick = onMedia,
            )
            Spacer(Modifier.height(12.dp))

            // Wide tile — game history.
            WideTile(
                title = "My Games",
                subtitle = "Replay & analyse your finished games",
                icon = Icons.Filled.History,
                onClick = onGames,
            )
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun FeaturedTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f)
    val ink = BokaGradient.onHero
    Column(
        Modifier.fillMaxWidth().scale(scale)
            .clip(CardShape)
            .background(BokaGradient.hero)   // gold hero gradient
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(ink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = ink.copy(alpha = 0.75f))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = ink, fontFamily = BokaType.serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = ink.copy(alpha = 0.8f), fontFamily = BokaType.sans, fontSize = 13.sp)
    }
}

@Composable
private fun GridTile(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f)
    Column(
        modifier.aspectRatio(1.08f).scale(scale)
            .clip(CardShape)
            .background(Boka.surface)
            .border(1.dp, Boka.border, CardShape)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Boka.goldSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Boka.text, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = Boka.text, fontFamily = BokaType.serif, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Boka.textMuted, fontFamily = BokaType.sans, fontSize = 11.5.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun WideTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f)
    Row(
        Modifier.fillMaxWidth().scale(scale)
            .clip(CardShape)
            .background(Boka.surface)
            .border(1.dp, Boka.border, CardShape)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Boka.goldSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Boka.text, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Boka.text, fontFamily = BokaType.serif, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = Boka.textMuted, fontFamily = BokaType.sans, fontSize = 12.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = Boka.textFaint)
    }
}
