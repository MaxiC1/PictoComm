package com.inacap.picto_comm.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.inacap.picto_comm.data.FuenteDatosMock
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado de la interfaz de usuario
 */
data class EstadoInterfazDemo(
    val oracionActual: List<PictogramaSimple> = emptyList(),
    val pictogramasDisponibles: List<PictogramaSimple> = emptyList(),
    val todosPictogramas: List<PictogramaSimple> = emptyList(),
    val categoriaSeleccionada: Categoria? = null
) {
    val textoOracion: String
        get() = oracionActual.joinToString(" ") { it.texto }

    val puedeReproducir: Boolean
        get() = oracionActual.isNotEmpty()

    val puedeGuardar: Boolean
        get() = oracionActual.size >= 2
}

/**
 * ViewModel principal - Gestiona el estado de pictogramas y oraciones
 */
class ViewModelDemo : ViewModel() {

    private val _estadoInterfaz = MutableStateFlow(EstadoInterfazDemo())
    val estadoInterfaz: StateFlow<EstadoInterfazDemo> = _estadoInterfaz.asStateFlow()

    init {
        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        val todosPictogramas = FuenteDatosMock.obtenerTodosPictogramas()
        val pictogramasIniciales = FuenteDatosMock.obtenerPictogramasMasUsados(20)

        _estadoInterfaz.update {
            it.copy(
                todosPictogramas = todosPictogramas,
                pictogramasDisponibles = pictogramasIniciales
            )
        }
    }

    fun anadirPictogramaAOracion(pictograma: PictogramaSimple) {
        _estadoInterfaz.update { estadoActual ->
            estadoActual.copy(
                oracionActual = estadoActual.oracionActual + pictograma
            )
        }
        sugerirSiguientesPictogramas(pictograma)
    }

    fun eliminarPictogramaDeOracion(indice: Int) {
        _estadoInterfaz.update { estadoActual ->
            estadoActual.copy(
                oracionActual = estadoActual.oracionActual.filterIndexed { i, _ -> i != indice }
            )
        }
    }

    fun limpiarOracion() {
        _estadoInterfaz.update {
            it.copy(
                oracionActual = emptyList(),
                categoriaSeleccionada = null
            )
        }
        cargarPictogramasMasUsadosDesdeMemoria()
    }

    fun seleccionarCategoria(categoria: Categoria?) {
        val todosPictogramas = _estadoInterfaz.value.todosPictogramas

        val pictogramas = if (todosPictogramas.isNotEmpty()) {
            if (categoria != null) {
                todosPictogramas.filter { it.categoria == categoria }
            } else {
                todosPictogramas.take(20)
            }
        } else {
            if (categoria != null) {
                FuenteDatosMock.obtenerPictogramasPorCategoria(categoria)
            } else {
                FuenteDatosMock.obtenerPictogramasMasUsados(20)
            }
        }

        _estadoInterfaz.update {
            it.copy(
                categoriaSeleccionada = categoria,
                pictogramasDisponibles = pictogramas
            )
        }
    }

    private fun cargarPictogramasMasUsados() {
        val pictogramas = FuenteDatosMock.obtenerPictogramasMasUsados(20)
        _estadoInterfaz.update {
            it.copy(pictogramasDisponibles = pictogramas)
        }
    }

    private fun cargarPictogramasMasUsadosDesdeMemoria() {
        val todosPictogramas = _estadoInterfaz.value.todosPictogramas
        if (todosPictogramas.isNotEmpty()) {
            _estadoInterfaz.update {
                it.copy(pictogramasDisponibles = todosPictogramas.take(20))
            }
        } else {
            cargarPictogramasMasUsados()
        }
    }

