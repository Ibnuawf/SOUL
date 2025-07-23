package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JournalEntry::class, SystemSetting::class], version = 1, exportSchema = false)
abstract class SoulDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: SoulDatabase? = null

        fun getDatabase(context: Context): SoulDatabase {
            return INSTANCE ?: synchronized(this) {
                // sync backups before Room wakes up
                try {
                    DataSyncManager.syncAndRestore(context.applicationContext)
                } catch (e: Exception) {
                    Log.e("SoulDatabase", "Error performing sync and restore", e)
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoulDatabase::class.java,
                    "soul_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
