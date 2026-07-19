package com.emon.proxagallery.ai

import androidx.room.TypeConverter

class AiConverters {
    @TypeConverter
    fun fromStatus(value: AiIndexStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): AiIndexStatus = AiIndexStatus.valueOf(value)
}
