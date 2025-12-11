package com.inacap.picto_comm.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen

/**
 * Entidad de Room para almacenar pictogramas en base de datos
 *
 * TABLA: pictogramas
 * 
 * Representa un pictograma con todos sus datos persistentes:
 * - ID único autogenerado
 * - Texto a mostrar
 * - Categoría gramatical
 * - Recurso de imagen
 * - Marcado como favorito
 * - Contador de frecuencia de uso
 * - Estado de aprobación (para control parental)
 * - Usuario creador
 * - Tipo de imagen (icono o foto)
 * - Ruta de imagen personalizada
 */
@Entity(tableName = "pictogramas")
data class PictogramaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val texto: String,
    val categoria: String, // Guardado como String para Room
    val recursoImagen: String,
    val esFavorito: Boolean = false,
    val frecuenciaUso: Int = 0,  // Contador de veces que se ha usado
    
    // Nuevos campos para control parental
    val aprobado: Boolean = true, // Por defecto aprobado (pictogramas del sistema)
    val creadoPor: Long = 0, // ID del usuario creador (0 = sistema)
    
    // Nuevos campos para fotos personalizadas
    val tipoImagen: String = "ICONO", // "ICONO" o "FOTO"
    val rutaImagen: String = "", // Ruta local de la imagen personalizada
    
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    /**
     * Convierte la entidad de Room a modelo de dominio
     */
    fun toModel(): PictogramaSimple {
        return PictogramaSimple(
            id = id.toString(),
            texto = texto,
            categoria = try {
                Categoria.valueOf(categoria)
            } catch (e: Exception) {
                Categoria.COSAS
            },
            recursoImagen = recursoImagen,
            esFavorito = esFavorito,
            aprobado = aprobado,
            creadoPor = creadoPor.toString(),
            tipoImagen = try {
                TipoImagen.valueOf(tipoImagen)
            } catch (e: Exception) {
                TipoImagen.ICONO
            },
            urlImagen = rutaImagen,
            fechaCreacion = fechaCreacion
        )
    }

    companion object {
        /**
         * Crea una entidad desde un modelo de dominio
         */
        fun fromModel(pictograma: PictogramaSimple, frecuenciaUso: Int = 0): PictogramaEntity {
            return PictogramaEntity(
                id = pictograma.id.toLongOrNull() ?: 0,
                texto = pictograma.texto,
                categoria = pictograma.categoria.name,
                recursoImagen = pictograma.recursoImagen,
                esFavorito = pictograma.esFavorito,
                frecuenciaUso = frecuenciaUso,
                aprobado = pictograma.aprobado,
                creadoPor = pictograma.creadoPor.toLongOrNull() ?: 0,
                tipoImagen = pictograma.tipoImagen.name,
                rutaImagen = pictograma.urlImagen,
                fechaCreacion = pictograma.fechaCreacion
            )
        }
    }
}
