package org.freedomsuite.calendar.ui

import org.freedomsuite.calendar.data.EventEntity
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

enum class AgendaFilter {
    UPCOMING,
    PAST,
}

sealed class AgendaRow {
    data class Header(val label: String) : AgendaRow()
    data class Event(val event: EventEntity) : AgendaRow()
}

object AgendaGrouper {
    fun buildAgenda(events: List<EventEntity>, filter: AgendaFilter, nowMs: Long = System.currentTimeMillis()): List<AgendaRow> {
        val filtered = when (filter) {
            AgendaFilter.UPCOMING -> events.filter { it.endEpochMs >= nowMs }
                .sortedBy { it.startEpochMs }
            AgendaFilter.PAST -> events.filter { it.endEpochMs < nowMs }
                .sortedByDescending { it.startEpochMs }
        }
        if (filtered.isEmpty()) return emptyList()

        val dayFormat = DateFormat.getDateInstance(DateFormat.FULL)
        val today = startOfDay(nowMs)
        val tomorrow = today + DAY_MS

        val grouped = linkedMapOf<String, MutableList<EventEntity>>()
        for (event in filtered) {
            val dayStart = startOfDay(event.startEpochMs)
            val label = when (dayStart) {
                today -> "Today"
                tomorrow -> "Tomorrow"
                else -> dayFormat.format(Date(dayStart))
            }
            grouped.getOrPut(label) { mutableListOf() } += event
        }

        return buildList {
            grouped.forEach { (label, dayEvents) ->
                add(AgendaRow.Header(label))
                dayEvents.forEach { add(AgendaRow.Event(it)) }
            }
        }
    }

    fun formatEventTime(event: EventEntity): String {
        if (event.isAllDay) {
            val day = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(event.startEpochMs))
            return "All day · $day"
        }
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val start = formatter.format(Date(event.startEpochMs))
        val endTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.endEpochMs))
        return "$start – $endTime"
    }

    fun startOfDay(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(epochMs: Long): Long =
        startOfDay(epochMs) + DAY_MS - 1

    fun normalizeAllDayRange(startEpochMs: Long, endEpochMs: Long): Pair<Long, Long> {
        val start = startOfDay(startEpochMs)
        val end = maxOf(endOfDay(endEpochMs), start + DAY_MS - 1)
        return start to end
    }

    private const val DAY_MS = 86_400_000L
}
