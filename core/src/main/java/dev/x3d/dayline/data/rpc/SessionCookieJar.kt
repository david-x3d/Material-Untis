package dev.x3d.dayline.data.rpc

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    @Volatile
    private var cookies: List<Cookie> = emptyList()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val session = cookies.filter { it.name.equals("JSESSIONID", ignoreCase = true) }
        if (session.isNotEmpty()) {
            this.cookies = session
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies

    fun setSessionId(sessionId: String, host: String) {
        val domain = host.removePrefix("https://").removePrefix("http://").substringBefore("/")
        cookies = listOf(
            Cookie.Builder()
                .name("JSESSIONID")
                .value(sessionId)
                .domain(domain)
                .path("/")
                .httpOnly()
                .secure()
                .build(),
        )
    }

    fun clear() {
        cookies = emptyList()
    }
}
