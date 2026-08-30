package com.flexboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dictionary_words")
data class DictionaryWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val frequency: Int = 1,
    val language: String = "en",
    val nextWord: String? = null
)

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val pinned: Boolean = false,
    val category: String = "All",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "macros")
data class Macro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcode: String,
    val expansion: String,
    val caseSensitive: Boolean = false,
    val triggerOnSpace: Boolean = true
)

@Entity(tableName = "saved_sentences")
data class SavedSentence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sentence: String,
    val useCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_words WHERE word LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun suggest(prefix: String, limit: Int = 5): List<DictionaryWord>

    @Query("SELECT * FROM dictionary_words WHERE nextWord = :prevWord ORDER BY frequency DESC LIMIT :limit")
    suspend fun nextWordFor(prevWord: String, limit: Int = 5): List<DictionaryWord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: DictionaryWord): Long

    @Query("UPDATE dictionary_words SET frequency = frequency + 1 WHERE word = :word")
    suspend fun bump(word: String)

    @Query("SELECT * FROM dictionary_words ORDER BY frequency DESC LIMIT :limit")
    fun all(limit: Int = 500): Flow<List<DictionaryWord>>

    @Delete
    suspend fun delete(word: DictionaryWord)
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, createdAt DESC")
    suspend fun allOnce(): List<ClipboardItem>

    @Insert
    suspend fun insert(item: ClipboardItem): Long

    @Update
    suspend fun update(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE pinned = 0 AND createdAt < :before")
    suspend fun trimOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM clipboard_items WHERE pinned = 0")
    suspend fun unpinnedCount(): Int

    @Query("DELETE FROM clipboard_items WHERE id IN (SELECT id FROM clipboard_items WHERE pinned = 0 ORDER BY createdAt ASC LIMIT :n)")
    suspend fun trimOldest(n: Int)
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY shortcode ASC")
    fun all(): Flow<List<Macro>>

    @Query("SELECT * FROM macros")
    suspend fun allOnce(): List<Macro>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(macro: Macro): Long

    @Delete
    suspend fun delete(macro: Macro)
}

@Dao
interface SavedSentenceDao {
    @Query("SELECT * FROM saved_sentences ORDER BY useCount DESC, createdAt DESC LIMIT :limit")
    fun top(limit: Int = 100): Flow<List<SavedSentence>>

    @Query("SELECT * FROM saved_sentences WHERE sentence = :s LIMIT 1")
    suspend fun find(s: String): SavedSentence?

    @Insert
    suspend fun insert(item: SavedSentence): Long

    @Query("UPDATE saved_sentences SET useCount = useCount + 1 WHERE id = :id")
    suspend fun bump(id: Long)

    @Delete
    suspend fun delete(item: SavedSentence)
}

@Database(
    entities = [DictionaryWord::class, ClipboardItem::class, Macro::class, SavedSentence::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun macroDao(): MacroDao
    abstract fun savedSentenceDao(): SavedSentenceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flexboard.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
