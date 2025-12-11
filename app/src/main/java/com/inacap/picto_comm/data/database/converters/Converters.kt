package com.inacap.picto_comm.data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inacap.picto_comm.data.model.Categoria

/**
 * TypeConverters para Room Database
 *
 * Convierte tipos complejos a tipos que Room puede almacenar
 */
class Converters {

    private val gson = Gson()

    /**
     * Convierte List<Long> a String JSON
     */
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return gson.toJson(value)
    }

    /**
     * Convierte String JSON a List<Long>
     */
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType)
    }

    /**
     * Convierte Categoria enum a String
     */
    @TypeConverter
    fun fromCategoria(categoria: Categoria): String {
        return categoria.name
    }

    /**
     * Convierte String a Categoria enum
     */
    @TypeConverter
    fun toCategoria(value: String): Categoria {
        return Categoria.valueOf(value)
    }
}
