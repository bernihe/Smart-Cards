package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String, // Hex color code e.g. "#6366F1"
    val iconName: String, // "travel", "project", "food", "idea", "general"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "smart_cards")
data class SmartCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,
    val title: String,
    val address: String,
    val description: String,
    val estimatedPrices: String,
    val alternatives: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SmartCardDao {
    // Folders
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Int): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("DELETE FROM smart_cards WHERE folderId = :folderId")
    suspend fun deleteCardsByFolderId(folderId: Int)

    // Smart Cards
    @Query("SELECT * FROM smart_cards WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getCardsForFolder(folderId: Int): Flow<List<SmartCard>>

    @Query("SELECT * FROM smart_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Int): SmartCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: SmartCard): Long

    @Update
    suspend fun updateCard(card: SmartCard)

    @Delete
    suspend fun deleteCard(card: SmartCard)
    
    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getFolderCount(): Int
}

@Database(entities = [Folder::class, SmartCard::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smartCardDao(): SmartCardDao
}
