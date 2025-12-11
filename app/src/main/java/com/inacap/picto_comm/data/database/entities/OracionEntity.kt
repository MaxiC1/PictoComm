package com.inacap.picto_comm.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.inacap.picto_comm.data.database.converters.Converters

/**
 * Entidad de Room para almacenar oraciones guardadas
 *
 * Representa una oración construida por el usuario:
 * - ID único autogenerado
 * - Lista de IDs de pictogramas (en orden)
 * - Texto completo de la oración
 * - Fecha de creación
 * - Contador de veces usada
 */
@Entity(tableName = "oraciones")
@TypeConverters(Converters::class)
data class OracionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val pictogramaIds: List<Long>,  // IDs de los pictogramas en orden
    val textoCompleto: String,       // "Yo Quiero Comer Helado"
    val fechaCreacion: Long = System.currentTimeMillis(),
    val vecesUsada: Int = 0
)
