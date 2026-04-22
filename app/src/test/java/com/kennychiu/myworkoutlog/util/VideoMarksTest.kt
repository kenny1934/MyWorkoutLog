package com.kennychiu.myworkoutlog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMarksTest {

    @Test
    fun `serialize null returns null`() {
        assertNull(VideoMarks.serialize(null))
    }

    @Test
    fun `serialize empty marks returns null`() {
        assertNull(VideoMarks.serialize(VideoMarks()))
        assertNull(VideoMarks.serialize(VideoMarks(holdStart = null, holdEnd = null, reps = emptyList())))
    }

    @Test
    fun `round trip preserves hold and rep timestamps`() {
        val original = VideoMarks(
            holdStart = 1200L,
            holdEnd = 4800L,
            reps = listOf(800L, 1600L, 2400L, 3200L)
        )
        val json = VideoMarks.serialize(original)
        assertTrue(json != null && json.isNotBlank())
        val parsed = VideoMarks.parse(json)
        assertEquals(original, parsed)
    }

    @Test
    fun `round trip with only reps`() {
        val original = VideoMarks(reps = listOf(500L, 1000L))
        val parsed = VideoMarks.parse(VideoMarks.serialize(original))
        assertEquals(original, parsed)
    }

    @Test
    fun `round trip with only hold`() {
        val original = VideoMarks(holdStart = 2000L, holdEnd = 5000L)
        val parsed = VideoMarks.parse(VideoMarks.serialize(original))
        assertEquals(original, parsed)
    }

    @Test
    fun `parse null or blank returns null`() {
        assertNull(VideoMarks.parse(null))
        assertNull(VideoMarks.parse(""))
        assertNull(VideoMarks.parse("   "))
    }

    @Test
    fun `parse malformed JSON returns null`() {
        assertNull(VideoMarks.parse("not-json"))
        assertNull(VideoMarks.parse("{holdStart:"))
    }

    @Test
    fun `parse JSON with only reps deserializes with default hold nulls`() {
        val parsed = VideoMarks.parse("""{"reps":[100,200,300]}""")
        assertEquals(VideoMarks(holdStart = null, holdEnd = null, reps = listOf(100L, 200L, 300L)), parsed)
    }

    @Test
    fun `parse JSON with no marks returns null via emptiness check`() {
        assertNull(VideoMarks.parse("""{}"""))
        assertNull(VideoMarks.parse("""{"reps":[]}"""))
    }
}
