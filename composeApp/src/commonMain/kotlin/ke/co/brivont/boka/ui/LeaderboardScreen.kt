package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.GameApi
import ke.co.brivont.boka.data.LeaderRow
import ke.co.brivont.boka.data.LeaderboardResponse
import ke.co.brivont.boka.ui.theme.Boka

@Composable
fun LeaderboardScreen() {
    var data by remember { mutableStateOf<LeaderboardResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { data = GameApi.leaderboard() } catch (e: Throwable) { error = "Couldn't load the leaderboard." }
    }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "🏆 Leaderboard")
        when {
            error != null -> Text(error!!, color = Boka.textMuted, modifier = Modifier.padding(24.dp))
            data == null -> Loading()
            else -> {
                val d = data!!
                d.me?.let { me ->
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp))
                            .background(Boka.goldSoft).border(1.dp, Boka.gold, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(if (me.rank != null) "Your rank: #${me.rank}" else "Play a rated game to get ranked",
                            color = Boka.goldBright, fontWeight = FontWeight.Bold)
                        Text("${me.rating}", color = Boka.goldBright, fontWeight = FontWeight.ExtraBold)
                    }
                }
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(d.top) { row -> LeaderItem(row) }
                    if (d.top.isEmpty()) item { Text("No ranked players yet — be the first!", color = Boka.textMuted, modifier = Modifier.padding(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LeaderItem(row: LeaderRow) {
    val medal = when (row.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${row.rank}" }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
            .background(if (row.isMe) Boka.goldSoft else Boka.surface).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(medal, color = if (row.rank <= 3) Boka.gold else Boka.textFaint, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.width(40.dp), fontSize = if (row.rank <= 3) 18.sp else 14.sp)
        Text(row.username + if (row.isMe) " (you)" else "", color = Boka.text,
            fontWeight = if (row.isMe) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("${row.gamesPlayed}", color = Boka.textFaint, fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
        Spacer(Modifier.width(8.dp))
        Text("${row.rating}", color = Boka.gold, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
    }
}
