package dev.x3d.dayline.data.rpc

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCookieJarTest {
    private val url = "https://school.webuntis.com/WebUntis/jsonrpc.do".toHttpUrl()

    @Test
    fun sendsSessionAndSchoolToAuthenticatedHost() {
        val jar = authenticatedJar()
        val cookies = jar.loadForRequest(url)
        assertEquals(listOf("JSESSIONID", "schoolname"), cookies.map { it.name })
        assertEquals("secret", cookies.first().value)
    }

    @Test
    fun doesNotLeakCookiesToOtherHostsOrSubdomains() {
        val jar = authenticatedJar()
        for (host in listOf("other.webuntis.com", "child.school.webuntis.com")) {
            assertTrue(jar.loadForRequest("https://$host/WebUntis/jsonrpc.do".toHttpUrl()).isEmpty())
        }
    }

    @Test
    fun doesNotSendSecureSessionOverHttp() {
        assertTrue(authenticatedJar().loadForRequest("http://school.webuntis.com/WebUntis/jsonrpc.do".toHttpUrl()).isEmpty())
    }

    @Test
    fun respectsServerCookiePathAndExpiry() {
        val jar = authenticatedJar()
        jar.saveFromResponse(url, listOf(Cookie.Builder()
            .name("JSESSIONID").value("restricted").hostOnlyDomain(url.host).path("/WebUntis/").build()))
        assertTrue(jar.loadForRequest("https://school.webuntis.com/other".toHttpUrl()).isEmpty())
        jar.saveFromResponse(url, listOf(Cookie.Builder()
            .name("JSESSIONID").value("expired").hostOnlyDomain(url.host).expiresAt(1).build()))
        assertTrue(jar.loadForRequest(url).isEmpty())
    }

    private fun authenticatedJar() = SessionCookieJar().apply {
        school = "example"
        setSessionId("secret", url.host)
    }
}
