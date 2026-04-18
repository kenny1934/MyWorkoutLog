package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.ProgramSessionDefinition
import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CycleProgressInfo(
    val orderedWeeks: List<ProgramWeekDefinition>,
    val currentWeek: ProgramWeekDefinition?,
    val currentWeekIndex: Int?,
    val nextSession: ProgramSessionDefinition?,
    val completedSessionCount: Int,
    val totalSessionCount: Int,
    val isComplete: Boolean,
    val startDate: LocalDate?,
    val plannedEndDate: LocalDate?
)

private val ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun cycleProgress(cycle: ActiveProgramCycle): CycleProgressInfo {
    val orderedWeeks = cycle.cycleProgram.weeks.sortedBy { it.order }
    val completed = cycle.completedSessions

    val currentWeekWithIndex = orderedWeeks.withIndex().firstOrNull { (_, week) ->
        week.sessions.any { session -> !completed.containsKey("${week.id}_${session.id}") }
    }
    val currentWeek = currentWeekWithIndex?.value
    val currentWeekIndex = currentWeekWithIndex?.index

    val nextSession = currentWeek?.sessions?.sortedBy { it.order }?.firstOrNull { session ->
        !completed.containsKey("${currentWeek.id}_${session.id}")
    }

    val totalSessionCount = orderedWeeks.sumOf { it.sessions.size }
    val completedSessionCount = orderedWeeks.sumOf { week ->
        week.sessions.count { session -> completed.containsKey("${week.id}_${session.id}") }
    }

    val startDate = runCatching { LocalDate.parse(cycle.startDate, ISO_DATE) }.getOrNull()
    val plannedEndDate = startDate?.plusWeeks(orderedWeeks.size.toLong())

    return CycleProgressInfo(
        orderedWeeks = orderedWeeks,
        currentWeek = currentWeek,
        currentWeekIndex = currentWeekIndex,
        nextSession = nextSession,
        completedSessionCount = completedSessionCount,
        totalSessionCount = totalSessionCount,
        isComplete = currentWeek == null && totalSessionCount > 0,
        startDate = startDate,
        plannedEndDate = plannedEndDate
    )
}
