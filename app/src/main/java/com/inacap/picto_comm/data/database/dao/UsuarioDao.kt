package com.inacap.picto_comm.data.database.dao

import androidx.room.*
import com.inacap.picto_comm.data.database.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones con usuarios en Room
 * 
 * Proporciona métodos para:
 * - Insertar usuarios
 * - Consultar usuarios
 * - Actualizar usuarios
 * - Eliminar usuarios
 * - Verificar PIN
 */
@Dao
interface UsuarioDao {
    
    /**
     * Inserta un nuevo usuario
     * @return ID del usuario insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity): Long
    
    /**
     * Inserta múltiples usuarios
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(usuarios: List<UsuarioEntity>)
    
    /**
     * Actualiza un usuario existente
     */
    @Update
    suspend fun actualizar(usuario: UsuarioEntity)
    
    /**
     * Elimina un usuario
     */
    @Delete
    suspend fun eliminar(usuario: UsuarioEntity)
    
    /**
     * Obtiene un usuario por su ID
     */
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun obtenerPorId(id: Long): UsuarioEntity?
    
    /**
     * Obtiene un usuario por su ID como Flow (observa cambios)
     */
    @Query("SELECT * FROM usuarios WHERE id = :id")
    fun obtenerPorIdFlow(id: Long): Flow<UsuarioEntity?>
    
    /**
     * Obtiene todos los usuarios
     */
    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY fechaCreacion DESC")
    suspend fun obtenerTodos(): List<UsuarioEntity>
    
    /**
     * Obtiene todos los usuarios como Flow (observa cambios)
     */
    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY fechaCreacion DESC")
    fun obtenerTodosFlow(): Flow<List<UsuarioEntity>>
    
    /**
     * Obtiene usuarios por tipo (PADRE o HIJO)
     */
    @Query("SELECT * FROM usuarios WHERE tipo = :tipo AND activo = 1")
    suspend fun obtenerPorTipo(tipo: String): List<UsuarioEntity>
    
    /**
     * Obtiene el usuario padre (administrador)
     */
    @Query("SELECT * FROM usuarios WHERE tipo = 'PADRE' AND activo = 1 LIMIT 1")
    suspend fun obtenerPadre(): UsuarioEntity?
    
    /**
     * Obtiene todos los usuarios hijo
     */
    @Query("SELECT * FROM usuarios WHERE tipo = 'HIJO' AND activo = 1")
    suspend fun obtenerHijos(): List<UsuarioEntity>
    
    /**
     * Verifica si existe un usuario padre
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE tipo = 'PADRE' AND activo = 1")
    suspend fun existePadre(): Int
    
    /**
     * Verifica si existe algún usuario
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE activo = 1")
    suspend fun contarUsuarios(): Int
    
    /**
     * Verifica el PIN de un usuario
     */
    @Query("SELECT * FROM usuarios WHERE id = :id AND pin = :pin AND activo = 1")
    suspend fun verificarPin(id: Long, pin: String): UsuarioEntity?
    
    /**
     * Actualiza el PIN de un usuario
     */
    @Query("UPDATE usuarios SET pin = :nuevoPin WHERE id = :id")
    suspend fun actualizarPin(id: Long, nuevoPin: String)
    
    /**
     * Desactiva un usuario (soft delete)
     */
    @Query("UPDATE usuarios SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Long)
    
    /**
     * Elimina todos los usuarios (para testing)
     */
    @Query("DELETE FROM usuarios")
    suspend fun eliminarTodos()
}
