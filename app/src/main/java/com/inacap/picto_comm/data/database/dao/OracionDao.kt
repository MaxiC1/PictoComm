package com.inacap.picto_comm.data.database.dao

import androidx.room.*
import com.inacap.picto_comm.data.database.entities.OracionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones con Oraciones guardadas
 *
 * Define todas las operaciones de base de datos para el historial de oraciones
 */
@Dao
interface OracionDao {

    /**
     * Inserta una nueva oración
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(oracion: OracionEntity): Long

    /**
     * Actualiza una oración existente
     */
    @Update
    suspend fun actualizar(oracion: OracionEntity)

    /**
     * Elimina una oración
     */
    @Delete
    suspend fun eliminar(oracion: OracionEntity)

    /**
     * Obtiene todas las oraciones ordenadas por fecha (más recientes primero)
     */
    @Query("SELECT * FROM oraciones ORDER BY fechaCreacion DESC")
    fun obtenerTodas(): Flow<List<OracionEntity>>

    /**
     * Obtiene las N oraciones más recientes
     */
    @Query("SELECT * FROM oraciones ORDER BY fechaCreacion DESC LIMIT :limite")
    fun obtenerRecientes(limite: Int = 10): Flow<List<OracionEntity>>

    /**
     * Obtiene las oraciones más usadas (ordenadas por vecesUsada)
     */
    @Query("SELECT * FROM oraciones ORDER BY vecesUsada DESC LIMIT :limite")
    fun obtenerMasUsadas(limite: Int = 10): Flow<List<OracionEntity>>

    /**
     * Obtiene una oración por ID
     */
    @Query("SELECT * FROM oraciones WHERE id = :id")
    suspend fun obtenerPorId(id: Long): OracionEntity?

    /**
     * Incrementa el contador de veces usada
     */
    @Query("UPDATE oraciones SET vecesUsada = vecesUsada + 1 WHERE id = :id")
    suspend fun incrementarUso(id: Long)

    /**
     * Busca oraciones que contengan cierto texto
     */
    @Query("SELECT * FROM oraciones WHERE textoCompleto LIKE '%' || :textoBusqueda || '%' ORDER BY fechaCreacion DESC")
    fun buscarPorTexto(textoBusqueda: String): Flow<List<OracionEntity>>

    /**
     * Cuenta el total de oraciones guardadas
     */
    @Query("SELECT COUNT(*) FROM oraciones")
    suspend fun contarOraciones(): Int

    /**
     * Elimina todas las oraciones (útil para reset)
     */
    @Query("DELETE FROM oraciones")
    suspend fun eliminarTodas()
}
