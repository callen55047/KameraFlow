package com.egan.core.tools

import io.ktor.util.date.getTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ImageProfiler {
    private val durations = mutableListOf<Long>()
    private val startTimes = mutableMapOf<String, Long>()

    /**
     * Call this when a new image is first received.
     * @param id A unique identifier for the image (e.g., UUID or timestamp).
     */

    fun markStart(): String {
        // TODO: create expect actual for timing
        val id = Uuid.random().toString()
        startTimes[id] = getTimeMillis()
        return id
    }

    /**
     * Call this when the image is finished being processed.
     * @param id The same identifier used in [markStart].
     */
    fun markEnd(id: String) {
        val startTime = startTimes.remove(id)
        if (startTime != null) {
            val durationNs = getTimeMillis() - startTime
            durations.add(durationNs)
            printAverage()
        } else {
            println("[ImageProfiler] Warning: markEnd called without matching markStart for id=$id")
        }
    }

    private fun printAverage() {
        val averageMs = durations.average()
        println("[ImageProfiler] Average processing time: $averageMs ms over ${durations.size} images")
    }
}