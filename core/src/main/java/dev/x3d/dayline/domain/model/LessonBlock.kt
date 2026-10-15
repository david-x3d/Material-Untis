package dev.x3d.dayline.domain.model

import dev.x3d.dayline.domain.time.UntisTime

/** One or more back-to-back periods of the same lesson, shown as a single block (e.g. a double period). */
data class LessonBlock(val lessons: List<Lesson>) {
    val first: Lesson get() = lessons.first()
    val start: UntisTime get() = lessons.first().start
    val end: UntisTime get() = lessons.last().end
    val ids: List<Long> get() = lessons.map { it.id }

    companion object {
        /** Max break (minutes) between two periods that still counts as one block. */
        const val MAX_JOIN_GAP = 5

        /** Lessons of one day → blocks. Identical consecutive periods are joined. */
        fun merge(lessons: List<Lesson>): List<LessonBlock> {
            val blocks = mutableListOf<MutableList<Lesson>>()
            for (lesson in lessons.sortedWith(compareBy({ it.start.raw }, { it.end.raw }, { it.id }))) {
                val target = blocks.firstOrNull { block ->
                    val last = block.last()
                    last.date == lesson.date &&
                        sameContent(last, lesson) &&
                        lesson.start >= last.end &&
                        lesson.start.toMinutesOfDay() - last.end.toMinutesOfDay() <= MAX_JOIN_GAP
                }
                if (target != null) target += lesson else blocks += mutableListOf(lesson)
            }
            return blocks.map { LessonBlock(it) }
        }

        private fun sameContent(a: Lesson, b: Lesson): Boolean =
            a.subject?.shortName == b.subject?.shortName &&
                a.teacher?.shortName == b.teacher?.shortName &&
                a.teacher?.originalShortName == b.teacher?.originalShortName &&
                a.room?.shortName == b.room?.shortName &&
                a.room?.originalShortName == b.room?.originalShortName &&
                a.status == b.status &&
                a.type == b.type &&
                a.info == b.info &&
                a.substText == b.substText &&
                a.lessonText == b.lessonText
    }
}
