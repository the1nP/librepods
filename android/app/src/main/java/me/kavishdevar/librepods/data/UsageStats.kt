/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.core.content.edit
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class UsageStats(
    val totalWornMs: Long = 0L,
    val sessionCount: Int = 0,
    val longestSessionMs: Long = 0L,
    val firstWornAt: Long? = null,
    val lastWornAt: Long? = null,
    // local epoch day -> worn milliseconds on that day
    val dailyWornMs: Map<Long, Long> = emptyMap(),
    val currentSessionStart: Long? = null,
) {
    val averageSessionMs: Long
        get() = if (sessionCount == 0) 0L else totalWornMs / sessionCount

    val daysWorn: Int
        get() = dailyWornMs.count { it.value > 0 }

    fun wornOn(day: LocalDate): Long = dailyWornMs[day.toEpochDay()] ?: 0L

    fun wornBetween(start: LocalDate, end: LocalDate): Long =
        dailyWornMs.filterKeys { it in start.toEpochDay()..end.toEpochDay() }.values.sum()

    // Includes the session that is still in progress, so the screen can show live numbers.
    fun withOpenSession(now: Long = System.currentTimeMillis()): UsageStats {
        val start = currentSessionStart ?: return this
        return addSession(start, now).copy(currentSessionStart = start)
    }

    internal fun addSession(start: Long, end: Long): UsageStats {
        val duration = end - start
        if (duration <= 0) return this
        val daily = dailyWornMs.toMutableMap()
        splitByDay(start, end).forEach { (day, ms) -> daily[day] = (daily[day] ?: 0L) + ms }
        return copy(
            totalWornMs = totalWornMs + duration,
            sessionCount = sessionCount + 1,
            longestSessionMs = maxOf(longestSessionMs, duration),
            firstWornAt = firstWornAt ?: start,
            lastWornAt = end,
            dailyWornMs = daily,
            currentSessionStart = null,
        )
    }

    private fun splitByDay(start: Long, end: Long): List<Pair<Long, Long>> {
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<Pair<Long, Long>>()
        var cursor = start
        while (cursor < end) {
            val day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val nextMidnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sliceEnd = minOf(end, nextMidnight)
            result += day.toEpochDay() to (sliceEnd - cursor)
            cursor = sliceEnd
        }
        return result
    }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("totalWornMs", totalWornMs)
        put("sessionCount", sessionCount)
        put("longestSessionMs", longestSessionMs)
        firstWornAt?.let { put("firstWornAt", it) }
        lastWornAt?.let { put("lastWornAt", it) }
        put("daily", JSONObject().apply {
            dailyWornMs.forEach { (day, ms) -> put(day.toString(), ms) }
        })
    }

    internal companion object {
        fun fromJson(json: JSONObject): UsageStats {
            val daily = json.optJSONObject("daily")
            return UsageStats(
                totalWornMs = json.optLong("totalWornMs"),
                sessionCount = json.optInt("sessionCount"),
                longestSessionMs = json.optLong("longestSessionMs"),
                firstWornAt = if (json.has("firstWornAt")) json.getLong("firstWornAt") else null,
                lastWornAt = if (json.has("lastWornAt")) json.getLong("lastWornAt") else null,
                dailyWornMs = daily?.keys()?.asSequence()
                    ?.associate { it.toLong() to daily.getLong(it) }
                    ?: emptyMap(),
            )
        }
    }
}

/**
 * Records in-ear sessions locally. A session starts when at least one bud goes in
 * and ends when both are out, or when the AirPods disconnect.
 */
object UsageTracker {
    private const val TAG = "UsageTracker"
    private const val PREFS = "usage_stats"
    private const val MIN_SESSION_MS = 3_000L

    private var sessionAddress: String? = null
    private var sessionStart: Long? = null

    @Synchronized
    fun onWearStateChanged(context: Context, address: String?, worn: Boolean) {
        val now = System.currentTimeMillis()
        if (worn) {
            if (sessionStart != null && sessionAddress == address) return
            endSession(context, now)
            if (address == null) return
            sessionAddress = address
            sessionStart = now
        } else {
            endSession(context, now)
        }
    }

    @Synchronized
    fun endSession(context: Context, now: Long = System.currentTimeMillis()) {
        val start = sessionStart ?: return
        val address = sessionAddress
        sessionStart = null
        sessionAddress = null
        if (address == null || now - start < MIN_SESSION_MS) return
        val updated = read(context, address).addSession(start, now)
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit {
            putString(key(address), updated.toJson().toString())
        }
    }

    @Synchronized
    fun load(context: Context, address: String?): UsageStats {
        if (address == null) return UsageStats()
        val stored = read(context, address)
        return if (sessionAddress == address) stored.copy(currentSessionStart = sessionStart) else stored
    }

    @Synchronized
    fun reset(context: Context, address: String?) {
        if (address == null) return
        if (sessionAddress == address) sessionStart = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit { remove(key(address)) }
    }

    private fun read(context: Context, address: String): UsageStats {
        val raw = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(key(address), null)
            ?: return UsageStats()
        return try {
            UsageStats.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            Log.w(TAG, "Discarding unreadable usage stats", e)
            UsageStats()
        }
    }

    private fun key(address: String) = "stats_${address.uppercase()}"
}

fun demoUsageStats(now: Long = System.currentTimeMillis()): UsageStats {
    val today = LocalDate.now()
    val minute = 60_000L
    val daily = (0L until 144L).associate { offset ->
        today.minusDays(offset).toEpochDay() to (2 * 60 + (offset * 37) % 300) * minute
    }
    return UsageStats(
        totalWornMs = daily.values.sum(),
        sessionCount = 3564,
        longestSessionMs = 200 * minute,
        firstWornAt = now - 143 * 86_400_000L,
        lastWornAt = now,
        dailyWornMs = daily,
    )
}
