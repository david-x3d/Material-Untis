package dev.x3d.dayline.data.rpc

import java.util.Base64
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

    /** School login name; WebUntis REST endpoints also want it as the `schoolname` cookie. */
    @Volatile
    var school: String? = null

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val name = school ?: return cookies
        if (!url.encodedPath.startsWith("/WebUntis/")) return cookies
        val schoolCookie = Cookie.Builder()
            .name("schoolname")
            .value(schoolCookieValue(name))
            .domain(url.host)
            .path("/")
            .build()
        return cookies + schoolCookie
    }

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

    companion object {
        /** Same encoding as the WebUntis web client: `"_" + base64(school)`, quoted. */
        fun schoolCookieValue(school: String): String =
            "\"_" + Base64.getEncoder().encodeToString(school.toByteArray(Charsets.UTF_8)) + "\""
    }
}
