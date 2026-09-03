package dev.x3d.dayline.data.rpc

import dev.x3d.dayline.domain.model.School
import java.net.URLDecoder

data class QrLogin(
    val school: School,
    val user: String,
    val secret: String,
)

object QrLoginParser {
    fun parse(raw: String): QrLogin? {
        val value = raw.trim()
        if (!value.startsWith("untis://", ignoreCase = true)) return null
        val query = value.substringAfter('?', missingDelimiterValue = "")
        if (query.isEmpty()) return null
        val params = query.split('&').mapNotNull { part ->
            val key = part.substringBefore('=')
            val v = part.substringAfter('=', missingDelimiterValue = "")
            if (key.isEmpty()) null else key to urlDecode(v)
        }.toMap()
        val url = params["url"].orEmpty()
        val schoolName = params["school"].orEmpty()
        val user = params["user"].orEmpty()
        val key = params["key"].orEmpty()
        if (schoolName.isBlank() || user.isBlank() || key.isBlank()) return null
        val host = WebUntisClient.normalizeHost(url)
        if (host.isBlank()) return null
        return QrLogin(
            school = School(
                displayName = schoolName,
                loginName = schoolName,
                host = host,
            ),
            user = user,
            secret = key,
        )
    }

    private fun urlDecode(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
}
