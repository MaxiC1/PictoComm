package com.inacap.picto_comm.data.database.dao

import androidx.room.*
import com.inacap.picto_comm.data.database.entities.PictogramaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones con Pictogramas en Room
 *
 * Define todas las operaciones de base de datos para pictogramas
 * Incluye funcionalidades de control parental y gestión de imágenes
 */
@Dao
interface PictogramaDao {

    /**
     * Inserta un pictograma (o actualiza si ya existe)
     * @return ID del pictograma insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(pictograma: PictogramaEntity): Long

    /**
     * Inserta múltiples pictogramas
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(pictogramas: List<PictogramaEntity>)

    /**
     * Actualiza un pictograma existente
     */
    @Update
    suspend fun actualizar(pictograma: PictogramaEntity)

    /**
     * Elimina un pictograma
     */
    @Delete
    suspend fun eliminar(pictograma: PictogramaEntity)

    /**
     * Obtiene todos los pictogramas aprobados (Flow para observar cambios)
     */
    @Query("SELECT * FROM pictogramas WHERE aprobado = 1 ORDER BY texto ASC")
    fun obtenerTodosAprobados(): Flow<List<PictogramaEntity>>

    /**
     * Obtiene todos los pictogramas aprobados (lista directa)
     */
    @Query("SELECT * FROM pictogramas WHERE aprobado = 1 ORDER BY texto ASC")
    suspend fun obtenerTodosAprobadosList(): List<PictogramaEntity>

    /**
     * Obtiene todos los pictogramas (incluyendo no aprobados) como Flow
     */
    @Query("SELECT * FROM pictogramas ORDER BY texto ASC")
    fun obtenerTodos(): Flow<List<PictogramaEntity>>

    /**
     * Obtiene todos los pictogramas (incluyendo no aprobados) como lista directa
     */
    @Query("SELECT * FROM pictogramas ORDER BY texto ASC")
    suspend fun obtenerTodosList(): List<PictogramaEntity>

    /**
     * Obtiene todos los pictogramas (no observable, para uso único)
     * @deprecated Use obtenerTodosAprobadosList() instead
     */
    @Query("SELECT * FROM pictogramas WHERE aprobado = 1 ORDER BY texto ASC")
    suspend fun obtenerTodosUnaVez(): List<PictogramaEntity>

    /**
     * Obtiene pictogramas por categoría (solo aprobados)
     */
    @Query("SELECT * FROM pictogramas WHERE categoria = :categoria AND aprobado = 1 ORDER BY texto ASC")
    fun obtenerPorCategoria(categoria: String): Flow<List<PictogramaEntity>>

    /**
     * Obtiene solo los favoritos (solo aprobados)
     */
    @Query("SELECT * FROM pictogramas WHERE esFavorito = 1 AND aprobado = 1 ORDER BY texto ASC")
    fun obtenerFavoritos(): Flow<List<PictogramaEntity>>

    /**
     * Obtiene los más usados (ordenados por frecuencia descendente)
     */
    @Query("SELECT * FROM pictogramas WHERE aprobado = 1 ORDER BY frecuenciaUso DESC LIMIT :limite")
    fun obtenerMasUsados(limite: Int): Flow<List<PictogramaEntity>>

    /**
     * Marca/desmarca un pictograma como favorito
     */
    @Query("UPDATE pictogramas SET esFavorito = :esFavorito WHERE id = :id")
    suspend fun actualizarFavorito(id: Long, esFavorito: Boolean)

    /**
     * Incrementa el contador de frecuencia de uso
     */
    @Query("UPDATE pictogramas SET frecuenciaUso = frecuenciaUso + 1 WHERE id = :id")
    suspend fun incrementarFrecuencia(id: Long)

    /**
     * Obtiene un pictograma por ID
     */
    @Query("SELECT * FROM pictogramas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): PictogramaEntity?

    /**
     * Cuenta el total de pictogramas
     */
    @Query("SELECT COUNT(*) FROM pictogramas")
    suspend fun contarPictogramas(): Int

    /**
     * Elimina todos los pictogramas (útil para reset)
     */
    @Query("DELETE FROM pictogramas")
    suspend fun eliminarTodos()

    // ====================================================================
    // NUEVAS FUNCIONES PARA CONTROL PARENTAL
    // ====================================================================

    /**
     * Obtiene pictogramas pendientes de aprobación
     */
    @Query("SELECT * FROM pictogramas WHERE aprobado = 0 ORDER BY fechaCreacion DESC")
    fun obtenerPendientesAprobacion(): Flow<List<PictogramaEntity>>

    /**
     * Obtiene pictogramas creados por un usuario específico
     */
    @Query("SELECT * FROM pictogramas WHERE creadoPor = :usuarioId ORDER BY fechaCreacion DESC")
    fun obtenerPorCreador(usuarioId: Long): Flow<List<PictogramaEntity>>

    /**
     * Aprueba un pictograma
     */
    @Query("UPDATE pictogramas SET aprobado = 1 WHERE id = :id")
    suspend fun aprobar(id: Long)

    /**
     * Rechaza (elimina) un pictograma
     */
    @Query("DELETE FROM pictogramas WHERE id = :id")
    suspend fun rechazar(id: Long)

    /**
     * Cuenta pictogramas pendientes de aprobación
     */
    @Query("SELECT COUNT(*) FROM pictogramas WHERE aprobado = 0")
    suspend fun contarPendientes(): Int

    /**
     * Obtiene pictogramas por tipo de imagen
     */
    @Query("SELECT * FROM pictogramas WHERE tipoImagen = :tipo AND aprobado = 1 ORDER BY texto ASC")
    fun obtenerPorTipoImagen(tipo: String): Flow<List<PictogramaEntity>>

    /**
     * Obtiene pictogramas con fotos personalizadas
     */
    @Query("SELECT * FROM pictogramas WHERE tipoImagen = 'FOTO' AND aprobado = 1 ORDER BY fechaCreacion DESC")
    fun obtenerConFotos(): Flow<List<PictogramaEntity>>
}
