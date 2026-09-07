package ke.co.brivont.boka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.co.brivont.boka.data.AuthApi
import ke.co.brivont.boka.data.User
import ke.co.brivont.boka.ui.theme.Boka
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthed: (User) -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null; loading = true
        scope.launch {
            try {
                val res = if (isLogin) AuthApi.login(email.trim(), password)
                else AuthApi.register(email.trim(), password, name.trim(), username.trim())
                loading = false
                if (!res.token.isNullOrBlank()) onAuthed(res.user ?: User(name = username.ifBlank { email }))
                else error = res.message ?: res.error ?: "Authentication failed"
            } catch (e: Throwable) {
                loading = false; error = "Network error — please try again."
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Boka.ground).verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(40.dp))
        BokaMark(48.dp)
        Text("BOKA", color = Boka.text, fontFamily = ke.co.brivont.boka.ui.theme.BokaType.serif,
            fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 1.5.sp)
        Text("PLAY · WATCH · MASTER", color = Boka.textFaint, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(28.dp))

        val fieldMod = Modifier.fillMaxWidth().widthIn(max = 420.dp).padding(vertical = 6.dp)
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Boka.gold, unfocusedBorderColor = Boka.border,
            focusedTextColor = Boka.text, unfocusedTextColor = Boka.text,
            cursorColor = Boka.gold, focusedContainerColor = Boka.surface, unfocusedContainerColor = Boka.surface,
            focusedLabelColor = Boka.textMuted, unfocusedLabelColor = Boka.textFaint,
        )

        if (!isLogin) {
            OutlinedTextField(name, { name = it }, fieldMod, label = { Text("Full Name") }, singleLine = true, colors = fieldColors)
            OutlinedTextField(username, { username = it }, fieldMod, label = { Text("Username (unique)") }, singleLine = true, colors = fieldColors)
        }
        OutlinedTextField(email, { email = it }, fieldMod, label = { Text("Email address") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = fieldColors)
        OutlinedTextField(password, { password = it }, fieldMod, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = fieldColors)

        error?.let { Text(it, color = Boka.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = if (loading) "Please wait…" else if (isLogin) "Sign In" else "Create Account",
            onClick = { if (!loading) submit() },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
        )
        TextButton({ isLogin = !isLogin; error = null }) {
            Text(if (isLogin) "New here? Create an account" else "Have an account? Sign in", color = Boka.goldBright)
        }
    }
}
