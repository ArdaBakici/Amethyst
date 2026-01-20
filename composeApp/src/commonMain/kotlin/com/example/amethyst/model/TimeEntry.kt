package com.example.amethyst.model

import kotlinx.datetime.Instant

data class TimeEntry(
    val startTime: Instant,
    val endTime: Instant? = null
) {
    val duration: Long?
        get() = endTime?.let { (it.toEpochMilliseconds() - startTime.toEpochMilliseconds()) / 1000 }

    fun toMap(): Map<String, String> {
        val map = mutableMapOf("startTime" to startTime.toString())
        endTime?.let { map["endTime"] = it.toString() }
        return map
    }

    companion object {
        fun fromMap(map: Map<String, Any>): TimeEntry? {
            val startTime = (map["startTime"] as? String)?.let { Instant.parse(it) } ?: return null
            val endTime = (map["endTime"] as? String)?.let { Instant.parse(it) }
            return TimeEntry(startTime, endTime)
        }
    }
}
