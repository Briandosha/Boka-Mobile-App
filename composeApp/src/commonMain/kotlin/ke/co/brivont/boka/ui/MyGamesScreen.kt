package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.ApiGame
import ke.co.brivont.boka.data.GameApi
import ke.co.brivont.boka.data.User
import ke.co.brivont.boka.ui.theme.Boka

private fun clean(name: String?): String =
    if (name.isNullOrBlank()) "Opponent" else if (name.contains('@')) name.substringBefore('@') else name

@Composable
fun MyGamesScreen(user: User, onAnalyze: (pgn: String, color: String) -> Unit) {
    var games by remember { mutableStateOf<List<ApiGame>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { games = GameApi.history() } catch (e: Throwable) { error = "Couldn't load your games." }
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "📜 My Games")
        when {
            error != null -> Text(error!!, color = Boka.textMuted, modifier = Modifier.padding(24.dp))
            games == null -> Loading()
            games!!.isEmpty() -> Text("No games yet — play a match and it'll show up here.", color = Boka.textMuted, modifier = Modifier.padding(24.dp))
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(games!!) { g -> GameRow(g, user, onAnalyze) }
            }
        }
    }
}

@Composable
private fun GameRow(g: ApiGame, user: User, onAnalyze: (String, String) -> Unit) {
    val isWhite = g.white_player_id == user.id
    val myColor = if (isWhite) "w" else "b"
    val opp = clean(if (isWhite) g.blackPlayer?.username else g.whitePlayer?.username)
    val result = when { g.winner_id == null -> "Draw"; g.winner_id == user.id -> "Win"; else -> "Loss" }
    val resultColor = when (result) { "Win" -> Boka.success; "Loss" -> Boka.danger; else -> Boka.textMuted }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
            .background(Boka.surface)
            .clickable(enabled = g.pgn != null) { g.pgn?.let { onAnalyze(it, myColor) } }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(result, color = resultColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
            textAlign = TextAlign.Center, modifier = Modifier.width(52.dp))
        Column(Modifier.weight(1f)) {
            Text("vs $opp", color = Boka.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text((if (isWhite) "♔ White" else "♚ Black") + (g.ended_at?.let { " · ${it.take(10)}" } ?: ""),
                color = Boka.textFaint, fontSize = 11.sp)
        }
        Text("Analyse ›", color = Boka.goldBright, fontSize = 12.sp)
    }
}
