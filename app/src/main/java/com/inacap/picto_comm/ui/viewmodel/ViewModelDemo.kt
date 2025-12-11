package com.inacap.picto_comm.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.inacap.picto_comm.data.FuenteDatosMock
import com.inacap.picto_comm.data.database.entities.PictogramaEntity
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado de la interfaz de usuario para el DEMO
 * Contiene toda la informacion necesaria para mostrar la pantalla
 */
data class EstadoInterfazDemo(
    // Lista de pictogramas que forman la oracion actual
    val oracionActual: List<PictogramaSimple> = emptyList(),

    // Pictogramas disponibles para seleccionar (filtrados por categoria)
    val pictogramasDisponibles: List<PictogramaSimple> = emptyList(),

    // Todos los pictogramas del sistema (sin filtrar)
    val todosPictogramas: List<PictogramaSimple> = emptyList(),

    // Categoria actualmente seleccionada (null = todas)
    val categoriaSeleccionada: Categoria? = null
) {
    /**
     * Convierte la lista de pictogramas en un texto legible
     * Ejemplo: [Yo, Quiero, Comer, Helado] -> "Yo Quiero Comer Helado"
     */
    val textoOracion: String
        get() = oracionActual.joinToString(" ") { it.texto }

    /**
     * Indica si se puede reproducir la oracion con voz
     * Requiere al menos un pictograma
     */
    val puedeReproducir: Boolean
        get() = oracionActual.isNotEmpty()

    /**
     * Indica si se puede guardar la oracion
     * Requiere al menos 2 pictogramas para formar una oracion valida
     */
    val puedeGuardar: Boolean
        get() = oracionActual.size >= 2
}

/**
 * ViewModel principal del DEMO
 *
 * RESPONSABILIDADES:
 * - Gestiona el estado de la interfaz (que pictogramas se muestran)
 * - Maneja la construccion de oraciones (anadir/quitar pictogramas)
 * - Implementa el sistema predictivo (sugiere siguientes pictogramas)
 * - Filtra pictogramas por categoria
 * - Gestiona favoritos (temporal, no persiste)
 *
 * ARQUITECTURA:
 * - Usa StateFlow para emitir cambios de estado
 * - La UI observa uiState y se actualiza automaticamente
 * - No usa base de datos, todo en memoria (MockDataSource)
 */
class ViewModelDemo : ViewModel() {

    // Estado interno mutable (privado)
    // Solo este ViewModel puede modificarlo
    private val _estadoInterfaz = MutableStateFlow(EstadoInterfazDemo())

    // Estado publico de solo lectura
    // La UI puede observarlo pero no modificarlo
    val estadoInterfaz: StateFlow<EstadoInterfazDemo> = _estadoInterfaz.asStateFlow()

    init {
        // Al crear el ViewModel, carga los datos iniciales
        cargarDatosIniciales()
    }

    /**
     * Carga los datos mock (falsos) para el demo
     * En una app real, esto cargaria desde base de datos
     */
    private fun cargarDatosIniciales() {
        // Obtiene todos los pictogramas del sistema
        val todosPictogramas = FuenteDatosMock.obtenerTodosPictogramas()

        // Obtiene los 20 mas usados para mostrar inicialmente
        val pictogramasIniciales = FuenteDatosMock.obtenerPictogramasMasUsados(20)

        // Actualiza el estado de la interfaz
        _estadoInterfaz.update {
            it.copy(
                todosPictogramas = todosPictogramas,
                pictogramasDisponibles = pictogramasIniciales
            )
        }
    }

    // ====================================================================
    // SECCION: GESTION DE LA ORACION
    // Funciones para construir y modificar la oracion actual
    // ====================================================================

