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
