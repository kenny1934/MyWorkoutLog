package com.kennychiu.myworkoutlog.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Serialized as the `videoMarks` column on [com.kennychiu.myworkoutlog.data.LoggedSet]
 * (nested inside the JSON blob that backs `logged_workout_table.loggedExercises`).
 * Stores scrubber pip positions captured during form review: optional hold start/end
 * timestamps in ms, plus the list of tap-along rep timestamps.
 */
data class VideoMarks(
    val holdStart: Long? = null,
    val holdEnd: Long? = null,
    val reps: List<Long> = emptyList(),
) {
    fun isEmpty(): Boolean = holdStart == null && holdEnd == null && reps.isEmpty()

    companion object {
        private val gson = Gson()

        fun parse(json: String?): VideoMarks? {
            if (json.isNullOrBlank()) return null
            return try {
                gson.fromJson(json, VideoMarks::class.java)?.takeUnless { it.isEmpty() }
            } catch (e: JsonSyntaxException) {
                null
            }
        }

        fun serialize(marks: VideoMarks?): String? {
            if (marks == null || marks.isEmpty()) return null
            return gson.toJson(marks)
        }
    }
}
