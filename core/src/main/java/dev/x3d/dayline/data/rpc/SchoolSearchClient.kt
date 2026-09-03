package dev.x3d.dayline.data.rpc

import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.model.School
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SchoolSearchClient(
    private val http: OkHttpClient,
    private val json: Json,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    suspend fun search(query: String): List<School> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val body = JsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "searchSchool",
            params = JsonArray(
                listOf(
                    buildJsonObject { put("search", JsonPrimitive(trimmed)) },
                ),
            ),
        )
        val request = Request.Builder()
            .url(endpoint)
            .post(json.encodeToString(JsonRpcRequest.serializer(), body).toRequestBody(JSON_MEDIA))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw PeriodException.Network(IOException("HTTP ${response.code}"))
                }
                val parsed = json.decodeFromString(SchoolSearchEnvelope.serializer(), response.body?.string().orEmpty())
                if (parsed.error != null) {
                    throw PeriodException.Rpc(parsed.error.code, parsed.error.message ?: "School search failed")
                }
                return@withContext parsed.result?.schools.orEmpty().mapNotNull { it.toSchool() }
            }
        } catch (e: PeriodException) {
            throw e
        } catch (e: IOException) {
            throw PeriodException.Network(e)
        }
    }

    private fun SchoolDto.toSchool(): School? {
        val login = loginName?.trim().orEmpty()
        val host = WebUntisClient.normalizeHost(server ?: serverUrl.orEmpty())
        if (login.isEmpty() || host.isEmpty()) return null
        return School(
            displayName = displayName?.trim().orEmpty().ifEmpty { login },
            loginName = login,
            host = host,
            address = address.orEmpty(),
        )
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://mobile.webuntis.com/ms/schoolquery2"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
