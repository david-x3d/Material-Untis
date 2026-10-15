package dev.x3d.dayline.data.rpc

import dev.x3d.dayline.domain.PeriodException
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
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
        set(value) {
            field = value
            cookieJar.school = value?.school
        }

    suspend fun authenticate(host: String, school: String, user: String, password: String): AuthenticateResult {
        connection = Connection(normalizeHost(host), school)
        cookieJar.clear()
        webContext = null
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

    /** CSRF token, tenant and linked students, as the web app gets them from `embedded.do`. */
    data class WebContext(
        val csrfToken: String?,
        val tenantId: String?,
        val roleId: Int?,
        val persons: List<WebPersonDto>,
    )

    @Volatile
    private var webContext: WebContext? = null

    suspend fun getWebContext(refresh: Boolean = false, retryAuth: Boolean = true): WebContext {
        if (!refresh) webContext?.let { return it }
        val conn = connection ?: throw PeriodException.Auth("No school selected")
        val url = "https://${normalizeHost(conn.host)}/WebUntis/embedded.do".toHttpUrl()
            .newBuilder().addQueryParameter("showSidebar", "false").build()
        val html = try {
            restGet(url, expectJson = false)
        } catch (e: PeriodException.SessionExpired) {
            val reauth = reauthenticator
            if (!retryAuth || reauth == null) throw e
            reauth.reauthenticate()
            return getWebContext(refresh = true, retryAuth = false)
        }
        val context = parseWebContext(html)
        // Logged out: the login page has no token.
        if (context.csrfToken == null) {
            val reauth = reauthenticator
            if (!retryAuth || reauth == null) throw PeriodException.SessionExpired()
            reauth.reauthenticate()
            return getWebContext(refresh = true, retryAuth = false)
        }
        webContext = context
        return context
    }

    suspend fun getAbsenceMeta(): AbsenceMetaDto {
        val conn = connection ?: throw PeriodException.Auth("No school selected")
        val url = "https://${normalizeHost(conn.host)}/WebUntis/api/classreg/absences/meta".toHttpUrl()
        val text = withSessionRetry { restGet(url, tenantId = getWebContext().tenantId) }
        return json.decodeFromString(AbsenceMetaEnvelope.serializer(), text).data
            ?: throw PeriodException.Rpc(0, text.take(200))
    }

    /** Reports an absence ("Krankmeldung") exactly like the web app. */
    suspend fun reportAbsence(request: ReportAbsenceRequest) {
        val conn = connection ?: throw PeriodException.Auth("No school selected")
        val url = "https://${normalizeHost(conn.host)}/WebUntis/api/classreg/absences/students/self".toHttpUrl()
        val body = json.encodeToString(ReportAbsenceRequest.serializer(), request)
        try {
            restPost(url, body, getWebContext())
        } catch (e: PeriodException.SessionExpired) {
            // Expired session or stale CSRF token: sign in again, fetch a fresh token, retry once.
            reauthenticator?.reauthenticate() ?: throw e
            restPost(url, body, getWebContext(refresh = true))
        }
    }

    private suspend fun withSessionRetry(block: suspend () -> String): String =
        try {
            block()
        } catch (e: PeriodException.SessionExpired) {
            val reauth = reauthenticator ?: throw e
            reauth.reauthenticate()
            webContext = null
            block()
        }

    suspend fun getHolidays(): List<HolidayDto> {
        val result = call("getHolidays", JsonObject(emptyMap()))
        if (result is JsonNull) return emptyList()
        return json.decodeFromJsonElement(result)
    }

    suspend fun getCurrentSchoolYear(): SchoolYearDto? {
        val result = call("getCurrentSchoolyear", JsonObject(emptyMap()))
        if (result is JsonNull) return null
        return json.decodeFromJsonElement(result)
    }

    /**
     * Student absences. Not part of JSON-RPC: this is the same REST endpoint the WebUntis web app
     * uses for "My absences". It is authorised by the JSESSIONID + schoolname cookies.
     */
    suspend fun getStudentAbsences(studentId: Int, startDate: Int, endDate: Int, retryAuth: Boolean = true): List<AbsenceDto> {
        val conn = connection ?: throw PeriodException.Auth("No school selected")
        val url = absencesUrl(conn.host, studentId, startDate, endDate)
        val text = try {
            restGet(url)
        } catch (e: PeriodException.SessionExpired) {
            val reauth = reauthenticator
            if (!retryAuth || reauth == null) throw e
            reauth.reauthenticate()
            return getStudentAbsences(studentId, startDate, endDate, retryAuth = false)
        }
        // No "data" means an error page/message, never "no absences".
        val data = json.decodeFromString(AbsencesEnvelope.serializer(), text).data
            ?: throw PeriodException.Rpc(0, text.take(200))
        return data.absences
    }

    private suspend fun restGet(url: HttpUrl, expectJson: Boolean = true, tenantId: String? = null): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", if (expectJson) "application/json" else "text/html")
                .apply { if (tenantId != null) header("Tenant-Id", tenantId) }
                .build()
            execute(request, expectJson)
        }

    private suspend fun restPost(url: HttpUrl, body: String, context: WebContext): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .apply {
                    context.csrfToken?.let { header("X-CSRF-TOKEN", it) }
                    context.tenantId?.let { header("Tenant-Id", it) }
                }
                .build()
            execute(request, expectJson = false)
        }

    private fun execute(request: Request, expectJson: Boolean): String {
        try {
            http.newCall(request).execute().use { httpResponse ->
                if (httpResponse.code == 401 || httpResponse.code == 403) {
                    throw PeriodException.SessionExpired()
                }
                val text = httpResponse.body?.string().orEmpty()
                if (!httpResponse.isSuccessful) {
                    if (httpResponse.code in 400..499) throw PeriodException.Rpc(httpResponse.code, errorMessage(text))
                    throw PeriodException.Network(IOException("HTTP ${httpResponse.code}"))
                }
                // An expired session is answered with the HTML login page instead of JSON.
                if (expectJson && !text.trimStart().startsWith("{")) throw PeriodException.SessionExpired()
                return text
            }
        } catch (e: PeriodException) {
            throw e
        } catch (e: IOException) {
            throw PeriodException.Network(e)
        }
    }

    /** WebUntis REST errors look like `{"errorMessage": "..."}` or `{"errors":[{"title": "..."}]}`. */
    private fun errorMessage(text: String): String =
        Regex("\"(?:errorMessage|message|title|detail)\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            ?: text.take(200)

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
            if (response.error.code == NO_ALLOWED_DATE) throw PeriodException.DateNotAllowed()
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
        private const val NO_ALLOWED_DATE = -7004

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

        fun absencesUrl(host: String, studentId: Int, startDate: Int, endDate: Int): HttpUrl =
            "https://${normalizeHost(host)}/WebUntis/api/classreg/absences/students".toHttpUrl()
                .newBuilder()
                .addQueryParameter("startDate", startDate.toString())
                .addQueryParameter("endDate", endDate.toString())
                .addQueryParameter("studentId", studentId.toString())
                .addQueryParameter("excuseStatusId", "-1")
                .build()

        fun parseWebContext(html: String): WebContext {
            fun str(key: String) = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(html)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
            val roleId = Regex("\"roleId\"\\s*:\\s*(\\d+)").find(html)?.groupValues?.get(1)?.toIntOrNull()
            val persons = arrayAfter(html, "\"persons\"")?.let { raw ->
                runCatching { LENIENT.decodeFromString(ListSerializer(WebPersonDto.serializer()), raw) }.getOrNull()
            }.orEmpty()
            return WebContext(str("csrfToken"), str("tenantId"), roleId, persons)
        }

        /** The JSON array that follows [key], by bracket matching (the page embeds a JS object). */
        private fun arrayAfter(text: String, key: String): String? {
            val keyAt = text.indexOf(key).takeIf { it >= 0 } ?: return null
            val start = text.indexOf('[', keyAt).takeIf { it >= 0 } ?: return null
            var depth = 0
            var inString = false
            var i = start
            while (i < text.length) {
                val c = text[i]
                when {
                    inString && c == '\\' -> i++
                    c == '"' -> inString = !inString
                    !inString && c == '[' -> depth++
                    !inString && c == ']' -> if (--depth == 0) return text.substring(start, i + 1)
                }
                i++
            }
            return null
        }

        private val LENIENT = Json { ignoreUnknownKeys = true; isLenient = true }

        fun isAuthError(error: JsonRpcError): Boolean {
            val message = error.message.orEmpty().lowercase()
            return error.code in AUTH_CODES ||
                "not authenticated" in message ||
                "not logged in" in message ||
                "session" in message && ("expired" in message || "invalid" in message)
        }
    }
}
