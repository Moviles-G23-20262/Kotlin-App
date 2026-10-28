package com.campusswap.app.data.local

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageDiskCacheTest {
    @get:Rule val tmp = TemporaryFolder()

    private var clock = 1_000_000L
    private fun cache(maxBytes: Long = 1_000) =
        ImageDiskCache(tmp.root.resolve("images"), maxBytes) { clock.also { clock += 1_000 } }

    private fun bytes(size: Int, fill: Int = 1) = ByteArray(size) { fill.toByte() }

    @Test fun storedImageSurvivesANewInstanceLikeAnAppRestart() {
        cache().put("https://cdn/a.jpg", bytes(10, 7))

        assertArrayEquals(bytes(10, 7), cache().get("https://cdn/a.jpg"))
        assertNull(cache().get("https://cdn/never-downloaded.jpg"))
    }

    @Test fun overTheLimitTheLeastRecentlyUsedImageIsDropped() {
        val cache = cache(maxBytes = 300)
        cache.put("a", bytes(100))
        cache.put("b", bytes(100))
        cache.put("c", bytes(100))
        cache.get("a") // "a" se vuelve a usar, ahora "b" es la mas vieja

        cache.put("d", bytes(100))

        assertNotNull(cache.get("a"))
        assertNull(cache.get("b"))
        assertNotNull(cache.get("c"))
        assertNotNull(cache.get("d"))
        assertEquals(300L, cache.sizeBytes())
    }

    @Test fun anImageBiggerThanTheWholeCacheIsNotStored() {
        val cache = cache(maxBytes = 100)
        cache.put("small", bytes(50))

        cache.put("huge", bytes(101))

        assertNull(cache.get("huge"))
        assertNotNull(cache.get("small"))
    }

    @Test fun rewritingAnImageReplacesItAndLeavesNoTemporaryFiles() {
        val cache = cache()
        cache.put("a", bytes(10, 1))
        cache.put("a", bytes(20, 2))

        assertArrayEquals(bytes(20, 2), cache.get("a"))
        assertEquals(20L, cache.sizeBytes())
        assertEquals(1, tmp.root.resolve("images").listFiles()!!.size)
    }
}
