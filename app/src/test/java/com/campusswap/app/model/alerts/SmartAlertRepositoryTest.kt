package com.campusswap.app.model.alerts

import com.campusswap.app.model.Category
import com.campusswap.app.model.Condition
import com.campusswap.app.model.SmartAlert
import com.campusswap.app.model.local.InMemoryResponseCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SmartAlertRepositoryTest {

    private class FakeRemote : SmartAlertRemoteDataSource {
        val stored = mutableListOf<SmartAlertDto>()
        var failure: Exception? = null
        val requests = mutableListOf<CreateSmartAlertRequest>()

        override suspend fun getAll(): List<SmartAlertDto> {
            failure?.let { throw it }
            return stored.toList()
        }

        override suspend fun create(request: CreateSmartAlertRequest): SmartAlertDto {
            failure?.let { throw it }
            requests += request
            return SmartAlertDto("srv-${requests.size}", request.keyword, request.courseCode, request.category,
                "${request.maxPrice}.00", request.minCondition, request.enabled).also { stored += it }
        }

        override suspend fun setEnabled(id: String, enabled: Boolean): SmartAlertDto {
            failure?.let { throw it }
            return stored.first { it.id == id }.copy(enabled = enabled)
        }

        override suspend fun remove(id: String) {
            failure?.let { throw it }
            stored.removeAll { it.id == id }
        }
    }

    private val remote = FakeRemote()
    private val repository = SmartAlertRepository(remote, currentUserId = { "me" }, cache = InMemoryResponseCache())

    private fun alert(category: Category = Category.CALCULATORS, condition: Condition = Condition.GOOD) =
        SmartAlert("local-1", " casio ", course = null, category = category, maxPrice = 80_000.0, minCondition = condition)

    @Test fun anAlertIsSavedWithServerEnumsAndComesBackWithTheServerId() = runTest {
        val saved = repository.create(alert())!!

        val request = remote.requests.single()
        assertEquals("casio", request.keyword)
        assertEquals("CALCULATORS", request.category)
        assertEquals("80000", request.maxPrice)
        assertEquals("GOOD", request.minCondition)
        assertEquals("srv-1", saved.id)
        assertEquals(80_000.0, saved.maxPrice, 0.0)
        assertEquals(Category.CALCULATORS, saved.category)
    }

    @Test fun anyCategoryAndAnyConditionTravelAsNull() = runTest {
        val saved = repository.create(alert(Category.ALL, Condition.FAIR))!!

        assertNull(remote.requests.single().category)
        assertNull(remote.requests.single().minCondition)
        assertEquals(Category.ALL, saved.category)
        assertEquals(Condition.FAIR, saved.minCondition)
    }

    @Test fun offlineTheAlertsSavedByTheLastLoadAreShown() = runTest {
        repository.create(alert())
        assertTrue(repository.load()!!.isLive)

        remote.failure = IOException("airplane mode")
        val saved = repository.load()!!

        assertFalse(saved.isLive)
        assertEquals(listOf("srv-1"), saved.alerts.map { it.id })
    }

    @Test fun withoutAConnectionChangesReportFailureInsteadOfThrowing() = runTest {
        remote.failure = IOException("airplane mode")

        assertNull(repository.load())
        assertNull(repository.create(alert()))
        assertFalse(repository.setEnabled("srv-1", false))
        assertFalse(repository.remove("srv-1"))
    }
}
