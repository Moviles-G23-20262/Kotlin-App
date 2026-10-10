package com.campusswap.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Last listings fetched from the server, so Home still has content without a connection. */
@Entity(tableName = "cached_materials")
data class CachedMaterialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val courseCode: String?,
    val price: String,
    val condition: String?,
    val status: String,
    val category: String,
    val sellerId: String,
    val sellerName: String?,
    val sellerEmail: String?,
    val sellerRating: Double?,
    /** Position in the server response, so the offline feed keeps the same order. */
    val position: Int,
    val cachedAtMillis: Long,
)

/** A listing the user published that hasn't reached the server yet. */
@Entity(tableName = "pending_listings")
data class PendingListingEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    /** Only this user's session may send it: another account logging in on the phone never does. */
    val ownerId: String,
    val title: String,
    val description: String,
    val courseCode: String?,
    val price: String,
    /** App [com.campusswap.app.data.Condition] and [com.campusswap.app.data.Category] names; mapped to server enums when sent. */
    val condition: String,
    val category: String,
    val createdAtMillis: Long,
    val attempts: Int = 0,
    /** Set when the server refused the listing; it stays visible so the user knows it wasn't published. */
    val failedReason: String? = null,
)

/** A chat message waiting to be delivered to the server. */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey val localId: String,
    val ownerId: String,
    val materialId: String,
    val content: String,
    val createdAtMillis: Long,
    val attempts: Int = 0,
    val failedReason: String? = null,
)

@Dao
interface CachedMaterialDao {
    @Query("SELECT * FROM cached_materials ORDER BY position")
    suspend fun getAll(): List<CachedMaterialEntity>

    @Insert
    suspend fun insertAll(items: List<CachedMaterialEntity>)

    @Query("DELETE FROM cached_materials")
    suspend fun clear()

    /** The cache mirrors the latest server response: listings that were sold or removed disappear. */
    @Transaction
    suspend fun replaceAll(items: List<CachedMaterialEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM pending_listings ORDER BY createdAtMillis")
    fun observeListings(): Flow<List<PendingListingEntity>>

    @Query("SELECT * FROM pending_listings WHERE ownerId = :ownerId AND failedReason IS NULL ORDER BY createdAtMillis")
    suspend fun listingsToSend(ownerId: String): List<PendingListingEntity>

    @Insert
    suspend fun insertListing(listing: PendingListingEntity): Long

    @Query("DELETE FROM pending_listings WHERE localId = :localId")
    suspend fun deleteListing(localId: Long)

    @Query("UPDATE pending_listings SET attempts = attempts + 1, failedReason = :reason WHERE localId = :localId")
    suspend fun markListingAttempt(localId: Long, reason: String?)

    @Query("SELECT * FROM pending_messages ORDER BY createdAtMillis")
    fun observeMessages(): Flow<List<PendingMessageEntity>>

    @Query("SELECT * FROM pending_messages WHERE ownerId = :ownerId AND failedReason IS NULL ORDER BY createdAtMillis")
    suspend fun messagesToSend(ownerId: String): List<PendingMessageEntity>

    @Insert
    suspend fun insertMessage(message: PendingMessageEntity)

    @Query("DELETE FROM pending_messages WHERE localId = :localId")
    suspend fun deleteMessage(localId: String)

    @Query("UPDATE pending_messages SET attempts = attempts + 1, failedReason = :reason WHERE localId = :localId")
    suspend fun markMessageAttempt(localId: String, reason: String?)
}

@Database(
    entities = [CachedMaterialEntity::class, PendingListingEntity::class, PendingMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CampusSwapDatabase : RoomDatabase() {
    abstract fun cachedMaterials(): CachedMaterialDao
    abstract fun outbox(): OutboxDao

    companion object {
        fun create(context: Context): CampusSwapDatabase =
            Room.databaseBuilder(context, CampusSwapDatabase::class.java, "campusswap.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