    private fun sugerirSiguientesPictogramas(ultimoPictograma: PictogramaSimple) {
        val oracionActual = _estadoInterfaz.value.oracionActual
        val tamanioOracion = oracionActual.size

        // Obtener penultimo pictograma si existe
        val penultimoPictograma = if (tamanioOracion >= 2) {
            oracionActual[tamanioOracion - 2]
        } else null

        // REGLAS ESPECIFICAS POR PALABRA (tienen prioridad)
        val sugerenciaEspecifica = obtenerSugerenciaEspecifica(ultimoPictograma)
        if (sugerenciaEspecifica != null) {
            seleccionarCategoria(sugerenciaEspecifica)
            return
        }

        // REGLAS CONTEXTUALES (consideran los ultimos 2 pictogramas)
        val sugerenciaContextual = obtenerSugerenciaContextual(penultimoPictograma, ultimoPictograma)
        if (sugerenciaContextual != null) {
            seleccionarCategoria(sugerenciaContextual)
            return
        }

        // REGLAS BASICAS (solo ultimo pictograma)
        val sugerenciaBasica = obtenerSugerenciaBasica(ultimoPictograma)
        if (sugerenciaBasica != null) {
            seleccionarCategoria(sugerenciaBasica)
        }
    }

    /**
     * Reglas especificas por palabras concretas
     * Ejemplo: "Voy" siempre sugiere LUGARES
     */
    private fun obtenerSugerenciaEspecifica(pictograma: PictogramaSimple): Categoria? {
        return when (pictograma.texto.lowercase()) {
            // Acciones que implican LUGARES
            "voy" -> Categoria.LUGARES
            "ir al bano", "ir al baño" -> Categoria.LUGARES
            "dormir" -> Categoria.LUGARES  // sugiere habitacion, casa, etc.

            // Acciones que implican COSAS (comida/bebida)
            "comer", "beber" -> Categoria.COSAS
            "hambre", "sed" -> Categoria.COSAS  // desde CUALIDADES

            // Acciones que implican COSAS (entretenimiento)
            "jugar", "ver", "escuchar" -> Categoria.COSAS

            else -> null
        }
    }

    /**
     * Reglas contextuales que consideran los ultimos 2 pictogramas
     * Ejemplo: "Yo" + "Voy" -> sugiere LUGARES
     */
    private fun obtenerSugerenciaContextual(
        penultimo: PictogramaSimple?,
        ultimo: PictogramaSimple
    ): Categoria? {
        // Si solo hay 1 pictograma, no hay contexto
        if (penultimo == null) return null

        // PERSONA + ACCION -> depende de la accion
        if (penultimo.categoria == Categoria.PERSONAS && ultimo.categoria == Categoria.ACCIONES) {
            return when (ultimo.texto.lowercase()) {
                "voy" -> Categoria.LUGARES
                "comer", "beber" -> Categoria.COSAS
                "jugar", "ver", "escuchar" -> Categoria.COSAS
                else -> Categoria.COSAS  // por defecto
            }
        }

        // PERSONA + CUALIDAD -> sugiere COSAS o ACCIONES
        if (penultimo.categoria == Categoria.PERSONAS && ultimo.categoria == Categoria.CUALIDADES) {
            return when (ultimo.texto.lowercase()) {
                "hambre", "sed" -> Categoria.COSAS
                "feliz", "triste", "enojado/a", "cansado/a" -> null  // no sugiere cambio
                else -> null
            }
        }

        // ACCION + COSA -> oracion completa, sugiere TIEMPO o LUGARES
        if (penultimo.categoria == Categoria.ACCIONES && ultimo.categoria == Categoria.COSAS) {
            // Oracion basica completa, podria agregar tiempo
            return Categoria.TIEMPO
        }

        // ACCION + LUGAR -> oracion completa, sugiere TIEMPO
        if (penultimo.categoria == Categoria.ACCIONES && ultimo.categoria == Categoria.LUGARES) {
            return Categoria.TIEMPO
        }

        return null
    }

    /**
     * Reglas basicas (solo categoria del ultimo pictograma)
     * Se aplican cuando no hay reglas especificas ni contextuales
     */
    private fun obtenerSugerenciaBasica(pictograma: PictogramaSimple): Categoria? {
        return when (pictograma.categoria) {
            Categoria.PERSONAS -> Categoria.ACCIONES     // Yo -> [verbos]
            Categoria.ACCIONES -> Categoria.COSAS        // Quiero -> [objetos]
            Categoria.CUALIDADES -> Categoria.COSAS      // Hambre -> [objetos]
            Categoria.COSAS -> Categoria.TIEMPO          // Helado -> [cuando]
            Categoria.LUGARES -> Categoria.TIEMPO        // Casa -> [cuando]
            Categoria.TIEMPO -> null                      // No sugiere mas
        }
    }

