package com.inacap.picto_comm.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.ui.adapters.PictogramaGestionAdapter
import com.inacap.picto_comm.ui.utils.PinHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Actividad para gestionar todos los pictogramas (editar/eliminar)
 * Solo accesible por usuarios PADRE con verificación de PIN
 */
class GestionarTodosPictogramasActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerTodosPictogramas: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: PictogramaGestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestionar_todos_pictogramas)

        // Verificar que sea usuario PADRE
        val sessionManager = (application as PictoCommApplication).sessionManager
        if (sessionManager.obtenerTipoUsuario() != TipoUsuario.PADRE.name) {
            Toast.makeText(this, "Solo el administrador puede acceder a esta pantalla", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarVistas()
        configurarToolbar()
        configurarRecyclerView()

        // Verificar PIN antes de continuar
        verificarPinYCargarDatos()
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        recyclerTodosPictogramas = findViewById(R.id.recycler_todos_pictogramas)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configurarRecyclerView() {
        adapter = PictogramaGestionAdapter(
            onEditar = { pictograma ->
                mostrarDialogoEditar(pictograma)
            },
            onEliminar = { pictograma ->
                mostrarDialogoConfirmacionEliminar(pictograma)
            }
        )

        recyclerTodosPictogramas.apply {
            layoutManager = LinearLayoutManager(this@GestionarTodosPictogramasActivity)
            adapter = this@GestionarTodosPictogramasActivity.adapter
        }
    }

    /**
     * Verifica el PIN del padre antes de mostrar los datos
     */
    private fun verificarPinYCargarDatos() {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                val padreId = firebaseRepository.auth.currentUser?.uid
                if (padreId == null) {
                    Toast.makeText(this@GestionarTodosPictogramasActivity,
                        "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val usuario = firebaseRepository.obtenerUsuario(padreId)
                val pinCorrecto = usuario?.pin ?: ""

                if (pinCorrecto.isEmpty()) {
                    // No tiene PIN configurado, ofrecer configurarlo o continuar
                    mostrarDialogoConfigurarPin(padreId)
                } else {
                    val pinVerificado = PinHelper.verificarPin(this@GestionarTodosPictogramasActivity, pinCorrecto)
                    if (pinVerificado) {
                        cargarTodosPictogramas()
                    } else {
                        Toast.makeText(this@GestionarTodosPictogramasActivity,
                            "Acceso denegado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@GestionarTodosPictogramasActivity,
                    "Error al verificar PIN: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Muestra un diálogo para configurar el PIN si no existe
     */
    private fun mostrarDialogoConfigurarPin(padreId: String) {
        AlertDialog.Builder(this)
            .setTitle("PIN no configurado")
            .setMessage("No tienes un PIN configurado. ¿Deseas configurar uno ahora para mayor seguridad?")
            .setPositiveButton("Configurar PIN") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    configurarPinYContinuar(padreId)
                }
            }
            .setNegativeButton("Continuar sin PIN") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this@GestionarTodosPictogramasActivity,
                    "Continuando sin PIN. Puedes configurarlo más tarde.",
                    Toast.LENGTH_SHORT).show()
                cargarTodosPictogramas()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Configura el PIN y continúa con la carga de datos
     */
    private suspend fun configurarPinYContinuar(padreId: String) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        val nuevoPin = PinHelper.solicitarConfiguracionPin(this)
        if (nuevoPin != null) {
            try {
                // Actualizar el PIN en Firestore
                firebaseRepository.firestore.collection("usuarios")
                    .document(padreId)
                    .update("pin", nuevoPin)
                    .await()

                Toast.makeText(this,
                    "PIN configurado exitosamente", Toast.LENGTH_SHORT).show()
                cargarTodosPictogramas()
            } catch (e: Exception) {
                Toast.makeText(this,
                    "Error al guardar PIN: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this,
                "Configuración de PIN cancelada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cargarTodosPictogramas() {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        progressBar.visibility = View.VISIBLE
        recyclerTodosPictogramas.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            var primeraVez = true
            try {
                firebaseRepository.obtenerTodosPictogramas().collect { pictogramas ->
                    if (primeraVez) {
                        progressBar.visibility = View.GONE
                        primeraVez = false
                    }

                    if (pictogramas.isEmpty()) {
                        recyclerTodosPictogramas.visibility = View.GONE
                        layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        recyclerTodosPictogramas.visibility = View.VISIBLE
                        layoutEmptyState.visibility = View.GONE
                        adapter.submitList(pictogramas)
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@GestionarTodosPictogramasActivity,
                        "Error al cargar pictogramas: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun mostrarDialogoEditar(pictograma: PictogramaSimple) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_pictograma, null)

        val etTexto = dialogView.findViewById<EditText>(R.id.et_texto_pictograma)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinner_categoria)

        // Configurar valores actuales
        etTexto.setText(pictograma.texto)

        // Configurar spinner de categorías
        val categorias = Categoria.values()
        val categoriasNombres = categorias.map { it.nombreMostrar }
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriasNombres)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner
        spinnerCategoria.setSelection(categorias.indexOf(pictograma.categoria))

        AlertDialog.Builder(this)
            .setTitle("Editar Pictograma")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevoTexto = etTexto.text.toString()
                val nuevaCategoria = categorias[spinnerCategoria.selectedItemPosition]

                if (nuevoTexto.isBlank()) {
                    Toast.makeText(this, "El texto no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                editarPictograma(pictograma, nuevoTexto, nuevaCategoria)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun editarPictograma(pictograma: PictogramaSimple, nuevoTexto: String, nuevaCategoria: Categoria) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                val pictogramaActualizado = pictograma.copy(
                    texto = nuevoTexto,
                    categoria = nuevaCategoria
                )
                firebaseRepository.actualizarPictograma(pictogramaActualizado)
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Pictograma actualizado",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Error al actualizar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarDialogoConfirmacionEliminar(pictograma: PictogramaSimple) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Pictograma")
            .setMessage("¿Estás seguro de que deseas eliminar \"${pictograma.texto}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                eliminarPictograma(pictograma)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun eliminarPictograma(pictograma: PictogramaSimple) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                firebaseRepository.eliminarPictograma(pictograma.id)
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Pictograma eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Error al eliminar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
