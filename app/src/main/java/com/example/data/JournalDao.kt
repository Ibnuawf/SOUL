package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY dateString DESC")
    fun getAllEntriesFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries")
    suspend fun getAllEntries(): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE dateString = :dateString LIMIT 1")
    suspend fun getEntryByDate(dateString: String): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntrySync(entry: JournalEntry)

    @Query("SELECT * FROM system_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SystemSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SystemSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSettingSync(setting: SystemSetting)
}
