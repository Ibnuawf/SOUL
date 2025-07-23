package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val dateString: String, // Format: YYYY-MM-DD
    val timestamp: Long,
    val unlockTimestamp: Long,
    val isCompleted: Boolean,
    val encryptedAnswers: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JournalEntry

        if (dateString != other.dateString) return false
        if (timestamp != other.timestamp) return false
        if (unlockTimestamp != other.unlockTimestamp) return false
        if (isCompleted != other.isCompleted) return false
        if (!encryptedAnswers.contentEquals(other.encryptedAnswers)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dateString.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + unlockTimestamp.hashCode()
        result = 31 * result + isCompleted.hashCode()
        result = 31 * result + encryptedAnswers.contentHashCode()
        return result
    }
}
