package com.hivend.agatha.data.local

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Conversiones de columnas que Room no conoce. Los enums no necesitan una: Room los guarda por
 * nombre, así que una constante de enum persistida no se renombra sin migración.
 */
class Converters {

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
