package dev.x3d.dayline.data.mapper

import dev.x3d.dayline.data.rpc.AbsencesEnvelope
import dev.x3d.dayline.domain.model.AbsenceStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AbsenceMapperTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun parsesWebUntisResponse() {
        val body = """
            {"data":{"absences":[
              {"id":7,"startDate":20260302,"endDate":20260302,"startTime":750,"endTime":1305,
               "reason":"Krankheit","text":"","interruptions":[],"isExcused":true,
               "excuseStatus":"entschuldigt","excuse":{"id":3,"text":"","excuseDate":20260303,"isExcused":true}},
              {"id":8,"startDate":20261001,"endDate":20261002,"startTime":750,"endTime":1305,
               "reason":{"name":"Krank"},"isExcused":false,"excuseStatus":null,"excuse":null},
              {"id":9,"startDate":20261005,"endDate":20261005,"startTime":940,"endTime":1115,
               "reason":"Krank","isExcused":false,"excuseStatus":"nicht entschuldigt"}
            ],"absenceReasons":[]}}
        """.trimIndent()
        val list = json.decodeFromString(AbsencesEnvelope.serializer(), body).data!!.absences.map(AbsenceMapper::toDomain)
        assertEquals(3, list.size)
        assertEquals(AbsenceStatus.EXCUSED, list[0].status)
        assertEquals("entschuldigt", list[0].statusLabel)
        assertEquals("Krankheit", list[0].reason)
        assertNull(list[0].text)
        assertEquals(AbsenceStatus.OPEN, list[1].status)
        assertEquals("Krank", list[1].reason)
        assertEquals(AbsenceStatus.UNEXCUSED, list[2].status)
        assertEquals(listOf(9L, 8L, 7L), AbsenceMapper.sort(list).map { it.id })
    }
}
