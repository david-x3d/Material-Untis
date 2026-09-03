package dev.x3d.dayline.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchPayload(
    val date: Int,
    val syncedAt: Long,
    val periods: List<WatchPeriod>,
)

@Serializable
data class WatchPeriod(
    val subject: String,
    val room: String,
    val teacher: String,
    val start: Int,
    val end: Int,
    val status: String,
    val info: String,
)