    // ====================================================================
    // SECCION: SISTEMA DE FAVORITOS
    // Permite marcar pictogramas para acceso rapido (no persiste en demo)
    // ====================================================================

    /**
     * Marca o desmarca un pictograma como favorito
     *
     * NOTA: En el demo, los favoritos NO persisten al cerrar la app
     * En la version completa, se guardarian en base de datos
     *
     * FUNCIONAMIENTO:
     * 1. Busca el pictograma en la lista completa
     * 2. Invierte su estado de favorito (true -> false, false -> true)
     * 3. Actualiza tanto la lista completa como la visible
     */
    fun alternarFavorito(pictograma: PictogramaSimple) {
        // Actualiza la lista completa de pictogramas
        val pictogramasActualizadosCompletos = _estadoInterfaz.value.todosPictogramas.map {
            if (it.id == pictograma.id) {
                // Invierte el estado de favorito de este pictograma
                it.copy(esFavorito = !it.esFavorito)
            } else {
                it  // Mantiene los demas sin cambios
            }
        }

        // Actualiza la lista visible (filtrada)
        val pictogramasActualizadosDisponibles = _estadoInterfaz.value.pictogramasDisponibles.map {
            if (it.id == pictograma.id) {
                it.copy(esFavorito = !it.esFavorito)
            } else {
                it
            }
        }

        // Aplica los cambios al estado
        _estadoInterfaz.update {
            it.copy(
                todosPictogramas = pictogramasActualizadosCompletos,
                pictogramasDisponibles = pictogramasActualizadosDisponibles
            )
        }
    }

    /**
     * Muestra solo los pictogramas marcados como favoritos
     * Util para acceso rapido a pictogramas frecuentes
     */
    fun cargarFavoritos() {
        val todosPictogramas = _estadoInterfaz.value.todosPictogramas
        val favoritos = if (todosPictogramas.isNotEmpty()) {
            // Filtrar favoritos desde los pictogramas en memoria
            todosPictogramas.filter { it.esFavorito }
        } else {
            // Fallback a Mock si no hay pictogramas en memoria
            FuenteDatosMock.obtenerPictogramasFavoritos(todosPictogramas)
        }

        _estadoInterfaz.update {
            it.copy(
                pictogramasDisponibles = favoritos,
                categoriaSeleccionada = null
            )
        }
    }

    // ====================================================================
    // SECCION: GUARDAR ORACION
    // Guarda la oración en Firebase
    // ====================================================================

    /**
     * Guarda la oración actual en Firebase
     *
     * @return Pair<Boolean, String> - (éxito, IDs de pictogramas en formato String)
     */
    fun guardarOracion(): Pair<Boolean, String> {
        val estado = _estadoInterfaz.value
        if (!estado.puedeGuardar) {
            return Pair(false, "")
        }

        // Obtener IDs de los pictogramas de la oración
        val pictogramaIds = estado.oracionActual.map { it.id }.filter { it.isNotEmpty() }
        val textoCompleto = estado.textoOracion

        return Pair(true, pictogramaIds.joinToString(",") + "|" + textoCompleto)
    }

    // ====================================================================
    // SECCION: CARGA DESDE BASE DE DATOS
    // Funciones para cargar pictogramas desde Firebase
    // ====================================================================

    /**
     * Carga pictogramas desde Firebase Firestore
     *
     * @param pictogramas Lista de pictogramas desde Firebase
     */
    fun cargarPictogramasDesdeBaseDatos(pictogramas: List<PictogramaSimple>) {
        // Actualizar estado con todos los pictogramas
        _estadoInterfaz.update {
            it.copy(
                todosPictogramas = pictogramas,
                pictogramasDisponibles = pictogramas.take(20) // Mostrar primeros 20
            )
        }
    }

    /**
     * Carga todos los pictogramas sin filtro
     * Usado cuando el usuario es PADRE
     */
    fun cargarTodosPictogramas() {
        val pictogramas = _estadoInterfaz.value.todosPictogramas
        _estadoInterfaz.update {
            it.copy(
                pictogramasDisponibles = pictogramas,
                categoriaSeleccionada = null
            )
        }
    }
}