    /**
     * Anade un pictograma a la oracion que se esta construyendo
     *
     * FLUJO:
     * 1. Anade el pictograma al final de la oracion
     * 2. Actualiza el estado de la interfaz
     * 3. Activa el sistema predictivo para sugerir siguiente pictograma
     *
     * EJEMPLO:
     * - Oracion actual: [Yo, Quiero]
     * - Usuario toca "Comer"
     * - Nueva oracion: [Yo, Quiero, Comer]
     * - Sistema sugiere pictogramas de categoria COSAS
     */
    fun anadirPictogramaAOracion(pictograma: PictogramaSimple) {
        // Actualiza el estado anadiendo el pictograma al final
        _estadoInterfaz.update { estadoActual ->
            estadoActual.copy(
                oracionActual = estadoActual.oracionActual + pictograma
            )
        }

        // Activa predicciones basadas en el pictograma anadido
        sugerirSiguientesPictogramas(pictograma)
    }

    /**
     * Elimina un pictograma especifico de la oracion
     *
     * @param indice Posicion del pictograma a eliminar (0 = primero)
     *
     * EJEMPLO:
     * - Oracion: [Yo, Quiero, Comer, Helado]
     * - eliminarPictogramaDeOracion(2)  // Elimina "Comer"
     * - Nueva oracion: [Yo, Quiero, Helado]
     */
    fun eliminarPictogramaDeOracion(indice: Int) {
        _estadoInterfaz.update { estadoActual ->
            estadoActual.copy(
                // Filtra todos los pictogramas excepto el del indice especificado
                oracionActual = estadoActual.oracionActual.filterIndexed { i, _ -> i != indice }
            )
        }
    }

    /**
     * Limpia completamente la oracion actual
     * Vuelve a mostrar los pictogramas mas usados
     */
    fun limpiarOracion() {
        _estadoInterfaz.update {
            it.copy(
                oracionActual = emptyList(),
                categoriaSeleccionada = null
            )
        }
        // Vuelve a cargar pictogramas desde los que ya están en memoria
        // (no desde Mock, para mantener los datos de la BD)
        cargarPictogramasMasUsadosDesdeMemoria()
    }

    // ====================================================================
    // SECCION: NAVEGACION POR CATEGORIAS
    // Funciones para filtrar pictogramas segun categoria
    // ====================================================================

    /**
     * Filtra los pictogramas por categoria
     *
     * @param categoria Categoria a filtrar (null = mostrar todos)
     *
     * CATEGORIAS DISPONIBLES:
     * - PERSONAS: Yo, Tu, Mama, Papa, etc.
     * - ACCIONES: Quiero, Tengo, Comer, Jugar, etc.
     * - COSAS: Helado, Agua, Comida, etc.
     * - CUALIDADES: Hambre, Sed, Feliz, Triste, etc.
     * - LUGARES: Casa, Escuela, Parque, etc.
     * - TIEMPO: Ahora, Despues, Manana, etc.
     *
     * FLUJO:
     * 1. Si categoria != null, filtra solo esa categoria
     * 2. Si categoria == null, muestra los mas usados
     * 3. Actualiza la interfaz con los pictogramas filtrados
     */
    fun seleccionarCategoria(categoria: Categoria?) {
        val todosPictogramas = _estadoInterfaz.value.todosPictogramas

        // Obtiene los pictogramas segun la categoria
        val pictogramas = if (todosPictogramas.isNotEmpty()) {
            // Usar los pictogramas de la base de datos (que ya están en memoria)
            if (categoria != null) {
                // Filtra por categoria especifica desde memoria
                todosPictogramas.filter { it.categoria == categoria }
            } else {
                // Muestra los primeros 20 desde memoria
                todosPictogramas.take(20)
            }
        } else {
            // Si no hay pictogramas en memoria, usar Mock (fallback)
            if (categoria != null) {
                FuenteDatosMock.obtenerPictogramasPorCategoria(categoria)
            } else {
                FuenteDatosMock.obtenerPictogramasMasUsados(20)
            }
        }

        // Actualiza el estado
        _estadoInterfaz.update {
            it.copy(
                categoriaSeleccionada = categoria,
                pictogramasDisponibles = pictogramas
            )
        }
    }

