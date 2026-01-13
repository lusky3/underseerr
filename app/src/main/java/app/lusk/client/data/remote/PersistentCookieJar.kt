package app.lusk.client.data.remote

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * A simple persistent cookie jar that saves cookies to SharedPreferences.
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val sharedPreferences = context.getSharedPreferences("lusk_cookies", Context.MODE_PRIVATE)
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        loadCookiesFromPrefs()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        Log.d("PersistentCookieJar", "Saving ${cookies.size} cookies for host: $host")
        val currentCookies = cookieStore.getOrPut(host) { mutableListOf() }
        
        // Remove existing cookies with same name
        val names = cookies.map { it.name }
        currentCookies.removeAll { it.name in names }
        currentCookies.addAll(cookies)
        
        saveCookiesToPrefs(host, currentCookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: emptyList()
        Log.d("PersistentCookieJar", "Loading ${cookies.size} cookies for host: $host")
        
        // Filter out expired cookies
        val now = System.currentTimeMillis()
        val validCookies = cookies.filter { it.expiresAt > now }
        
        if (validCookies.size != cookies.size) {
            cookieStore[host] = validCookies.toMutableList()
            saveCookiesToPrefs(host, validCookies)
        }
        
        return validCookies
    }

    fun clear() {
        cookieStore.clear()
        sharedPreferences.edit().clear().apply()
        Log.d("PersistentCookieJar", "All cookies cleared")
    }

    private fun saveCookiesToPrefs(host: String, cookies: List<Cookie>) {
        val cookieStrings = cookies.map { serializeCookie(it) }.toSet()
        sharedPreferences.edit().putStringSet(host, cookieStrings).apply()
    }

    private fun loadCookiesFromPrefs() {
        val allEntries = sharedPreferences.all
        for ((host, value) in allEntries) {
            if (value is Set<*>) {
                val cookies = value.mapNotNull { it as? String }.mapNotNull { deserializeCookie(it) }
                cookieStore[host] = cookies.toMutableList()
            }
        }
    }

    private fun serializeCookie(cookie: Cookie): String {
        return "${cookie.name}|${cookie.value}|${cookie.expiresAt}|${cookie.domain}|${cookie.path}|${cookie.secure}|${cookie.httpOnly}|${cookie.persistent}|${cookie.hostOnly}"
    }

    private fun deserializeCookie(serialized: String): Cookie? {
        return try {
            val parts = serialized.split("|")
            if (parts.size < 9) return null
            Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .expiresAt(parts[2].toLong())
                .domain(parts[3])
                .path(parts[4])
                .apply {
                    if (parts[5].toBoolean()) secure()
                    if (parts[6].toBoolean()) httpOnly()
                    if (parts[8].toBoolean()) hostOnlyDomain(parts[3])
                }
                .build()
        } catch (e: Exception) {
            Log.e("PersistentCookieJar", "Failed to deserialize cookie", e)
            null
        }
    }
}
