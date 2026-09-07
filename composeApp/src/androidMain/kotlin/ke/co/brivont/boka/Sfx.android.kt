package ke.co.brivont.boka

import android.media.AudioAttributes
import android.media.SoundPool
import ke.co.brivont.boka.core.androidAppContext

// SoundPool is ideal for short, low-latency clips played repeatedly (moves).
private val pool: SoundPool by lazy {
    SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
}

private val soundIds = HashMap<String, Int>()
private var loaded = false

private fun ensureLoaded() {
    if (loaded) return
    loaded = true
    val ctx = androidAppContext
    for (name in listOf("move", "capture", "check", "castle", "promote", "notify")) {
        @Suppress("DiscouragedApi")
        val resId = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
        if (resId != 0) soundIds[name] = pool.load(ctx, resId, 1)
    }
}

actual fun playSfx(name: String) {
    try {
        ensureLoaded()
        val id = soundIds[name] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    } catch (e: Throwable) {
        // never let audio crash gameplay
    }
}
