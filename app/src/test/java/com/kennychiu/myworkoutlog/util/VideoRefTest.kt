package com.kennychiu.myworkoutlog.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRefTest {

    @Test
    fun `http URLs are recognized as remote`() {
        assertTrue(isRemoteVideoLink("http://example.com/video.mp4"))
        assertTrue(isRemoteVideoLink("https://youtube.com/watch?v=abc"))
    }

    @Test
    fun `mixed-case http scheme is still remote`() {
        assertTrue(isRemoteVideoLink("HTTPS://YOUTUBE.COM/watch"))
        assertTrue(isRemoteVideoLink("Http://example.com"))
    }

    @Test
    fun `leading and trailing whitespace does not break detection`() {
        assertTrue(isRemoteVideoLink("   https://youtube.com/watch  "))
    }

    @Test
    fun `content URIs are not remote`() {
        assertFalse(isRemoteVideoLink("content://media/external/video/media/42"))
    }

    @Test
    fun `file URIs are not remote`() {
        assertFalse(isRemoteVideoLink("file:///sdcard/video.mp4"))
    }

    @Test
    fun `empty string is not remote`() {
        assertFalse(isRemoteVideoLink(""))
        assertFalse(isRemoteVideoLink("   "))
    }

    @Test
    fun `bare domain without scheme is not treated as remote`() {
        // Intentional — we show a link icon only when we can open via ACTION_VIEW,
        // which requires an explicit http(s) scheme. Bare domains fall through to
        // the content-URI path and will fail to open, surfacing the user error.
        assertFalse(isRemoteVideoLink("youtube.com/watch?v=abc"))
    }
}
