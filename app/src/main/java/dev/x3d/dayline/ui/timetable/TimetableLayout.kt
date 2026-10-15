package dev.x3d.dayline.ui.timetable

import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.LessonBlock
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.time.UntisTime

/** One row of the time axis (a school period). */
data class Slot(val start: UntisTime, val end: UntisTime) {
    val minutes: Int get() = end.toMinutesOfDay() - start.toMinutesOfDay()

    fun overlaps(other: Slot): Boolean = start < other.end && other.start < end
}

/**
 * Periods for the left time column. Uses the school's time grid and adds slots for lessons that
 * fall outside of it. Without a time grid, the lesson times themselves become the rows.
 */
fun buildSlots(grid: List<TimegridDay>, lessons: List<Lesson>): List<Slot> {
    val slots = mutableListOf<Slot>()
    fun add(candidate: Slot) {
        if (candidate.minutes <= 0) return
        if (slots.none { it.overlaps(candidate) }) slots += candidate
    }
    grid.flatMap { it.units }
        .map { Slot(it.start, it.end) }
        .distinct()
        .sortedWith(compareBy({ it.start.raw }, { -it.minutes }))
        .forEach(::add)
    val first = slots.minOfOrNull { it.start.raw }
    val last = slots.maxOfOrNull { it.end.raw }
    lessons
        .filter { first == null || last == null || it.end.raw <= first || it.start.raw >= last }
        .map { Slot(it.start, it.end) }
        .distinct()
        .sortedWith(compareBy({ it.start.raw }, { it.minutes }))
        .forEach(::add)
    return slots.sortedBy { it.start.raw }
}

/** Maps times to vertical positions (in dp) for a list of slots. Breaks get a smaller, capped gap. */
class TimeAxis(val slots: List<Slot>) {
    private val tops = FloatArray(slots.size)
    private val heights = FloatArray(slots.size)
    val totalHeight: Float

    init {
        var y = TOP_PADDING
        slots.forEachIndexed { index, slot ->
            if (index > 0) {
                val gap = slot.start.toMinutesOfDay() - slots[index - 1].end.toMinutesOfDay()
                if (gap > 0) y += (gap * GAP_DP_PER_MIN).coerceIn(MIN_GAP, MAX_GAP)
            }
            tops[index] = y
            heights[index] = (slot.minutes * DP_PER_MIN).coerceAtLeast(MIN_SLOT)
            y += heights[index]
        }
        totalHeight = y + BOTTOM_PADDING
    }

    fun top(index: Int): Float = tops[index]
    fun bottom(index: Int): Float = tops[index] + heights[index]

    fun y(time: UntisTime): Float {
        if (slots.isEmpty()) return TOP_PADDING
        val t = time.toMinutesOfDay()
        slots.forEachIndexed { index, slot ->
            val start = slot.start.toMinutesOfDay()
            val end = slot.end.toMinutesOfDay()
            if (t < start) {
                if (index == 0) return tops[0]
                // Inside the break before this slot.
                val prevEnd = slots[index - 1].end.toMinutesOfDay()
                val fraction = (t - prevEnd).toFloat() / (start - prevEnd).coerceAtLeast(1)
                return bottom(index - 1) + fraction.coerceIn(0f, 1f) * (tops[index] - bottom(index - 1))
            }
            if (t <= end) {
                return tops[index] + (t - start).toFloat() / slot.minutes.coerceAtLeast(1) * heights[index]
            }
        }
        return bottom(slots.lastIndex)
    }

    fun contains(time: UntisTime): Boolean =
        slots.isNotEmpty() && time >= slots.first().start && time <= slots.last().end

    companion object {
        const val DP_PER_MIN = 1.4f
        const val MIN_SLOT = 56f
        const val GAP_DP_PER_MIN = 1.1f
        const val MIN_GAP = 6f
        const val MAX_GAP = 26f
        const val TOP_PADDING = 8f
        const val BOTTOM_PADDING = 24f
    }
}

/** A block with its horizontal lane, for lessons that run in parallel. */
data class PlacedBlock(val block: LessonBlock, val lane: Int, val lanes: Int)

fun placeLanes(blocks: List<LessonBlock>): List<PlacedBlock> {
    val sorted = blocks.sortedWith(compareBy({ it.start.raw }, { -it.end.raw }))
    val result = mutableListOf<PlacedBlock>()
    var cluster = mutableListOf<Pair<LessonBlock, Int>>()
    var clusterEnd = -1
    fun flush() {
        val lanes = (cluster.maxOfOrNull { it.second } ?: -1) + 1
        cluster.forEach { (block, lane) -> result += PlacedBlock(block, lane, lanes) }
        cluster = mutableListOf()
    }
    for (block in sorted) {
        if (cluster.isNotEmpty() && block.start.raw >= clusterEnd) flush()
        val laneEnds = mutableMapOf<Int, Int>()
        cluster.forEach { (b, lane) -> laneEnds[lane] = maxOf(laneEnds[lane] ?: -1, b.end.raw) }
        val lane = generateSequence(0) { it + 1 }.first { (laneEnds[it] ?: -1) <= block.start.raw }
        cluster += block to lane
        clusterEnd = maxOf(if (cluster.size == 1) -1 else clusterEnd, block.end.raw)
    }
    if (cluster.isNotEmpty()) flush()
    return result
}
