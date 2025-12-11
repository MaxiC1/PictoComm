package com.inacap.picto_comm.data.repository

import com.inacap.picto_comm.data.database.dao.OracionDao
import com.inacap.picto_comm.data.database.dao.PictogramaDao
import com.inacap.picto_comm.data.database.entities.OracionEntity
import com.inacap.picto_comm.data.database.entities.PictogramaEntity
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio principal de la aplicación
 *
 * Actúa como fuente única de verdad para los datos
 * Coordina el acceso a Room Database y expone datos al ViewModel
 *
 * RESPONSABILIDADES:
 * - Obtener pictogramas de la base de datos
 * - Filtrar por categoría, favoritos, más usados
 * - Guardar y obtener oraciones
 * - Actualizar favoritos y frecuencias de uso
 */
class PictoCommRepository(
    private val pictogramaDao: PictogramaDao,
    private val oracionDao: OracionDao
) {

    // ====================================================================
    // PICTOGRAMAS
    // ====================================================================

    /**
     * Obtiene todos los pictogramas (Flow observable)
     */
    fun obtenerTodosPictogramas(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerTodos().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Obtiene pictogramas por categoría
     */
    fun obtenerPictogramasPorCategoria(categoria: Categoria): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerPorCategoria(categoria.name).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Obtiene solo los favoritos
     */
    fun obtenerFavoritos(): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerFavoritos().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Obtiene los más usados
     */
    fun obtenerMasUsados(limite: Int = 20): Flow<List<PictogramaSimple>> {
        return pictogramaDao.obtenerMasUsados(limite).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Marca/desmarca un pictograma como favorito
     */
    suspend fun alternarFavorito(id: Long, esFavorito: Boolean) {
        pictogramaDao.actualizarFavorito(id, esFavorito)
    }

    /**
     * Incrementa el contador de uso de un pictograma
     * Se llama cada vez que se usa un pictograma en una oración
     */
    suspend fun incrementarFrecuenciaPictograma(id: Long) {
        pictogramaDao.incrementarFrecuencia(id)
    }

    /**
     * Obtiene un pictograma por ID (suspending, no observable)
     */
    suspend fun obtenerPictogramaPorId(id: Long): PictogramaSimple? {
        return pictogramaDao.obtenerPorId(id)?.toModel()
    }

    // ====================================================================
    // ORACIONES
    // ====================================================================

    /**
     * Guarda una nueva oración en el historial
     */
    suspend fun guardarOracion(
        pictogramas: List<PictogramaSimple>,
        textoCompleto: String
    ): Long {
        // Crear entidad
        val oracion = OracionEntity(
            pictogramaIds = pictogramas.map { it.id.toLongOrNull() ?: 0L },
            textoCompleto = textoCompleto,
            vecesUsada = 1  // Primera vez que se usa
        )

        // Insertar en base de datos
        val idOracion = oracionDao.insertar(oracion)

        // Incrementar frecuencia de uso de cada pictograma
        pictogramas.forEach { pictograma ->
            val id = pictograma.id.toLongOrNull() ?: 0L
            if (id > 0) {
                incrementarFrecuenciaPictograma(id)
            }
        }

        return idOracion
    }

    /**
     * Obtiene todas las oraciones del historial
     */
    fun obtenerTodasOraciones(): Flow<List<OracionEntity>> {
        return oracionDao.obtenerTodas()
    }

    /**
     * Obtiene las N oraciones más recientes
     */
    fun obtenerOracionesRecientes(limite: Int = 10): Flow<List<OracionEntity>> {
        return oracionDao.obtenerRecientes(limite)
    }

    /**
     * Obtiene las oraciones más usadas
     */
    fun obtenerOracionesMasUsadas(limite: Int = 10): Flow<List<OracionEntity>> {
        return oracionDao.obtenerMasUsadas(limite)
    }

    /**
     * Incrementa el contador de uso de una oración
     * Se llama cuando el usuario reutiliza una oración del historial
     */
    suspend fun incrementarUsoOracion(id: Long) {
        oracionDao.incrementarUso(id)
    }

    /**
     * Elimina una oración del historial
     */
    suspend fun eliminarOracion(oracion: OracionEntity) {
        oracionDao.eliminar(oracion)
    }

    /**
     * Busca oraciones por texto
     */
    fun buscarOraciones(texto: String): Flow<List<OracionEntity>> {
        return oracionDao.buscarPorTexto(texto)
    }

    /**
     * Reconstruye una oración desde el historial
     * Obtiene los pictogramas correspondientes a los IDs guardados
     */
    suspend fun reconstruirOracion(oracion: OracionEntity): List<PictogramaSimple> {
        return oracion.pictogramaIds.mapNotNull { id ->
            pictogramaDao.obtenerPorId(id)?.toModel()
        }
    }

    // ====================================================================
    // ESTADÍSTICAS Y UTILIDADES
    // ====================================================================

    /**
     * Cuenta total de pictogramas en la base de datos
     */
    suspend fun contarPictogramas(): Int {
        return pictogramaDao.contarPictogramas()
    }

    /**
     * Cuenta total de oraciones guardadas
     */
    suspend fun contarOraciones(): Int {
        return oracionDao.contarOraciones()
    }
}
