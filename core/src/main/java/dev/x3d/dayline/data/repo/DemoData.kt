package dev.x3d.dayline.data.repo

import dev.x3d.dayline.domain.model.*
import dev.x3d.dayline.domain.time.*
import java.time.DayOfWeek

/** Dates follow today so the showcase stays useful after the release. */
object DemoData {
    private fun monday(today: UntisDate) = UntisDate.from(today.toLocalDate().with(DayOfWeek.MONDAY))
    fun holidays(today: UntisDate = UntisDate.today()): List<Holiday> {
        val start = monday(today).plusDays(7)
        return listOf(Holiday("Demo-Ferien", "Demo-Ferien – nächste Woche", start, start.plusDays(6)))
    }
    fun lessons(today: UntisDate = UntisDate.today()): List<Lesson> =
        (-2L..6L).flatMap { week ->
            (0L..4L).flatMap { day ->
                val date = monday(today).plusDays(week * 7 + day)
                if (holidays(today).any { date in it }) emptyList() else day(date, day.toInt())
            }
        }

    private fun day(date: UntisDate, variant: Int): List<Lesson> {
        val subjects = listOf("M" to "Mathematik", "D" to "Deutsch", "E" to "Englisch", "Bio" to "Biologie", "Ge" to "Geschichte", "Sp" to "Sport", "Mu" to "Musik", "Ku" to "Kunst", "Ch" to "Chemie", "Inf" to "Informatik")
        val slots = listOf(745 to 830, 835 to 920, 940 to 1025, 1030 to 1115, 1135 to 1220, 1225 to 1310, 1400 to 1445, 1450 to 1535, 1540 to 1625)
        return slots.mapIndexed { index, (start, end) ->
            val subject = subjects[(index + variant) % subjects.size]
            val base = Lesson(
                id = date.yyyymmdd.toLong() * 100 + index,
                date = date, start = UntisTime(start), end = UntisTime(end),
                subject = NamedRef(index + 1, subject.first, subject.second),
                teacher = NamedRef(10, "MÜL", "Anna Müller"),
                room = NamedRef(101, "A101", "Klassenraum A101"),
                klass = NamedRef(12, "10a", "Klasse 10a"),
                status = LessonStatus.NORMAL, type = LessonType.LESSON,
                info = "Demo: ${subject.second} – Unterrichtsmaterial und Aufgaben im Detail.",
                substText = null, lessonText = "Bitte Heft und Schulbuch mitbringen.",
            )
            when (index) {
                0 -> base
                1 -> base.copy(status = LessonStatus.CANCELLED, info = "Entfall: Lehrkraft erkrankt. Keine Vertretung.", lessonText = "Aufgaben auf Seite 42 selbstständig bearbeiten.")
                2 -> base.copy(status = LessonStatus.IRREGULAR, room = NamedRef(205, "B205", "Biologieraum", "A101"), substText = "Raumverlegung: A101 → B205", info = "Bitte direkt zum Fachraum im B-Trakt kommen.")
                3 -> base.copy(status = LessonStatus.IRREGULAR, teacher = NamedRef(11, "SCH", "Lukas Schmidt", "MÜL"), substText = "Lehrerwechsel: Schmidt vertritt Müller.")
                4 -> base.copy(status = LessonStatus.IRREGULAR, subject = NamedRef(20, "Ph", "Physik", subject.first), teacher = NamedRef(12, "WEB", "Eva Weber", "MÜL"), room = NamedRef(301, "C301", "Physikraum", "A101"), substText = "Fachwechsel: ${subject.second} → Physik; neue Lehrkraft und neuer Raum.")
                5 -> base.copy(type = LessonType.EXAM, info = "Klassenarbeit: 45 Minuten. Taschenrechner erlaubt.", lessonText = "Themen: Gleichungen und Funktionen.")
                6, 7 -> base.copy(subject = NamedRef(30, "VA", "Schulveranstaltung"), teacher = NamedRef(13, "ORG", "Organisationsteam"), room = NamedRef(400, "Aula", "Aula"), status = LessonStatus.IRREGULAR, info = "Schulveranstaltung: Projektpräsentationen und Schulfest.", substText = "Gemeinsame Veranstaltung aller Klassen.", lessonText = "Treffpunkt Aula; Anwesenheit erforderlich.")
                else -> when (variant) {
                    0 -> base.copy(type = LessonType.OFFICE_HOUR, info = "Sprechstunde: Fragen zur Projektarbeit.")
                    1 -> base.copy(type = LessonType.STANDBY, info = "Bereitschaft / zusätzliche Betreuung.")
                    2 -> base.copy(type = LessonType.BREAK_SUPERVISION, info = "Pausenaufsicht im Schulhof.")
                    3 -> base.copy(status = LessonStatus.CANCELLED, info = "Nachmittagsunterricht entfällt: Konferenz.")
                    else -> base.copy(end = UntisTime(1710), info = "Doppelstunde: Workshop mit praktischen Übungen.")
                }
            }
        }
    }
}
