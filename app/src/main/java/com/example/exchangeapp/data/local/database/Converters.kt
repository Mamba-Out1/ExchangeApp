package com.example.exchangeapp.data.local.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return json.encodeToString(list)
    }
}
