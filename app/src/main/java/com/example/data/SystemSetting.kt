package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_settings")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String
)
