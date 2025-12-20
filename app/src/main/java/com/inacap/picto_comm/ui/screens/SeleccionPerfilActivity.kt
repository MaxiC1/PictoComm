package com.inacap.picto_comm.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.inacap.picto_comm.MainActivity
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.data.model.Usuario
import kotlinx.coroutines.launch

/**
 * Pantalla de selección de perfil
 *
 * Muestra lista de usuarios (PADRE + HIJOs)
 * - Al seleccionar HIJO -> Ir directo a MainActivity
 * - Al seleccionar PADRE -> Ir a GoogleSignInActivity (autenticación con Google)
 */
class SeleccionPerfilActivity : AppCompatActivity() {

    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var fabAgregarHijo: FloatingActionButton
    private lateinit var usuariosAdapter: UsuariosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_perfil)

        recyclerUsuarios = findViewById(R.id.recycler_usuarios)
        fabAgregarHijo = findViewById(R.id.fab_agregar_hijo)

        recyclerUsuarios.layoutManager = LinearLayoutManager(this)

        // FAB para agregar hijo (solo visible si el padre está autenticado)
        fabAgregarHijo.setOnClickListener {
            mostrarDialogoAgregarHijo()
        }

        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                android.util.Log.d("SeleccionPerfil", "Cargando usuarios desde Firebase...")
                val usuarios = firebaseRepository.obtenerTodosUsuarios()
                android.util.Log.d("SeleccionPerfil", "Usuarios obtenidos: ${usuarios.size}")

                if (usuarios.isEmpty()) {
                    android.util.Log.w("SeleccionPerfil", "No hay usuarios en Firebase")
                    android.widget.Toast.makeText(
                        this@SeleccionPerfilActivity,
                        "No hay usuarios registrados. Creando usuario de prueba...",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    // Volver a GoogleSignInActivity para crear usuarios
                    finish()
                } else {
                    usuariosAdapter = UsuariosAdapter(usuarios) { usuario ->
                        onUsuarioSeleccionado(usuario)
                    }
                    recyclerUsuarios.adapter = usuariosAdapter
                    android.util.Log.d("SeleccionPerfil", "Adapter configurado con ${usuarios.size} usuarios")
                }
            } catch (e: Exception) {
                android.util.Log.e("SeleccionPerfil", "Error al cargar usuarios", e)
                android.widget.Toast.makeText(
                    this@SeleccionPerfilActivity,
                    "Error al cargar usuarios: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onUsuarioSeleccionado(usuario: Usuario) {
        val sessionManager = (application as PictoCommApplication).sessionManager

        if (usuario.tipo == TipoUsuario.HIJO) {
            // HIJO: Autenticar anónimamente en Firebase y luego ir a MainActivity
            val auth = FirebaseAuth.getInstance()

            // Si ya hay un usuario autenticado, usarlo; si no, autenticar anónimamente
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener {
                        android.util.Log.d("SeleccionPerfil", "Usuario HIJO autenticado anónimamente")
                        sessionManager.guardarUsuarioActivo(usuario.id, usuario.nombre, usuario.tipo.name)
                        sessionManager.guardarFirebaseUid(it.user?.uid ?: "")

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("SeleccionPerfil", "Error al autenticar anónimamente", e)
                        android.widget.Toast.makeText(
                            this,
                            "Error al iniciar sesión: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                // Ya hay un usuario autenticado
                sessionManager.guardarUsuarioActivo(usuario.id, usuario.nombre, usuario.tipo.name)
                sessionManager.guardarFirebaseUid(auth.currentUser?.uid ?: "")

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            // PADRE: Ir a autenticar con Google
            val intent = Intent(this, GoogleSignInActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Muestra diálogo para agregar un usuario hijo
     */
    private fun mostrarDialogoAgregarHijo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            android.widget.Toast.makeText(this,
                "Debes iniciar sesión como administrador primero",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this)
        input.hint = "Nombre del usuario"
        input.setPadding(50, 40, 50, 40)

        AlertDialog.Builder(this)
            .setTitle("Agregar Usuario Hijo")
            .setMessage("Ingresa el nombre del usuario hijo:")
            .setView(input)
            .setPositiveButton("Agregar") { dialog, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    agregarUsuarioHijo(nombre, currentUser.uid)
                } else {
                    android.widget.Toast.makeText(this,
                        "El nombre no puede estar vacío",
                        android.widget.Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Agrega un usuario hijo a Firestore
     */
    private fun agregarUsuarioHijo(nombre: String, padreId: String) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                android.util.Log.d("SeleccionPerfil", "Creando usuario hijo: $nombre")
                val resultado = firebaseRepository.crearUsuarioHijo(nombre, padreId)

                resultado.onSuccess { hijoId ->
                    android.util.Log.d("SeleccionPerfil", "✅ Usuario hijo creado: $hijoId")
                    android.widget.Toast.makeText(
                        this@SeleccionPerfilActivity,
                        "Usuario hijo agregado: $nombre",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // Recargar lista de usuarios
                    cargarUsuarios()
                }.onFailure { error ->
                    android.util.Log.e("SeleccionPerfil", "❌ Error al crear hijo", error)
                    android.widget.Toast.makeText(
                        this@SeleccionPerfilActivity,
                        "Error: ${error.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("SeleccionPerfil", "❌ Error inesperado", e)
                android.widget.Toast.makeText(
                    this@SeleccionPerfilActivity,
                    "Error inesperado: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Adapter para mostrar lista de usuarios
     */
    class UsuariosAdapter(
        private val usuarios: List<Usuario>,
        private val onUsuarioClick: (Usuario) -> Unit
    ) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_usuario, parent, false)
            return UsuarioViewHolder(view)
        }

        override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
            val usuario = usuarios[position]
            holder.bind(usuario)
        }

        override fun getItemCount(): Int = usuarios.size

        inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardUsuario: MaterialCardView = itemView.findViewById(R.id.card_usuario)
            private val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_usuario)
            private val tvTipo: TextView = itemView.findViewById(R.id.tv_tipo_usuario)

            fun bind(usuario: Usuario) {
                tvNombre.text = usuario.nombre
                tvTipo.text = if (usuario.tipo == TipoUsuario.PADRE) {
                    "👨 Administrador"
                } else {
                    "👶 Usuario"
                }

                cardUsuario.setOnClickListener {
                    onUsuarioClick(usuario)
                }
            }
        }
    }
}
