package com.campusswap.app.data.materials

import com.campusswap.app.data.local.CachedMaterialDao
import com.campusswap.app.data.local.CachedMaterialEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MaterialRepositoryTest {
    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private class FakeCache : CachedMaterialDao {
        var rows = listOf<CachedMaterialEntity>()
        override suspend fun getAll() = rows.sortedBy { it.position }
        override suspend fun insertAll(items: List<CachedMaterialEntity>) { rows = rows + items }
        override suspend fun clear() { rows = emptyList() }
    }

    private class FakeRemote(var outcome: () -> List<MaterialDto>) : MaterialRemoteDataSource {
        override suspend fun getMaterials() = outcome()
        override suspend fun create(request: CreateMaterialRequest) = error("not used")
    }

    private fun dto(id: String, status: String = "AVAILABLE") = MaterialDto(
        id = id, title = "Book $id", description = "d", courseCode = "MATE1203", price = "45000",
        condition = "GOOD", status = status, category = "BOOKS", sellerId = "s1",
        seller = MaterialSellerDto("s1", "ana@uniandes.edu.co", "Ana Ruiz", 4.5),
    )

    @Test fun liveLoadReplacesTheCacheWithAvailableListings() = runTest {
        val cache = FakeCache().apply { rows = listOf(dto("old").toCachedEntity(0, 1L)) }
        val repository = MaterialRepository(FakeRemote { listOf(dto("a"), dto("sold", "SOLD"), dto("b")) }, cache, clock)

        val feed = repository.load()

        assertTrue(feed.isLive)
        assertEquals(listOf("a", "b"), feed.products.map { it.id })
        assertEquals(listOf("a", "b"), cache.rows.map { it.id })
        assertEquals(now.toEpochMilli(), feed.updatedAtMillis)
    }

    @Test fun offlineLoadServesTheCacheInTheSameOrder() = runTest {
        val cache = FakeCache()
        val remote = FakeRemote { listOf(dto("a"), dto("b")) }
        val repository = MaterialRepository(remote, cache, clock)
        repository.load()

        remote.outcome = { throw IOException("airplane mode") }
        val feed = repository.load()

        assertFalse(feed.isLive)
        assertEquals(listOf("a", "b"), feed.products.map { it.id })
        assertEquals("Ana Ruiz", feed.products.first().seller.name)
        assertEquals(now.toEpochMilli(), feed.updatedAtMillis)
    }

    @Test fun offlineWithNothingCachedIsEmpty() = runTest {
        val feed = MaterialRepository(FakeRemote { throw IOException() }, FakeCache(), clock).load()

        assertFalse(feed.isLive)
        assertTrue(feed.products.isEmpty())
        assertNull(feed.updatedAtMillis)
    }
}
