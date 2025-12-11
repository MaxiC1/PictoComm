package com.inacap.picto_comm.data.repository

import com.inacap.picto_comm.data.database.dao.OracionDao
import com.inacap.picto_comm.data.database.dao.PictogramaDao
import com.inacap.picto_comm.data.database.dao.UsuarioDao
import com.inacap.picto_comm.data.database.entities.PictogramaEntity
import com.inacap.picto_comm.data.database.entities.UsuarioEntity
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio principal para operaciones con Room Database
 * 
 * Actúa como capa de abstracción entre la UI y la base de datos
 * Proporciona métodos para:
 * - Gestión de usuarios (PADRE/HIJO)
 * - Gestión de pictogramas (CRUD + control parental)
 * - Gestión de oraciones guardadas
 * 
 * VENTAJAS DE USAR ROOM:
 * - 100% gratuito
 * - Funciona offline
 * - Datos persistentes localmente
 * - No requiere configuración externa
 */
class RoomRepository(
    private val pictogramaDao: PictogramaDao,
    private val oracionDao: OracionDao,
    private val usuarioDao: UsuarioDao
) {

    // ====================================================================
    // SECCIÓN: GESTIÓN DE USUARIOS
    // ====================================================================

    /**
     * Crea un nuevo usuario
     * @return ID del usuario creado
     */
    suspend fun crearUsuario(usuario: Usuario): Long {
        val entity = UsuarioEntity.fromUsuario(usuario)
        return usuarioDao.insertar(entity)
    }

    /**
     * Obtiene un usuario por ID
     */
    suspend fun obtenerUsuario(id: Long): Usuario? {
        return usuarioDao.obtenerPorId(id)?.toUsuario()
    }

    /**
     * Obtiene todos los usuarios activos
     */
    suspend fun obtenerTodosUsuarios(): List<Usuario> {
        return usuarioDao.obtenerTodos().map { it.toUsuario() }
    }

    /**
     * Obtiene todos los usuarios como Flow (observa cambios)
     */
    fun obtenerTodosUsuariosFlow(): Flow<List<Usuario>> {
        return usuarioDao.obtenerTodosFlow().map { lista ->
            lista.map { it.toUsuario() }
        }
    }

    /**
     * Obtiene el usuario padre (administrador)
     */
    suspend fun obtenerPadre(): Usuario? {
        return usuarioDao.obtenerPadre()?.toUsuario()
    }

    /**
     * Obtiene todos los usuarios hijo
     */
    suspend fun obtenerHijos(): List<Usuario> {
        return usuarioDao.obtenerHijos().map { it.toUsuario() }
    }

    /**
     * Verifica si existe un usuario padre
     */
    suspend fun existePadre(): Boolean {
        return usuarioDao.existePadre() > 0
    }

    /**
     * Verifica si existe algún usuario
     */
    suspend fun existenUsuarios(): Boolean {
        return usuarioDao.contarUsuarios() > 0
    }

    /**
     * Verifica el PIN de un usuario
     * @return Usuario si el PIN es correcto, null si no
     */
    suspend fun verificarPin(usuarioId: Long, pin: String): Usuario? {
        return usuarioDao.verificarPin(usuarioId, pin)?.toUsuario()
    }

    /**
     * Actualiza el PIN de un usuario
     */
    suspend fun actualizarPin(usuarioId: Long, nuevoPin: String) {
        usuarioDao.actualizarPin(usuarioId, nuevoPin)
    }

    /**
     * Actualiza un usuario
     */
    suspend fun actualizarUsuario(usuario: Usuario) {
        val entity = UsuarioEntity.fromUsuario(usuario)
        usuarioDao.actualizar(entity)
    }

    /**
     * Desactiva un usuario (soft delete)
     */
    suspend fun desactivarUsuario(usuarioId: Long) {
        usuarioDao.desactivar(usuarioId)
    }

    // ====================================================================
    // SECCIÓN: GESTIÓN DE PICTOGRAMAS
    // ====================================================================

    /**
     * Obtiene todos los pictogramas aprobados (como Flow)
     */
    fun obtenerTodosPictogramas(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerTodosAprobados().map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Obtiene todos los pictogramas aprobados (como lista directa)
     * Para uso en MainActivity
     */
    suspend fun obtenerPictogramasAprobados(): List<PictogramaEntity> {
        return pictogramaDao.obtenerTodosAprobadosList()
    }

    /**
     * Obtiene todos los pictogramas (incluyendo no aprobados) como Flow
     * Solo para uso del PADRE
     */
    fun obtenerTodosPictogramasInclusoNoAprobados(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerTodos().map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Obtiene todos los pictogramas (incluyendo no aprobados) como lista directa
     * Solo para uso del PADRE
     */
    suspend fun obtenerTodosPictogramasList(): List<PictogramaEntity> {
        return pictogramaDao.obtenerTodosList()
    }

    /**
     * Obtiene pictogramas por categoría (solo aprobados)
     */
    fun obtenerPictogramasPorCategoria(categoria: Categoria): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerPorCategoria(categoria.name).map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Obtiene pictogramas favoritos
     */
    fun obtenerPictogramasFavoritos(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerFavoritos().map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Obtiene los pictogramas más usados
     */
    fun obtenerPictogramasMasUsados(limite: Int = 20): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerMasUsados(limite).map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Crea un nuevo pictograma
     * @return ID del pictograma creado
     */
    suspend fun crearPictograma(pictograma: PictogramaSimple): Long {
        val entity = PictogramaEntity.fromModel(pictograma, frecuenciaUso = 0)
        return pictogramaDao.insertar(entity)
    }

    /**
     * Actualiza un pictograma existente
     */
    suspend fun actualizarPictograma(pictograma: PictogramaSimple) {
        val entity = PictogramaEntity.fromModel(pictograma, frecuenciaUso = 0)
        pictogramaDao.actualizar(entity)
    }

    /**
     * Elimina un pictograma
     */
    suspend fun eliminarPictograma(pictograma: PictogramaSimple) {
        val entity = PictogramaEntity.fromModel(pictograma, frecuenciaUso = 0)
        pictogramaDao.eliminar(entity)
    }

    /**
     * Marca/desmarca un pictograma como favorito
     */
    suspend fun alternarFavorito(pictogramaId: String, esFavorito: Boolean) {
        val id = pictogramaId.toLongOrNull() ?: return
        pictogramaDao.actualizarFavorito(id, esFavorito)
    }

    /**
     * Incrementa el contador de uso de un pictograma
     */
    suspend fun incrementarUso(pictogramaId: String) {
        val id = pictogramaId.toLongOrNull() ?: return
        pictogramaDao.incrementarFrecuencia(id)
    }

    // ====================================================================
    // SECCIÓN: CONTROL PARENTAL
    // ====================================================================

    /**
     * Obtiene pictogramas pendientes de aprobación
     * Solo para uso del PADRE
     */
    fun obtenerPictogramasPendientes(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerPendientesAprobacion().map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Obtiene pictogramas creados por un usuario específico
     */
    fun obtenerPictogramasPorCreador(usuarioId: Long): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerPorCreador(usuarioId).map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Aprueba un pictograma
     * Solo puede hacerlo el PADRE
     */
    suspend fun aprobarPictograma(pictogramaId: String) {
        val id = pictogramaId.toLongOrNull() ?: return
        pictogramaDao.aprobar(id)
    }

    /**
     * Rechaza (elimina) un pictograma
     * Solo puede hacerlo el PADRE
     */
    suspend fun rechazarPictograma(pictogramaId: String) {
        val id = pictogramaId.toLongOrNull() ?: return
        pictogramaDao.rechazar(id)
    }

    /**
     * Cuenta pictogramas pendientes de aprobación
     */
    suspend fun contarPictogramasPendientes(): Int {
        return pictogramaDao.contarPendientes()
    }

    // ====================================================================
    // SECCIÓN: GESTIÓN DE FOTOS (PREPARADO PARA FUTURO)
    // ====================================================================

    /**
     * Obtiene pictogramas con fotos personalizadas
     */
    fun obtenerPictogramasConFotos(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerConFotos().map { lista ->
            lista.map { it.toModel() }
        }
    }

    /**
     * Guarda la ruta de una foto personalizada
     * (Para implementar cuando se agregue la funcionalidad de cámara)
     */
    suspend fun guardarFotoPictograma(pictogramaId: String, rutaFoto: String) {
        val id = pictogramaId.toLongOrNull() ?: return
        val pictograma = pictogramaDao.obtenerPorId(id) ?: return
        val actualizado = pictograma.copy(
            tipoImagen = "FOTO",
            rutaImagen = rutaFoto
        )
        pictogramaDao.actualizar(actualizado)
    }

    // ====================================================================
    // SECCIÓN: UTILIDADES
    // ====================================================================

    /**
     * Cuenta el total de pictogramas
     */
    suspend fun contarPictogramas(): Int {
        return pictogramaDao.contarPictogramas()
    }

    /**
     * Limpia todos los datos (útil para testing o reset)
     */
    suspend fun limpiarTodo() {
        pictogramaDao.eliminarTodos()
        usuarioDao.eliminarTodos()
    }
}
