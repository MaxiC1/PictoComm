package com.inacap.picto_comm.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.ui.adapters.PictogramaPendienteAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Actividad para gestionar pictogramas pendientes de aprobación
 * Solo accesible por usuarios PADRE
 */
class GestionarPictogramasActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerPendientes: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: PictogramaPendienteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestionar_pictogramas)

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
        cargarPictogramasPendientes()
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        recyclerPendientes = findViewById(R.id.recycler_pendientes)
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
        adapter = PictogramaPendienteAdapter(
            onAprobar = { pictograma ->
                aprobarPictograma(pictograma)
            },
            onRechazar = { pictograma ->
                mostrarDialogoConfirmacionRechazo(pictograma)
            },
            obtenerNombreUsuario = { usuarioId ->
                obtenerNombreUsuarioPorId(usuarioId)
            }
        )

        recyclerPendientes.apply {
            layoutManager = LinearLayoutManager(this@GestionarPictogramasActivity)
            adapter = this@GestionarPictogramasActivity.adapter
        }
    }

    private fun cargarPictogramasPendientes() {
        val repository = (application as PictoCommApplication).repository

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            recyclerPendientes.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE

            repository.obtenerPictogramasPendientes().collect { pictogramas ->
                progressBar.visibility = View.GONE

                if (pictogramas.isEmpty()) {
                    recyclerPendientes.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                } else {
                    recyclerPendientes.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                    adapter.submitList(pictogramas)
                }
            }
        }
    }

    private fun aprobarPictograma(pictograma: PictogramaSimple) {
        val repository = (application as PictoCommApplication).repository

        lifecycleScope.launch {
            try {
                repository.aprobarPictograma(pictograma.id)
                Toast.makeText(
                    this@GestionarPictogramasActivity,
                    getString(R.string.pictograma_aprobado),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarPictogramasActivity,
                    "Error al aprobar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rechazarPictograma(pictograma: PictogramaSimple) {
        val repository = (application as PictoCommApplication).repository

        lifecycleScope.launch {
            try {
                repository.rechazarPictograma(pictograma.id)
                Toast.makeText(
                    this@GestionarPictogramasActivity,
                    getString(R.string.pictograma_rechazado),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarPictogramasActivity,
                    "Error al rechazar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarDialogoConfirmacionRechazo(pictograma: PictogramaSimple) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rechazar))
            .setMessage(getString(R.string.confirmar_rechazo))
            .setPositiveButton(getString(R.string.confirmar)) { dialog, _ ->
                rechazarPictograma(pictograma)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun obtenerNombreUsuarioPorId(usuarioId: String): String {
        val repository = (application as PictoCommApplication).repository
        var nombre = "Usuario"

        lifecycleScope.launch {
            try {
                val usuario = repository.obtenerUsuario(usuarioId.toLongOrNull() ?: 0)
                nombre = usuario?.nombre ?: "Usuario"
            } catch (e: Exception) {
                nombre = "Usuario"
            }
        }

        return nombre
    }
}
