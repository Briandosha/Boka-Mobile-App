package ke.co.brivont.boka.core

import android.content.Context

/** Set from MainActivity.onCreate before any store access. */
lateinit var androidAppContext: Context

actual fun provideStore(): KeyValueStore = object : KeyValueStore {
    private val prefs = androidAppContext.getSharedPreferences("boka", Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

actual fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000
