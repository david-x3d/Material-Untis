package dev.x3d.dayline.data.rpc

import dev.x3d.dayline.domain.PeriodException
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class WebUntisClient(
    private val http: OkHttpClient,
    private val json: Json,
    private val cookieJar: SessionCookieJar,
    var reauthenticator: Reauthenticator? = null,
    private val rpcUrlOverride: HttpUrl? = null,
) {
    fun interface Reauthenticator {
        suspend fun reauthenticate()
    }

    data class Connection(val host: String, val school: String)

    @Volatile
    var connection: Connection? = null

    suspend fun authenticate(host: String, school: String, user: String, password: String): AuthenticateResult {
        connection = Connection(normalizeHost(host), school)
        cookieJar.clear()
        val params = json.encodeToJsonElement(AuthenticateParams(user = user, password = password, client = CLIENT_ID))
        val result = call("authenticate", params, retryAuth = false)
        val auth = json.decodeFromJsonElement<AuthenticateResult>(result)
        cookieJar.setSessionId(auth.sessionId, normalizeHost(host))
        return auth
    }

    suspend fun logout() {
        try {
            call("logout", JsonObject(emptyMap()), retryAuth = false)
        } catch (_: Exception) {
        } finally {
            cookieJar.clear()
        }
    }

    suspend fun getTimetable(personId: Int, personType: Int, startDate: Int, endDate: Int): List<PeriodDto> {
        val params = json.encodeToJsonElement(
            TimetableOptions(
                options = TimetableOptionsInner(
                    element = TimetableElement(id = personId, type = personType),
                    startDate = startDate,
                    endDate = endDate,
                ),
            ),
        )
        val result = call("getTimetable", params)
        return json.decodeFromJsonElement(result)
    }

    suspend fun getLatestImportTime(): Long {
        val result = call("getLatestImportTime", JsonObject(emptyMap()))
        return json.decodeFromJsonElement(result)
    }

    suspend fun getTimegridUnits(): List<TimegridDayDto> {
        val result = call("getTimegridUnits", JsonObject(emptyMap()))
        return json.decodeFromJsonElement(result)
    }

    suspend fun call(method: String, params: JsonElement, retryAuth: Boolean = true): JsonElement {
        val response = execute(method, params)
        if (response.error != null && isAuthError(response.error) && retryAuth) {
            val reauth = reauthenticator
            if (reauth != null) {
                reauth.reauthenticate()
                return call(method, params, retryAuth = false)
            }
            throw PeriodException.SessionExpired()
        }
        if (response.error != null) {
            if (isAuthError(response.error)) throw PeriodException.SessionExpired()
            throw PeriodException.Rpc(response.error.code, response.error.message ?: "Unknown RPC error")
        }
        return response.result ?: JsonNull
    }

    private suspend fun execute(method: String, params: JsonElement): JsonRpcResponse = withContext(Dispatchers.IO) {
        val conn = connection ?: throw PeriodException.Auth("No school selected")
        val url = rpcUrlOverride ?: rpcUrl(conn.host, conn.school)
        val body = JsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = method,
            params = params,
        )
        val payload = json.encodeToString(body)
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        try {
            http.newCall(request).execute().use { httpResponse ->
                if (httpResponse.code == 401) {
                    throw PeriodException.SessionExpired()
                }
                if (!httpResponse.isSuccessful) {
                    throw PeriodException.Network(IOException("HTTP ${httpResponse.code}"))
                }
                val text = httpResponse.body?.string().orEmpty()
                return@withContext json.decodeFromString(JsonRpcResponse.serializer(), text)
            }
        } catch (e: PeriodException) {
            throw e
        } catch (e: IOException) {
            throw PeriodException.Network(e)
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private val AUTH_CODES = setOf(-8504, -8520, -8521)

        fun normalizeHost(host: String): String {
            var value = host.trim()
            value = value.removePrefix("https://").removePrefix("http://")
            value = value.substringBefore("/").substringBefore(":")
            return value
        }

        fun rpcUrl(host: String, school: String): HttpUrl {
            val encodedSchool = URLEncoder.encode(school, Charsets.UTF_8.name())
            return "https://${normalizeHost(host)}/WebUntis/jsonrpc.do?school=$encodedSchool".toHttpUrl()
        }

        fun isAuthError(error: JsonRpcError): Boolean {
            val message = error.message.orEmpty().lowercase()
            return error.code in AUTH_CODES ||
                "not authenticated" in message ||
                "not logged in" in message ||
                "session" in message && ("expired" in message || "invalid" in message)
        }
    }
}
