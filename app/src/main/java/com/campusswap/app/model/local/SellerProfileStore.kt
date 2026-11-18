package com.campusswap.app.model.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * Last server answer for a seller profile, so it can be opened again without a connection.
 * It keeps the raw JSON of both endpoints; the summary (average, stars, tags) is rebuilt from it.
 */
@Entity(tableName = "seller_profiles")
data class SellerProfileEntity(
    @PrimaryKey val sellerId: String,
    val ratingsJson: String,
    val listingsJson: String,
    val savedAtMillis: Long,
)

@Dao
interface SellerProfileDao {
    @Query("SELECT * FROM seller_profiles WHERE sellerId = :sellerId")
    suspend fun get(sellerId: String): SellerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(profile: SellerProfileEntity)

    /** Deletes all but the [keep] profiles saved most recently. */
    @Query(
        "DELETE FROM seller_profiles WHERE sellerId NOT IN " +
            "(SELECT sellerId FROM seller_profiles ORDER BY savedAtMillis DESC LIMIT :keep)",
    )
    suspend fun keepNewest(keep: Int)

    /** Saves the profile and trims the table in one transaction, so it never grows without limit. */
    @Transaction
    suspend fun save(profile: SellerProfileEntity, keep: Int) {
        put(profile)
        keepNewest(keep)
    }
}