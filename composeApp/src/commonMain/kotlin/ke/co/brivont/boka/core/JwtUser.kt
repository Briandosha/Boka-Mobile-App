package ke.co.brivont.boka.core

import ke.co.brivont.boka.data.User
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Recover the logged-in user (id + display name) from the stored JWT so a
 * cold start with a saved session still knows who you are (needed for My Games
 * win/loss). Best-effort; returns null if there's no/invalid token.
 */
@OptIn(ExperimentalEncodingApi::class)
fun tokenUser(): User? {
    val t = Session.accessToken ?: return null
    return runCatching {
        val payload = t.split('.').getOrNull(1) ?: return null
        val padded = payload.padEnd((payload.length + 3) / 4 * 4, '=')
        val bytes = Base64.UrlSafe.decode(padded)
        val obj = json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val id = (obj["uid"] ?: obj["sub"] ?: obj["id"])?.jsonPrimitive?.content ?: ""
        var name = obj["name"]?.jsonPrimitive?.content ?: obj["email"]?.jsonPrimitive?.content ?: "Player"
        if (name.contains('@')) name = name.substringBefore('@')
        User(id = id, name = name)
    }.getOrNull()
}

/** True if the stored access token is missing or its `exp` is at/near now (needs refresh). */
@OptIn(ExperimentalEncodingApi::class)
fun accessTokenExpired(skewSeconds: Long = 20): Boolean {
    val t = Session.accessToken ?: return true
    return runCatching {
        val payload = t.split('.').getOrNull(1) ?: return true
        val padded = payload.padEnd((payload.length + 3) / 4 * 4, '=')
        val obj = json.parseToJsonElement(Base64.UrlSafe.decode(padded).decodeToString()).jsonObject
        val exp = obj["exp"]?.jsonPrimitive?.content?.toLongOrNull() ?: return true
        nowEpochSeconds() >= (exp - skewSeconds)
    }.getOrDefault(true)
}
