package br.com.equilibra.mobile.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CookieJar persistente que guarda cookies httpOnly (ex.: equilibra_refresh)
 * em EncryptedSharedPreferences (Keystore). Necessário porque o refresh token
 * é httpOnly e não pode ser lido via JS; o OkHttp precisa reenviá-lo em
 * POST /auth/refresh e /auth/logout mesmo após restart do app.
 *
 * Formato de persistência: "name|value|expiresAt|domain|path|secure|httpOnly|hostOnly"
 */
@Singleton
class PersistentCookieJar @Inject constructor(
    @ApplicationContext private val context: Context
) : CookieJar {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cookie_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val memoryCache = mutableMapOf<String, MutableList<Cookie>>()

    companion object {
        private const val KEY_COOKIES = "cookies"
        private const val COOKIE_REFRESH_NAME = "equilibra_refresh"
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val existing = loadForHost(host).toMutableList()
        for (newCookie in cookies) {
            existing.removeAll { it.name == newCookie.name }
            if (newCookie.expiresAt > System.currentTimeMillis()) {
                existing.add(newCookie)
            }
        }
        memoryCache[host] = existing
        persist(host, existing)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        // Tenta cache em memória; se vazio, carrega do disco
        if (!memoryCache.containsKey(host)) {
            memoryCache[host] = loadPersisted(host).toMutableList()
        }
        val now = System.currentTimeMillis()
        val valid = memoryCache[host]?.filter { it.expiresAt > now } ?: emptyList()
        // Limpa expirados do disco
        if (valid.size != memoryCache[host]?.size) {
            memoryCache[host] = valid.toMutableList()
            persist(host, valid)
        }
        return valid
    }

    fun clear() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }

    private fun loadForHost(host: String): List<Cookie> = memoryCache[host] ?: loadPersisted(host)

    private fun loadPersisted(host: String): List<Cookie> {
        val raw = prefs.getString(keyForHost(host), null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(";;").mapNotNull { deserializeCookie(it) }
    }

    private fun persist(host: String, cookies: List<Cookie>) {
        val raw = cookies.joinToString(";;") { serializeCookie(it) }
        prefs.edit().putString(keyForHost(host), raw).apply()
    }

    private fun keyForHost(host: String) = "${KEY_COOKIES}_$host"

    private fun serializeCookie(cookie: Cookie): String = listOf(
        cookie.name, cookie.value, cookie.expiresAt.toString(),
        cookie.domain, cookie.path, cookie.secure.toString(),
        cookie.httpOnly.toString(), cookie.hostOnly.toString()
    ).joinToString("|")

    private fun deserializeCookie(raw: String): Cookie? {
        val parts = raw.split("|")
        if (parts.size < 8) return null
        return try {
            Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .expiresAt(parts[2].toLong())
                .apply {
                    if (parts[7].toBoolean()) hostOnlyDomain(parts[3]) else domain(parts[3])
                    path(parts[4])
                    if (parts[5].toBoolean()) secure()
                    if (parts[6].toBoolean()) httpOnly()
                }
                .build()
        } catch (_: Exception) { null }
    }
}
