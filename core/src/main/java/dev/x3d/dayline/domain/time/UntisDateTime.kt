package dev.x3d.dayline.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@JvmInline
value class UntisDate(val yyyymmdd: Int) : Comparable<UntisDate> {
    val year: Int get() = yyyymmdd / 10_000
    val month: Int get() = (yyyymmdd / 100) % 100
    val day: Int get() = yyyymmdd % 100

    fun toLocalDate(): LocalDate = LocalDate.of(year, month, day)

    fun plusDays(days: Long): UntisDate = from(toLocalDate().plusDays(days))

    override fun compareTo(other: UntisDate): Int = yyyymmdd.compareTo(other.yyyymmdd)

    fun formatDisplay(): String = toLocalDate().format(DAY_MONTH)

    fun formatWeekday(): String = toLocalDate().format(WEEKDAY)

    companion object {
        private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
        private val WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

        fun from(date: LocalDate): UntisDate =
            UntisDate(date.year * 10_000 + date.monthValue * 100 + date.dayOfMonth)

        fun today(): UntisDate = from(LocalDate.now())

        fun weekRange(anchor: LocalDate = LocalDate.now()): Pair<UntisDate, UntisDate> {
            val monday = anchor.with(DayOfWeek.MONDAY)
            val sunday = monday.plusDays(6)
            return from(monday) to from(sunday)
        }
    }
}

@JvmInline
value class UntisTime(val raw: Int) : Comparable<UntisTime> {
    val hour: Int get() = raw / 100
    val minute: Int get() = raw % 100

    fun toLocalTime(): LocalTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun toMinutesOfDay(): Int = hour * 60 + minute

    fun format(): String = String.format(Locale.US, "%02d:%02d", hour, minute)

    override fun compareTo(other: UntisTime): Int = raw.compareTo(other.raw)

    companion object {
        fun fromLocalTime(time: LocalTime): UntisTime =
            UntisTime(time.hour * 100 + time.minute)

        fun now(): UntisTime = fromLocalTime(LocalTime.now())

        fun parse(raw: Int): UntisTime = UntisTime(raw)
    }
}

fun minutesUntil(start: UntisTime, now: UntisTime = UntisTime.now()): Int =
    start.toMinutesOfDay() - now.toMinutesOfDay()
