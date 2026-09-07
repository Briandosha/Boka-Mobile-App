package ke.co.brivont.boka.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.User
import ke.co.brivont.boka.ui.theme.Boka
import ke.co.brivont.boka.ui.theme.BokaType

private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun ProfileScreen(user: User, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Boka.ground)) {
        TopBar(title = "Profile")
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            // Identity card
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(Boka.surface)
                    .border(1.dp, Boka.border, CardShape).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Boka.goldSoft)
                        .border(1.5.dp, Boka.text, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.name.trim().firstOrNull()?.uppercase() ?: "P",
                        color = Boka.text, fontFamily = BokaType.serif,
                        fontWeight = FontWeight.Bold, fontSize = 24.sp,
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(user.name, color = Boka.text, fontFamily = BokaType.serif,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Boka member", color = Boka.textFaint, fontFamily = BokaType.sans, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("SETTINGS", color = Boka.textFaint, fontFamily = BokaType.sans,
                fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(8.dp))
            SettingRow(
                icon = if (Boka.isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                label = "Appearance",
                value = if (Boka.isDark) "Dark" else "Light",
                onClick = { Boka.setDarkTheme(!Boka.isDark) },
            )
            SettingRow(Icons.Filled.VolumeUp, "Sound effects", "On")
            SettingRow(Icons.Filled.Notifications, "Move alerts", "On")
            SettingRow(Icons.Filled.Info, "About Boka", "v1.0")

            Spacer(Modifier.height(28.dp))
            DangerButton("Sign out", Icons.AutoMirrored.Filled.Logout, onLogout)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(CardShape)
            .background(Boka.surface).border(1.dp, Boka.border, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Boka.text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Text(label, color = Boka.text, fontFamily = BokaType.sans,
            fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Boka.textMuted, fontFamily = BokaType.sans,
            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun DangerButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(CardShape)
            .background(Boka.surface).border(1.dp, Boka.danger, CardShape)
            .clickable(interaction, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Boka.danger, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, color = Boka.danger, fontFamily = BokaType.sans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
