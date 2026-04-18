package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition
import java.util.UUID

fun duplicateWeekInto(
    weeks: List<ProgramWeekDefinition>,
    source: ProgramWeekDefinition,
    idGenerator: () -> String = { UUID.randomUUID().toString() }
): List<ProgramWeekDefinition> {
    val idx = weeks.indexOfFirst { it.id == source.id }
    if (idx < 0) return weeks
    val copy = source.copy(
        id = idGenerator(),
        weekLabel = "Copy of ${source.weekLabel}",
        sessions = source.sessions.map { it.copy(id = idGenerator()) }
    )
    val inserted = weeks.toMutableList().apply { add(idx + 1, copy) }
    return inserted.mapIndexed { i, w -> w.copy(order = i + 1) }
}

fun moveWeek(
    weeks: List<ProgramWeekDefinition>,
    fromIndex: Int,
    toIndex: Int
): List<ProgramWeekDefinition> {
    if (fromIndex == toIndex) return weeks
    if (fromIndex !in weeks.indices) return weeks
    if (toIndex !in weeks.indices) return weeks
    val reordered = weeks.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
    return reordered.mapIndexed { i, w -> w.copy(order = i + 1) }
}

fun moveSessionWithinWeek(
    weeks: List<ProgramWeekDefinition>,
    weekId: String,
    fromIndex: Int,
    toIndex: Int
): List<ProgramWeekDefinition> {
    if (fromIndex == toIndex) return weeks
    val week = weeks.firstOrNull { it.id == weekId } ?: return weeks
    val sorted = week.sessions.sortedBy { it.order }
    if (fromIndex !in sorted.indices) return weeks
    if (toIndex !in sorted.indices) return weeks
    val reordered = sorted.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    val renumbered = reordered.mapIndexed { i, s -> s.copy(order = i + 1) }
    return weeks.map { w -> if (w.id == weekId) w.copy(sessions = renumbered) else w }
}

fun moveSessionToWeek(
    weeks: List<ProgramWeekDefinition>,
    fromWeekId: String,
    sessionId: String,
    toWeekId: String
): List<ProgramWeekDefinition> {
    if (fromWeekId == toWeekId) return weeks
    val fromWeek = weeks.firstOrNull { it.id == fromWeekId } ?: return weeks
    val toWeek = weeks.firstOrNull { it.id == toWeekId } ?: return weeks
    val session = fromWeek.sessions.firstOrNull { it.id == sessionId } ?: return weeks

    val remainingFrom = fromWeek.sessions
        .filter { it.id != sessionId }
        .mapIndexed { i, s -> s.copy(order = i + 1) }
    val appendedTo = (toWeek.sessions + session.copy(order = toWeek.sessions.size + 1))

    return weeks.map { w ->
        when (w.id) {
            fromWeekId -> w.copy(sessions = remainingFrom)
            toWeekId -> w.copy(sessions = appendedTo)
            else -> w
        }
    }
}
