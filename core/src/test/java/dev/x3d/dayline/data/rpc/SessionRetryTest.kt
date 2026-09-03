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
}
