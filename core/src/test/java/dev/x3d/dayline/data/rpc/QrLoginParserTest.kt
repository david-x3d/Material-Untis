package dev.x3d.dayline.data.rpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrLoginParserTest {
    @Test
    fun parsesUntisQr() {
        val parsed = QrLoginParser.parse(
            "untis://setschool?url=https://ajax.webuntis.com&school=example&user=student&key=AABBCCDDEEFF00112233445566778899",
        )
        assertEquals("ajax.webuntis.com", parsed?.school?.host)
        assertEquals("example", parsed?.school?.loginName)
        assertEquals("student", parsed?.user)
        assertEquals("AABBCCDDEEFF00112233445566778899", parsed?.secret)
    }

    @Test
    fun rejectsUnknown() {
        assertNull(QrLoginParser.parse("https://example.com"))
    }
}
