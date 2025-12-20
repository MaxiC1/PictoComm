package com.inacap.picto_comm

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.ui.adapters.CategoriaAdapter
import com.inacap.picto_comm.ui.adapters.ItemCategoria
import com.inacap.picto_comm.ui.adapters.OracionAdapter
import com.inacap.picto_comm.ui.adapters.PictogramaAdapter
import com.inacap.picto_comm.ui.adapters.TipoItemCategoria
import com.inacap.picto_comm.ui.screens.CrearPictogramaActivity
import com.inacap.picto_comm.ui.screens.GestionarPictogramasActivity
import com.inacap.picto_comm.ui.screens.GestionarTodosPictogramasActivity
import com.inacap.picto_comm.ui.screens.SeleccionPerfilActivity
import com.inacap.picto_comm.ui.screens.SettingsActivity
import com.inacap.picto_comm.ui.viewmodel.ViewModelDemo
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Activity principal de PictoComm
 * Permite construir oraciones con pictogramas y reproducirlas con voz
 */
class MainActivity : AppCompatActivity() {

    // ViewModel
    private lateinit var viewModel: ViewModelDemo

    // Text-to-Speech
    private var tts: TextToSpeech? = null

    // Sensor de luz para ajuste automático de brillo
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var brightnessAdjustmentEnabled = true

