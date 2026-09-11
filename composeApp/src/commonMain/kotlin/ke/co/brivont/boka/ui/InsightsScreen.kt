package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.InsightsApi
import ke.co.brivont.boka.data.InsightsDto
import ke.co.brivont.boka.ui.theme.Boka

/** Personalized weakness report — reads /api/insights and renders it as coaching. */
@Composable
fun InsightsScreen(onBack: () -> Unit, onPractice: () -> Unit) {
    var state by remember { mutableStateOf("loading") }   // loading | ready | error
    var data by remember { mutableStateOf<InsightsDto?>(null) }

    suspend fun run() {
        state = "loading"
        val d = InsightsApi.insights()
        if (d == null) { state = "error" } else { data = d; state = "ready" }
    }
    LaunchedEffect(Unit) { run() }

    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "◆ Insights", onBack = onBack)

        when (state) {
            "loading" -> Box(Modifier.fillMaxWidth().padding(24.dp)) { Text("Reading your games and puzzles…", color = Boka.textMuted) }
            "error" -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Couldn't load your insights right now.", color = Boka.textMuted)
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Retry", { }, Modifier.fillMaxWidth(), enabled = false)
            }
            "ready" -> {
                val d = data!!
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    // Headline
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Boka.surface).padding(18.dp)) {
                        Text("YOUR COACH SAYS", color = Boka.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(d.headline, color = Boka.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                    }

                    if (d.games.total > 0) {
                        Spacer(Modifier.height(16.dp))
                        Text("YOUR GAMES (${d.games.total})", color = Boka.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatBox("Win rate", "${d.games.winRate}%", Modifier.weight(1f), Boka.gold)
                            StatBox("W/L/D", "${d.games.wins}/${d.games.losses}/${d.games.draws}", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatBox("As White", if (d.games.byColor.white.games > 0) "${d.games.byColor.white.winRate}%" else "—", Modifier.weight(1f))
                            StatBox("As Black", if (d.games.byColor.black.games > 0) "${d.games.byColor.black.winRate}%" else "—", Modifier.weight(1f))
                        }
                        d.games.insights.forEach { line ->
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Boka.surface).padding(14.dp)) {
                                Text("• ", color = Boka.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(line, color = Boka.text, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }

                    if (d.puzzleWeaknesses.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Text("TACTICS TO DRILL", color = Boka.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        d.puzzleWeaknesses.forEach { w ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(Boka.surface).padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(w.label, color = Boka.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("miss ${w.failRate}%", color = Boka.danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(w.tip, color = Boka.textMuted, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        PrimaryButton("Practice puzzles", onPractice, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier, accent: androidx.compose.ui.graphics.Color = Boka.text) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Boka.surface).padding(vertical = 12.dp, horizontal = 14.dp)) {
        Text(label.uppercase(), color = Boka.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