    /**
     * Carga los pictogramas mas frecuentemente usados
     * Usado cuando se limpia la oracion o se inicia la app
     */
    private fun cargarPictogramasMasUsados() {
        val pictogramas = FuenteDatosMock.obtenerPictogramasMasUsados(20)
        _estadoInterfaz.update {
            it.copy(pictogramasDisponibles = pictogramas)
        }
    }

    /**
     * Carga los pictogramas desde los que ya están en memoria (todosPictogramas)
     * Usado al limpiar la oración para no perder los datos de la BD
     */
    private fun cargarPictogramasMasUsadosDesdeMemoria() {
        val todosPictogramas = _estadoInterfaz.value.todosPictogramas
        if (todosPictogramas.isNotEmpty()) {
            // Usa los pictogramas que ya están cargados desde la BD
            _estadoInterfaz.update {
                it.copy(pictogramasDisponibles = todosPictogramas.take(20))
            }
        } else {
            // Si no hay pictogramas en memoria, cargar desde Mock
            cargarPictogramasMasUsados()
        }
    }

    // ====================================================================
    // SECCION: SISTEMA PREDICTIVO MEJORADO
    // Sugiere automaticamente la siguiente categoria segun contexto
    // ====================================================================

    /**
     * Sistema de prediccion inteligente MEJORADO
     * Sugiere que categoria mostrar despues de seleccionar un pictograma
     *
     * LOGICA DE PREDICCION AVANZADA:
     *
     * 1. CONTEXTUAL: Analiza los ultimos 2 pictogramas para entender mejor
     * 2. ESPECIFICA POR PALABRA: Ciertas acciones sugieren categorias especificas
     *    - "Voy" / "Ir al baño" -> LUGARES
     *    - "Comer" / "Beber" -> COSAS (comida/bebida)
     *    - "Jugar" -> COSAS (juguetes)
     *    - "Dormir" -> LUGARES (habitacion)
     * 3. ORDEN GRAMATICAL: Sigue estructura natural del español
     *    - PERSONA + ACCION + OBJETO/LUGAR
     *    - PERSONA + CUALIDAD + OBJETO
     * 4. COMPLETA ORACIONES: Sugiere TIEMPO cuando la oracion esta completa
     */
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
    // En el demo solo valida, en version completa guardaria en BD
    // ====================================================================

    /**
     * Simula el guardado de una oracion
     *
     * En el DEMO:
     * - Solo retorna true/false si se puede guardar
     * - NO guarda realmente (no hay base de datos)
     *
     * En la VERSION COMPLETA:
     * - Guardaria en Room Database
     * - Incrementaria contador de frecuencia
     * - Permitiria acceso rapido desde historial
     *
     * @return true si la oracion es valida para guardar
     */
    fun guardarOracion(): Boolean {
        return _estadoInterfaz.value.puedeGuardar
    }

    // ====================================================================
    // SECCION: CARGA DESDE BASE DE DATOS
    // Funciones para cargar pictogramas desde Room Database
    // ====================================================================

    /**
     * Carga pictogramas desde la base de datos Room
     * Convierte PictogramaEntity a PictogramaSimple
     *
     * @param pictogramasEntity Lista de pictogramas desde la BD
     */
    fun cargarPictogramasDesdeBaseDatos(pictogramasEntity: List<PictogramaEntity>) {
        // Convertir PictogramaEntity a PictogramaSimple usando el método toModel()
        // que mapea correctamente todos los campos incluyendo aprobado, creadoPor, etc.
        val pictogramasSimple = pictogramasEntity.map { entity ->
            entity.toModel()
        }

        // Actualizar estado con todos los pictogramas
        _estadoInterfaz.update {
            it.copy(
                todosPictogramas = pictogramasSimple,
                pictogramasDisponibles = pictogramasSimple.take(20) // Mostrar primeros 20
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
