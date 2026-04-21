package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition

enum class CycleWeekPhase(val label: String) {
    Baseline("Baseline"),
    Accumulation("Accumulation"),
    Intensity("Intensity"),
    Peak("Peak"),
    Deload("Deload"),
    MidBlock("Mid-block")
}

private val FIRST_INT_REGEX = Regex("""\d+""")

private fun parseTargetRir(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    return FIRST_INT_REGEX.find(raw)?.value?.toIntOrNull()
}

fun classifyCycleWeek(week: ProgramWeekDefinition, cycle: ActiveProgramCycle): CycleWeekPhase {
    if (week.isDeloadWeek) return CycleWeekPhase.Deload

    val orderedWeeks = cycle.cycleProgram.weeks.sortedBy { it.order }
    val parsedByOrder = orderedWeeks.map { parseTargetRir(it.targetRir) }
    val weekValue = parseTargetRir(week.targetRir)

    val allParsed = parsedByOrder.size >= 2 && parsedByOrder.all { it != null }
    if (allParsed && weekValue != null) {
        val values = parsedByOrder.filterNotNull()
        val monoDecreasing = values.zipWithNext().all { (a, b) -> b <= a } &&
            values.first() > values.last()
        if (monoDecreasing) {
            val min = values.min()
            val max = values.max()
            val midpoint = (min + max) / 2.0
            return when {
                weekValue == min -> CycleWeekPhase.Peak
                weekValue > midpoint -> CycleWeekPhase.Accumulation
                else -> CycleWeekPhase.Intensity
            }
        }
    }

    val idx = orderedWeeks.indexOfFirst { it.id == week.id }
    val last = orderedWeeks.size - 1
    return when {
        orderedWeeks.size <= 1 -> CycleWeekPhase.Baseline
        idx == 0 -> CycleWeekPhase.Baseline
        idx == last -> CycleWeekPhase.Peak
        else -> CycleWeekPhase.MidBlock
    }
}
