package ke.co.brivont.boka

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.core.Session
import ke.co.brivont.boka.core.accessTokenExpired
import ke.co.brivont.boka.core.tokenUser
import ke.co.brivont.boka.data.AuthApi
import ke.co.brivont.boka.data.User
import ke.co.brivont.boka.ui.AnalysisScreen
import ke.co.brivont.boka.ui.AuthScreen
import ke.co.brivont.boka.ui.CoachScreen
import ke.co.brivont.boka.ui.GameScreen
import ke.co.brivont.boka.ui.HomeScreen
import ke.co.brivont.boka.ui.LeaderboardScreen
import ke.co.brivont.boka.ui.MediaScreen
import ke.co.brivont.boka.ui.MyGamesScreen
import ke.co.brivont.boka.ui.OpeningsScreen
import ke.co.brivont.boka.ui.ProfileScreen
import ke.co.brivont.boka.ui.SpectateScreen
import ke.co.brivont.boka.ui.theme.Boka
import ke.co.brivont.boka.ui.theme.BokaTheme

/** Bottom-navigation destinations. */
enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Games("Games", Icons.Filled.History),
    Learn("Learn", Icons.Filled.MenuBook),
    Ranks("Ranks", Icons.Filled.EmojiEvents),
    Profile("Profile", Icons.Filled.Person),
}

/** Full-screen flows shown over the tab shell (they own a back button, no bottom nav). */
private sealed interface Route {
    data object Play : Route
    data object Coach : Route
    data object Spectate : Route
    data object Media : Route
    data class Analysis(val pgn: String, val color: String) : Route
}

@Composable
fun App() {
    BokaTheme {
        Surface(color = Boka.ground) {
            var user by remember { mutableStateOf(if (Session.isLoggedIn) tokenUser() ?: User(name = "Player") else null) }
            var tab by remember { mutableStateOf(Tab.Home) }
            var route by remember { mutableStateOf<Route?>(null) }

            // Return to sign-in the instant the session is force-expired (a refresh
            // was definitively rejected). Also proactively renew a stale token on
            // launch so a long-idle session reconnects instead of silently failing.
            LaunchedEffect(Unit) {
                if (Session.isLoggedIn && accessTokenExpired()) AuthApi.refresh()
                Session.sessionExpired.collect {
                    user = null; tab = Tab.Home; route = null
                }
            }

            // Phone back button should walk the in-app hierarchy, not drop out of the
            // app: close a full-screen flow first, then fall back to the Home tab, and
            // only let the system exit when we're already Home with nothing open (or on
            // the sign-in screen, where there's nowhere to go back to).
            SystemBackHandler(enabled = user != null && (route != null || tab != Tab.Home)) {
                when {
                    route != null -> route = null
                    tab != Tab.Home -> tab = Tab.Home
                }
            }

            val u = user
            when {
                u == null -> AuthScreen(onAuthed = { user = it; tab = Tab.Home; route = null })

                route != null -> when (val r = route) {
                    Route.Play -> GameScreen(onBack = { route = null })
                    Route.Coach -> CoachScreen(onBack = { route = null })
                    Route.Spectate -> SpectateScreen(onBack = { route = null })
                    Route.Media -> MediaScreen(onBack = { route = null })
                    is Route.Analysis -> AnalysisScreen(
                        pgn = r.pgn, playerColor = r.color,
                        onBack = { route = null },
                        onUpsell = { route = null }, // subscription flow is web-based for now
                    )
                    null -> {}
                }

                else -> Column(Modifier.fillMaxSize().background(Boka.ground)) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (tab) {
                            Tab.Home -> HomeScreen(
                                user = u,
                                onPlay = { route = Route.Play },
                                onCoach = { route = Route.Coach },
                                onWatch = { route = Route.Spectate },
                                onLearn = { tab = Tab.Learn },
                                onRanks = { tab = Tab.Ranks },
                                onGames = { tab = Tab.Games },
                                onMedia = { route = Route.Media },
                            )
                            Tab.Games -> MyGamesScreen(
                                user = u,
                                onAnalyze = { pgn, color -> route = Route.Analysis(pgn, color) },
                            )
                            Tab.Learn -> OpeningsScreen()
                            Tab.Ranks -> LeaderboardScreen()
                            Tab.Profile -> ProfileScreen(user = u, onLogout = { Session.clear(); user = null })
                        }
                    }
                    BottomBar(current = tab, onSelect = { tab = it })
                }
            }
        }
    }
}

/**
 * Slim floating navigation pill in the Gates palette: the bar is the INK color
 * (weathered slate on parchment, parchment on slate — like the site's black
 * nav/footer on parchment). The active tab is an inverse chip with an inline
 * label; inactive tabs are icon-only, which keeps the bar thin.
 */
@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 12.dp, top = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .shadow(14.dp, RoundedCornerShape(30.dp), clip = false, spotColor = Color(0x66313A44))
                .clip(RoundedCornerShape(30.dp))
                .background(Boka.text)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (t in Tab.entries) BottomItem(t, t == current) { onSelect(t) }
        }
    }
}

@Composable
private fun BottomItem(tab: Tab, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) Boka.ground else Color.Transparent)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (active) 14.dp else 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            tab.icon, tab.label,
            tint = if (active) Boka.text else Boka.ground.copy(alpha = 0.65f),
            modifier = Modifier.height(20.dp),
        )
        if (active) {
            Spacer(Modifier.width(6.dp))
            Text(
                tab.label,
                color = Boka.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
