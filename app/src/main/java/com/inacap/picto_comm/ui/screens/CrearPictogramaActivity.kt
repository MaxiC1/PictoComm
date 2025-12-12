package com.inacap.picto_comm.ui.screens

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.ui.utils.IconoHelper
import kotlinx.coroutines.launch

/**
 * Actividad para crear nuevos pictogramas
 * Accesible tanto para PADRE como para HIJO
 *
 * - PADRE: Los pictogramas se aprueban automáticamente
 * - HIJO: Los pictogramas quedan pendientes de aprobación
 */
class CrearPictogramaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etTexto: TextInputEditText
    private lateinit var btnCategoria: MaterialButton
    private lateinit var btnSeleccionarIcono: MaterialButton
    private lateinit var btnUsarCamara: MaterialButton
    private lateinit var btnCrear: MaterialButton
    private lateinit var ivVistaPrevia: ImageView

    private var categoriaSeleccionada: Categoria? = null
    private var iconoSeleccionado: String? = null

    // Iconos predefinidos disponibles para seleccionar
    private val iconosDisponibles = listOf(
        "ic_person_yo" to "Persona",
        "ic_action_want" to "Acción",
        "ic_thing_icecream" to "Objeto",
        "ic_quality_happy" to "Cualidad",
        "ic_place_home" to "Lugar",
        "ic_time_now" to "Tiempo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_pictograma)

        inicializarVistas()
        configurarToolbar()
        configurarListeners()
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        etTexto = findViewById(R.id.et_texto)
        btnCategoria = findViewById(R.id.btn_categoria)
        btnSeleccionarIcono = findViewById(R.id.btn_seleccionar_icono)
        btnUsarCamara = findViewById(R.id.btn_usar_camara)
        btnCrear = findViewById(R.id.btn_crear)
        ivVistaPrevia = findViewById(R.id.iv_vista_previa)
    }

    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configurarListeners() {
        btnCategoria.setOnClickListener {
            mostrarSelectorCategoria()
        }

        btnSeleccionarIcono.setOnClickListener {
            mostrarSelectorIcono()
        }

        btnUsarCamara.setOnClickListener {
            mostrarMensajeCamara()
        }

        btnCrear.setOnClickListener {
            crearPictograma()
        }
    }

    private fun mostrarSelectorCategoria() {
        val categorias = Categoria.values()
        val nombres = categorias.map { it.nombreMostrar }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.seleccionar_categoria))
            .setItems(nombres) { dialog, which ->
                categoriaSeleccionada = categorias[which]
                btnCategoria.text = categorias[which].nombreMostrar
                dialog.dismiss()
            }
            .show()
    }

    private fun mostrarSelectorIcono() {
        val nombres = iconosDisponibles.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.seleccionar_icono))
            .setItems(nombres) { dialog, which ->
                iconoSeleccionado = iconosDisponibles[which].first

                // Actualizar vista previa
                val iconoRes = IconoHelper.obtenerIconoParaPictograma(iconoSeleccionado!!)
                ivVistaPrevia.setImageResource(iconoRes)

                btnSeleccionarIcono.text = "Icono: ${iconosDisponibles[which].second}"
                dialog.dismiss()
            }
            .show()
    }

    private fun mostrarMensajeCamara() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.usar_camara))
            .setMessage(getString(R.string.camara_proximamente))
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun crearPictograma() {
        // Validar campos
        val texto = etTexto.text?.toString()?.trim()
        if (texto.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.error_texto_vacio), Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriaSeleccionada == null) {
            Toast.makeText(this, getString(R.string.error_categoria_no_seleccionada), Toast.LENGTH_SHORT).show()
            return
        }

        if (iconoSeleccionado == null) {
            Toast.makeText(this, getString(R.string.error_icono_no_seleccionado), Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener información del usuario
        val sessionManager = (application as PictoCommApplication).sessionManager
        val tipoUsuario = sessionManager.obtenerTipoUsuario()
        val usuarioId = sessionManager.obtenerUsuarioId()

        // Determinar si el pictograma se aprueba automáticamente
        val aprobadoAutomaticamente = tipoUsuario == TipoUsuario.PADRE.name

        // Crear el pictograma
        val nuevoPictograma = PictogramaSimple(
            id = "0", // Se generará automáticamente en la BD
            texto = texto,
            categoria = categoriaSeleccionada!!,
            recursoImagen = iconoSeleccionado!!,
            esFavorito = false,
            aprobado = aprobadoAutomaticamente,
            creadoPor = usuarioId.toString(),
            tipoImagen = TipoImagen.ICONO,
            urlImagen = "",
            fechaCreacion = System.currentTimeMillis()
        )

        // Guardar en Firebase Firestore
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository
        lifecycleScope.launch {
            try {
                firebaseRepository.crearPictograma(nuevoPictograma)

                if (aprobadoAutomaticamente) {
                    Toast.makeText(
                        this@CrearPictogramaActivity,
                        getString(R.string.pictograma_creado),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@CrearPictogramaActivity,
                        getString(R.string.pictograma_pendiente_aprobacion),
                        Toast.LENGTH_LONG
                    ).show()
                }

                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CrearPictogramaActivity,
                    "Error al crear pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
