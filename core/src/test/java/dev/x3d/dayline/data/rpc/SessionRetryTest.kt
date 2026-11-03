package dev.x3d.dayline.data.rpc

import dev.x3d.dayline.domain.PeriodException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRetryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun retriesOnceAfterHttp401() = checkHttp401Retry(repeated = false, retryAuth = true)

    @Test
    fun doesNotLoopOnRepeatedHttp401() = checkHttp401Retry(repeated = true, retryAuth = true)

    @Test
    fun doesNotRetryHttp401DuringAuthentication() = checkHttp401Retry(repeated = true, retryAuth = false)

    private fun checkHttp401Retry(repeated: Boolean, retryAuth: Boolean) = runBlocking {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(
                if (repeated) MockResponse().setResponseCode(401)
                else MockResponse().setBody("""{"jsonrpc":"2.0","id":"2","result":42}"""),
            )
            server.start()
            val reauthCount = AtomicInteger(0)
            val cookieJar = SessionCookieJar()
            val client = WebUntisClient(
                http = OkHttpClient.Builder().cookieJar(cookieJar).build(),
                json = json,
                cookieJar = cookieJar,
                rpcUrlOverride = server.url("/WebUntis/jsonrpc.do?school=example"),
            )
            client.connection = WebUntisClient.Connection("127.0.0.1", "example")
            client.reauthenticator = WebUntisClient.Reauthenticator { reauthCount.incrementAndGet() }

            try {
                val result = client.call("getLatestImportTime", JsonObject(emptyMap()), retryAuth)
                assertTrue(!repeated && retryAuth)
                assertEquals("42", result.toString())
            } catch (e: PeriodException.SessionExpired) {
                assertTrue(repeated || !retryAuth)
            }
            assertEquals(if (retryAuth) 1 else 0, reauthCount.get())
            assertEquals(if (retryAuth) 2 else 1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun retriesOnceAfterAuthError() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody("""{"jsonrpc":"2.0","id":"1","error":{"code":-8504,"message":"not authenticated"}}"""),
        )
        server.enqueue(
            MockResponse().setBody("""{"jsonrpc":"2.0","id":"2","result":1756886400000}"""),
        )
        server.start()
        val reauthCount = AtomicInteger(0)
        val cookieJar = SessionCookieJar()
        val http = OkHttpClient.Builder().cookieJar(cookieJar).build()
        val client = WebUntisClient(
            http = http,
            json = json,
            cookieJar = cookieJar,
            rpcUrlOverride = server.url("/WebUntis/jsonrpc.do?school=example"),
        )
        client.connection = WebUntisClient.Connection("127.0.0.1", "example")
        client.reauthenticator = WebUntisClient.Reauthenticator { reauthCount.incrementAndGet() }

        val latest = client.getLatestImportTime()
        assertEquals(1_756_886_400_000L, latest)
        assertEquals(1, reauthCount.get())
        assertEquals(2, server.requestCount)
        server.shutdown()
    }

    @Test
    fun doesNotLoopOnRepeatedAuthError() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody("""{"jsonrpc":"2.0","id":"1","error":{"code":-8504,"message":"not authenticated"}}"""),
        )
        server.enqueue(
            MockResponse().setBody("""{"jsonrpc":"2.0","id":"2","error":{"code":-8504,"message":"not authenticated"}}"""),
        )
        server.start()
        val cookieJar = SessionCookieJar()
        val http = OkHttpClient.Builder().cookieJar(cookieJar).build()
        val client = WebUntisClient(
            http = http,
            json = json,
            cookieJar = cookieJar,
            rpcUrlOverride = server.url("/WebUntis/jsonrpc.do?school=example"),
        )
        client.connection = WebUntisClient.Connection("127.0.0.1", "example")
        client.reauthenticator = WebUntisClient.Reauthenticator { }

        try {
            client.getLatestImportTime()
            throw AssertionError("expected session expired")
        } catch (e: PeriodException.SessionExpired) {
            assertTrue(true)
        }
        assertEquals(2, server.requestCount)
        server.shutdown()
    }

    @Test
    fun lockedDateRangeIsNotAnAuthError() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody("""{"jsonrpc":"2.0","id":"1","error":{"code":-7004,"message":"no allowed date"}}"""),
        )
        server.start()
        val reauthCount = AtomicInteger(0)
        val cookieJar = SessionCookieJar()
        val client = WebUntisClient(
            http = OkHttpClient.Builder().cookieJar(cookieJar).build(),
            json = json,
            cookieJar = cookieJar,
            rpcUrlOverride = server.url("/WebUntis/jsonrpc.do?school=example"),
        )
        client.connection = WebUntisClient.Connection("127.0.0.1", "example")
        client.reauthenticator = WebUntisClient.Reauthenticator { reauthCount.incrementAndGet() }

        try {
            client.getTimetable(1, 5, 20261109, 20261115)
            throw AssertionError("expected DateNotAllowed")
        } catch (e: PeriodException.DateNotAllowed) {
            assertTrue(true)
        }
        assertEquals(0, reauthCount.get())
        assertEquals(1, server.requestCount)
        server.shutdown()
    }

    @Test
    fun parsesHolidays() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"jsonrpc":"2.0","id":"1","result":[{"id":3,"name":"Herbst","longName":"Herbstferien","startDate":20261019,"endDate":20261101}]}""",
            ),
        )
        server.start()
        val cookieJar = SessionCookieJar()
        val client = WebUntisClient(
            http = OkHttpClient.Builder().cookieJar(cookieJar).build(),
            json = json,
            cookieJar = cookieJar,
            rpcUrlOverride = server.url("/WebUntis/jsonrpc.do?school=example"),
        )
        client.connection = WebUntisClient.Connection("127.0.0.1", "example")

        val holidays = client.getHolidays()
        assertEquals(1, holidays.size)
        assertEquals("Herbstferien", holidays[0].longName)
        assertEquals(20261019, holidays[0].startDate)
        assertEquals(20261101, holidays[0].endDate)
        server.shutdown()
    }
}
