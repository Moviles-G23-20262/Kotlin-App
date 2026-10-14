package com.campusswap.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    val sellerMajor: String? = null,
    /** URLs separadas por salto de linea */
    val imageUrls: String? = null,
    val edition: String? = null,
    val model: String? = null,
    val createdAt: String? = null,
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
    val photoPaths: String? = null,
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

/**
 * Last answer the server gave for a screen (a chat thread, the inbox...), as JSON, so the screen
 * can show it without a connection. Keyed by user so another account on the phone never sees it.
 */
@Entity(tableName = "cached_responses", primaryKeys = ["ownerId", "cacheKey"])
data class CachedResponseEntity(
    val ownerId: String,
    val cacheKey: String,
    val json: String,
    val cachedAtMillis: Long,
)

/** Something the user did offline (check-in, rating, meeting proposal) that hasn't reached the server yet. */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val ownerId: String,
    /** [com.campusswap.app.data.sync.PendingActionKind] name. */
    val kind: String,
    val productId: String,
    /** The action's details as JSON; its shape depends on [kind]. */
    val payload: String,
    val createdAtMillis: Long,
    val attempts: Int = 0,
    val failedReason: String? = null,
)

@Dao
interface ResponseCacheDao {
    @Query("SELECT * FROM cached_responses WHERE ownerId = :ownerId AND cacheKey = :cacheKey")
    suspend fun get(ownerId: String, cacheKey: String): CachedResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CachedResponseEntity)
}

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

    @Query("SELECT * FROM pending_actions ORDER BY localId")
    fun observeActions(): Flow<List<PendingActionEntity>>

    /** In the order they were done: a rating must not reach the server before its check-in. */
    @Query("SELECT * FROM pending_actions WHERE ownerId = :ownerId AND failedReason IS NULL ORDER BY localId")
    suspend fun actionsToSend(ownerId: String): List<PendingActionEntity>

    @Insert
    suspend fun insertAction(action: PendingActionEntity): Long

    @Query("DELETE FROM pending_actions WHERE localId = :localId")
    suspend fun deleteAction(localId: Long)

    @Query("UPDATE pending_actions SET attempts = attempts + 1, failedReason = :reason WHERE localId = :localId")
    suspend fun markActionAttempt(localId: Long, reason: String?)
}

@Database(
    entities = [
        CachedMaterialEntity::class, PendingListingEntity::class, PendingMessageEntity::class,
        CachedResponseEntity::class, PendingActionEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class CampusSwapDatabase : RoomDatabase() {
    abstract fun cachedMaterials(): CachedMaterialDao
    abstract fun outbox(): OutboxDao
    abstract fun responses(): ResponseCacheDao

    companion object {
        // v2: columnas nuevas de GET /materials (fotos, edición, modelo, fecha y carrera del vendedor)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("sellerMajor", "imageUrls", "edition", "model", "createdAt").forEach { column ->
                    db.execSQL("ALTER TABLE cached_materials ADD COLUMN $column TEXT")
                }
            }
        }
        // v3: fotos de las publicaciones pendientes del Outbox
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_listings ADD COLUMN photoPaths TEXT")
            }
        }
        // v4: respuestas guardadas para ver chats sin conexion y acciones pendientes (check-in, calificacion, propuesta)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cached_responses (ownerId TEXT NOT NULL, cacheKey TEXT NOT NULL, " +
                        "json TEXT NOT NULL, cachedAtMillis INTEGER NOT NULL, PRIMARY KEY(ownerId, cacheKey))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pending_actions (localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "ownerId TEXT NOT NULL, kind TEXT NOT NULL, productId TEXT NOT NULL, payload TEXT NOT NULL, " +
                        "createdAtMillis INTEGER NOT NULL, attempts INTEGER NOT NULL, failedReason TEXT)",
                )
            }
        }
        fun create(context: Context): CampusSwapDatabase =
            Room.databaseBuilder(context, CampusSwapDatabase::class.java, "campusswap.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
    }
}