    private val lightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (brightnessAdjustmentEnabled && it.sensor.type == Sensor.TYPE_LIGHT) {
                    val lux = it.values[0]
                    ajustarBrilloPantalla(lux)
                    actualizarIndicadorSensor(lux)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No se necesita implementación
        }
    }

    // Views - Toolbar
    private lateinit var toolbar: MaterialToolbar

    // Views - Barra de Oración
    private lateinit var tvTextoOracion: TextView
    private lateinit var btnLimpiar: ImageButton
    private lateinit var recyclerOracion: RecyclerView
    private lateinit var btnReproducir: MaterialButton
    private lateinit var btnGuardar: MaterialButton

    // Views - Grid de Pictogramas
    private lateinit var recyclerCategorias: RecyclerView
    private lateinit var recyclerPictogramas: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var fabCrearPictograma: FloatingActionButton
    private lateinit var tvSensorInfo: TextView

    // Adapters
    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var pictogramaAdapter: PictogramaAdapter
    private lateinit var oracionAdapter: OracionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar sesión activa
        val sessionManager = (application as PictoCommApplication).sessionManager
        if (!sessionManager.hayUsuarioActivo()) {
            // No hay sesión, redirigir a selección de perfil
            val intent = Intent(this, SeleccionPerfilActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this)[ViewModelDemo::class.java]

        // Inicializar vistas
        inicializarVistas()

        // Configurar RecyclerViews
        configurarRecyclerViews()

        // Configurar Text-to-Speech
        configurarTextToSpeech()

        // Observar cambios del ViewModel
        observarEstado()

        // Configurar Toolbar
        configurarToolbar()

        // Inicializar sensor de luz
        inicializarSensorDeLuz()

        // Cargar datos iniciales desde BD
        cargarPictogramasDesdeBaseDatos()
    }

    /**
     * Inicializa el sensor de luz para ajuste automático de brillo
     */
    private fun inicializarSensorDeLuz() {
        val sessionManager = (application as PictoCommApplication).sessionManager
        brightnessAdjustmentEnabled = sessionManager.obtenerBrilloAutomatico()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor == null) {
            brightnessAdjustmentEnabled = false
            tvSensorInfo.visibility = View.GONE
        } else {
            // Verificar permiso WRITE_SETTINGS
            if (brightnessAdjustmentEnabled && !Settings.System.canWrite(this)) {
                // Solicitar permiso
                solicitarPermisoModificarBrillo()
            }

            // Mostrar indicador solo si el brillo automático está habilitado
            tvSensorInfo.visibility = if (brightnessAdjustmentEnabled) View.VISIBLE else View.GONE
        }
    }

    /**
     * Solicita permiso para modificar el brillo del sistema
     */
    private fun solicitarPermisoModificarBrillo() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Para ajustar el brillo automáticamente, PictoComm necesita permiso para modificar la configuración del sistema.\n\n¿Deseas otorgar este permiso?")
            .setPositiveButton("Sí, otorgar") { dialog, _ ->
                try {
                    val intent = android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)

                    Toast.makeText(
                        this,
                        "Por favor, activa el permiso 'Modificar ajustes del sistema' y regresa a la app",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No se pudo abrir la configuración de permisos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                brightnessAdjustmentEnabled = false
                tvSensorInfo.visibility = View.GONE
                Toast.makeText(
                    this,
                    "El brillo automático está desactivado. Puedes activarlo desde Configuración.",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Ajusta el brillo de la pantalla según el nivel de luz ambiente
     */
    private fun ajustarBrilloPantalla(lux: Float) {
        try {
            // Verificar si podemos modificar el brillo del sistema
            if (!Settings.System.canWrite(this)) {
                android.util.Log.w("MainActivity", "No tiene permiso WRITE_SETTINGS")
                return
            }

            val layoutParams = window.attributes

            // Calcular brillo basado en lux (0.0 - 1.0)
            // Valores típicos de lux:
            // - 0-10: Muy oscuro
            // - 10-50: Poca luz
            // - 50-200: Luz interior normal
            // - 200-500: Luz brillante
            // - 500+: Muy brillante / Exterior
            val brightness = when {
                lux < 10 -> 0.2f          // Muy oscuro: brillo mínimo
                lux < 50 -> 0.4f           // Poca luz: brillo bajo
                lux < 200 -> 0.6f          // Normal: brillo medio
                lux < 500 -> 0.8f          // Brillante: brillo alto
                else -> 1.0f               // Muy brillante: brillo máximo
            }

            // Ajustar brillo solo si hay un cambio significativo
            if (kotlin.math.abs(layoutParams.screenBrightness - brightness) > 0.05f) {
                layoutParams.screenBrightness = brightness
                window.attributes = layoutParams
                android.util.Log.d("MainActivity", "Brillo ajustado a: ${(brightness * 100).toInt()}% (${lux.toInt()} lux)")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error al ajustar brillo", e)
        }
    }

    /**
     * Actualiza el indicador visual del sensor de luz
     */
    private fun actualizarIndicadorSensor(lux: Float) {
        val layoutParams = window.attributes
        val brightnessPercent = ((layoutParams.screenBrightness * 100).toInt())

        val condicion = when {
            lux < 10 -> "Muy Oscuro"
            lux < 50 -> "Poca Luz"
            lux < 200 -> "Normal"
            lux < 500 -> "Brillante"
            else -> "Muy Brillante"
        }

        tvSensorInfo.text = "💡 Sensor: ${lux.toInt()} lux\n🔆 Brillo: $brightnessPercent%\n📊 $condicion"
    }

    override fun onResume() {
        super.onResume()

        // Verificar si ahora tiene el permiso (después de ir a configuración)
        if (brightnessAdjustmentEnabled && !Settings.System.canWrite(this)) {
            // Actualizar indicador para mostrar que falta permiso
            tvSensorInfo.text = "⚠️ Falta permiso\nVe a Configuración\npara activar brillo\nautomático"
            tvSensorInfo.visibility = View.VISIBLE
        }

        // Registrar el sensor de luz
        if (brightnessAdjustmentEnabled && lightSensor != null && Settings.System.canWrite(this)) {
            sensorManager.registerListener(
                lightSensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el sensor de luz para ahorrar batería
        if (brightnessAdjustmentEnabled && lightSensor != null) {
            sensorManager.unregisterListener(lightSensorListener)
        }
    }

    /**
     * Inicializa todas las vistas referenciando los IDs del XML
     */
    private fun inicializarVistas() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar)

        // Barra de Oración
        tvTextoOracion = findViewById(R.id.tv_texto_oracion)
        btnLimpiar = findViewById(R.id.btn_limpiar)
        recyclerOracion = findViewById(R.id.recycler_oracion)
        btnReproducir = findViewById(R.id.btn_reproducir)
        btnGuardar = findViewById(R.id.btn_guardar)

        // Grid de Pictogramas
        recyclerCategorias = findViewById(R.id.recycler_categorias)
        recyclerPictogramas = findViewById(R.id.recycler_pictogramas)
        progressBar = findViewById(R.id.progress_bar)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        fabCrearPictograma = findViewById(R.id.fab_crear_pictograma)
        tvSensorInfo = findViewById(R.id.tv_sensor_info)
    }

    /**
     * Configura los tres RecyclerViews de la aplicación
     */
    private fun configurarRecyclerViews() {
        // 1. RecyclerView de Categorías (horizontal)
        categoriaAdapter = CategoriaAdapter { itemCategoria ->
            onCategoriaSeleccionada(itemCategoria)
        }
        recyclerCategorias.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoriaAdapter
        }

        // 2. RecyclerView de Pictogramas (grid adaptativo)
        pictogramaAdapter = PictogramaAdapter(
            onClickPictograma = { pictograma ->
                viewModel.anadirPictogramaAOracion(pictograma)
            },
            onLongClickPictograma = { pictograma ->
                viewModel.alternarFavorito(pictograma)
                mostrarMensajeFavorito(pictograma.esFavorito)
            }
        )
        recyclerPictogramas.apply {
            layoutManager = GridLayoutManager(this@MainActivity, calcularNumeroColumnas())
            adapter = pictogramaAdapter
        }

        // 3. RecyclerView de Oración (horizontal, compacto)
        oracionAdapter = OracionAdapter { indice ->
            viewModel.eliminarPictogramaDeOracion(indice)
        }
        recyclerOracion.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = oracionAdapter
        }
    }

    /**
     * Calcula el número de columnas del grid según el ancho de pantalla
     */
    private fun calcularNumeroColumnas(): Int {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val columnWidthDp = 120 + 16 // 120dp del item + 16dp de margen
        return (dpWidth / columnWidthDp).toInt().coerceAtLeast(2)
    }

    /**
     * Configura el motor de Text-to-Speech en español
     */
    private fun configurarTextToSpeech() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag("es-ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "El idioma español no está disponible", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error al inicializar Text-to-Speech", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar botón reproducir
        btnReproducir.setOnClickListener {
            reproducirOracion()
        }

        // Configurar botón guardar
        btnGuardar.setOnClickListener {
            guardarOracion()
        }

        // Configurar botón limpiar
        btnLimpiar.setOnClickListener {
            viewModel.limpiarOracion()
        }

        // Configurar FAB crear pictograma
        fabCrearPictograma.setOnClickListener {
            val intent = Intent(this, CrearPictogramaActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Reproduce la oración actual usando Text-to-Speech
     */
    private fun reproducirOracion() {
        lifecycleScope.launch {
            viewModel.estadoInterfaz.value.let { estado ->
                if (estado.textoOracion.isNotEmpty()) {
                    tts?.speak(estado.textoOracion, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    /**
     * Guarda la oración actual en Firebase
     */
    private fun guardarOracion() {
        val (puedeGuardar, datos) = viewModel.guardarOracion()
        if (!puedeGuardar) {
            Toast.makeText(this, R.string.oracion_no_valida, Toast.LENGTH_SHORT).show()
            return
        }

        // Parsear datos: "id1,id2,id3|texto completo"
        val partes = datos.split("|")
        if (partes.size != 2) {
            Toast.makeText(this, "Error al preparar la oración", Toast.LENGTH_SHORT).show()
            return
        }

        val pictogramaIds = partes[0].split(",").filter { it.isNotEmpty() }
        val textoCompleto = partes[1]

        // Guardar en Firebase
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository
        lifecycleScope.launch {
            try {
                firebaseRepository.guardarOracion(pictogramaIds, textoCompleto)
                Toast.makeText(this@MainActivity, R.string.oracion_guardada, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity,
                    "Error al guardar oración: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Maneja la selección de una categoría
     */
    private fun onCategoriaSeleccionada(item: ItemCategoria) {
        when (item.tipo) {
            TipoItemCategoria.TODOS -> {
                viewModel.seleccionarCategoria(null)
                categoriaAdapter.seleccionarItem(item)
            }
            TipoItemCategoria.FAVORITOS -> {
                viewModel.cargarFavoritos()
                categoriaAdapter.seleccionarItem(item)
            }
            TipoItemCategoria.CATEGORIA -> {
                item.categoria?.let { categoria ->
                    viewModel.seleccionarCategoria(categoria)
                    categoriaAdapter.seleccionarItem(item)
                }
            }
        }
    }

    /**
     * Muestra mensaje al agregar/quitar favorito
     */
    private fun mostrarMensajeFavorito(esFavorito: Boolean) {
        val mensaje = if (esFavorito) {
            R.string.favorito_agregado
        } else {
            R.string.favorito_eliminado
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    /**
     * Observa los cambios del estado del ViewModel y actualiza la UI
     */
    private fun observarEstado() {
        lifecycleScope.launch {
            viewModel.estadoInterfaz.collect { estado ->
                // Actualizar texto de la oración
                tvTextoOracion.text = if (estado.textoOracion.isEmpty()) {
                    getString(R.string.construye_oracion)
                } else {
                    estado.textoOracion
                }

                // Mostrar/ocultar botón limpiar
                btnLimpiar.visibility = if (estado.oracionActual.isEmpty()) View.GONE else View.VISIBLE

                // Actualizar RecyclerView de oración
                oracionAdapter.submitList(estado.oracionActual)
                recyclerOracion.visibility = if (estado.oracionActual.isEmpty()) View.GONE else View.VISIBLE

                // Habilitar/deshabilitar botones
                btnReproducir.isEnabled = estado.puedeReproducir
                btnGuardar.isEnabled = estado.puedeGuardar

                // Actualizar grid de pictogramas
                pictogramaAdapter.submitList(estado.pictogramasDisponibles)

                // Mostrar empty state si no hay pictogramas
                if (estado.pictogramasDisponibles.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerPictogramas.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerPictogramas.visibility = View.VISIBLE
                }

                // Actualizar categoría seleccionada
                estado.categoriaSeleccionada?.let { categoria ->
                    categoriaAdapter.seleccionarCategoria(categoria)
                }
            }
        }
    }

    /**
     * Carga pictogramas desde Firebase Firestore
     */
    private fun cargarPictogramasDesdeBaseDatos() {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository
        val sessionManager = (application as PictoCommApplication).sessionManager

        lifecycleScope.launch {
            try {
                // Obtener usuario actual
                val tipoUsuario = sessionManager.obtenerTipoUsuario()

                // Observar pictogramas en tiempo real desde Firebase
                val flowPictogramas = if (tipoUsuario == TipoUsuario.PADRE.name) {
                    // PADRE ve todos los pictogramas
                    firebaseRepository.obtenerTodosPictogramas()
                } else {
                    // HIJO solo ve pictogramas aprobados
                    firebaseRepository.obtenerPictogramasAprobados()
                }

                // Recopilar Flow y actualizar ViewModel
                flowPictogramas.collect { pictogramas ->
                    // Cargar pictogramas en ViewModel
                    viewModel.cargarPictogramasDesdeBaseDatos(pictogramas)
                }

            } catch (e: Exception) {
                // Solo mostrar error si no es una cancelación normal
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(this@MainActivity,
                        "Error al cargar pictogramas: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Configura el Toolbar
     */
    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        
        // Mostrar nombre del usuario en el toolbar
        val sessionManager = (application as PictoCommApplication).sessionManager
        val nombreUsuario = sessionManager.obtenerNombreUsuario()
        supportActionBar?.subtitle = "Usuario: $nombreUsuario"
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Mostrar opciones de administración solo para PADRE
        val sessionManager = (application as PictoCommApplication).sessionManager
        val esPadre = sessionManager.obtenerTipoUsuario() == TipoUsuario.PADRE.name

        val menuGestionar = menu?.findItem(R.id.action_gestionar_pictogramas)
        menuGestionar?.isVisible = esPadre

        val menuEditarEliminar = menu?.findItem(R.id.action_editar_eliminar_pictogramas)
        menuEditarEliminar?.isVisible = esPadre

        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gestionar_pictogramas -> {
                val intent = Intent(this, GestionarPictogramasActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_editar_eliminar_pictogramas -> {
                val intent = Intent(this, GestionarTodosPictogramasActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_historial -> {
                Toast.makeText(this, "Historial (próximamente)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_info -> {
                mostrarDialogoInfo()
                true
            }
            R.id.action_cambiar_usuario -> {
                cambiarUsuario()
                true
            }
            R.id.action_configuracion -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Permite cambiar de usuario
     */
    private fun cambiarUsuario() {
        val sessionManager = (application as PictoCommApplication).sessionManager
        sessionManager.cerrarSesion()
        
        val intent = Intent(this, SeleccionPerfilActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Muestra un diálogo con información de la app
     */
    private fun mostrarDialogoInfo() {
        val sessionManager = (application as PictoCommApplication).sessionManager
        val tipoUsuario = sessionManager.obtenerTipoUsuario()
        val nombreUsuario = sessionManager.obtenerNombreUsuario()
        
        val mensaje = if (tipoUsuario == TipoUsuario.PADRE.name) {
            "Usuario: $nombreUsuario (Administrador)\n\n" +
            "Características:\n" +
            "• Sistema de comunicación con pictogramas\n" +
            "• Sistema predictivo inteligente\n" +
            "• Text-to-Speech en español\n" +
            "• Control parental de pictogramas\n" +
            "• Base de datos Room local\n\n" +
            "Como administrador puedes gestionar todos los pictogramas."
        } else {
            "Usuario: $nombreUsuario\n\n" +
            "Características:\n" +
            "• Sistema de comunicación con pictogramas\n" +
            "• Sistema predictivo inteligente\n" +
            "• Text-to-Speech en español\n" +
            "• Pictogramas aprobados por tu padre/madre\n\n" +
            "Puedes agregar nuevos pictogramas que serán revisados."
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("PictoComm")
            .setMessage(mensaje)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        // Liberar recursos de Text-to-Speech
        tts?.shutdown()
        super.onDestroy()
    }
}
