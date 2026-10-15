package dev.x3d.dayline.data.rpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebContextTest {
    @Test
    fun readsTokenTenantAndLinkedStudent() {
        // Shape of embedded.do (anonymized).
        val html = """
            <script>var dojoConfig = { grupet: {"mode":"EMBEDDED","loginServiceConfig":{"user":{"name":"parent@example.org",
            "id":11,"roleId":12,"personId":22,"persons":[{"id":33,"type":5,"displayName":"Muster Max","longName":"Muster",
            "foreName":"Max"}],"email":"parent@example.org"}},"csrfHeader":"X-CSRF-TOKEN","csrfToken":"abc-123_x",
            "ui2020":true,"tenantId":"1234567"} };</script>
        """.trimIndent()
        val context = WebUntisClient.parseWebContext(html)
        assertEquals("abc-123_x", context.csrfToken)
        assertEquals("1234567", context.tenantId)
        assertEquals(12, context.roleId)
        assertEquals(listOf(WebPersonDto(33, 5, "Muster Max")), context.persons)
    }

    @Test
    fun loginPageHasNoToken() {
        val context = WebUntisClient.parseWebContext("<html><form action=\"j_spring_security_check\"></form></html>")
        assertNull(context.csrfToken)
        assertEquals(emptyList<WebPersonDto>(), context.persons)
    }
}
