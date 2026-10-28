package com.campusswap.app.model.ratings

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingRepositoryTest {
    private val material = "b0000000-0000-4000-8000-000000000001"
    private val seller = "seller"

    private val remote = object : RatingRemoteDataSource {
        val created = mutableListOf<CreateRatingRequest>()

        override suspend fun getExchanges(materialId: String) = listOf(
            ExchangeSummaryDto("with-ana", material, buyerId = "ana", sellerId = seller),
            ExchangeSummaryDto("with-luis", material, buyerId = "luis", sellerId = seller),
        )

        override suspend fun createRating(request: CreateRatingRequest): String {
            created += request
            return "r-${created.size}"
        }

        override suspend fun getRatings(exchangeId: String?, userId: String?) =
            if (exchangeId == "with-ana") listOf(RatingDto("r1", "with-ana", seller, "ana", 5, null, null, null, null)) else emptyList()
    }

    private val repository = RatingRepository(remote)

    @Test
    fun `my rating of one buyer doesn't close the exchange with another`() = runTest {
        val found = repository.exchangeRatings(material, seller, otherId = "luis")

        assertNull(found?.mine)
    }

    @Test
    fun `a rating is saved on the exchange with the person rated`() = runTest {
        repository.submit(material, ratedId = "luis", stars = 4, tags = emptyList(), review = null)

        assertEquals("with-luis", remote.created.single().exchangeId)
    }
}
