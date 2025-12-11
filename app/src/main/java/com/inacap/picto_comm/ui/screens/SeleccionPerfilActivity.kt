package com.inacap.picto_comm.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
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
 * - Al seleccionar PADRE -> Ir a PinActivity
 */
class SeleccionPerfilActivity : AppCompatActivity() {

    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var usuariosAdapter: UsuariosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_perfil)

        recyclerUsuarios = findViewById(R.id.recycler_usuarios)
        recyclerUsuarios.layoutManager = LinearLayoutManager(this)

        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        val repository = (application as PictoCommApplication).repository

        lifecycleScope.launch {
            val usuarios = repository.obtenerTodosUsuarios()
            usuariosAdapter = UsuariosAdapter(usuarios) { usuario ->
                onUsuarioSeleccionado(usuario)
            }
            recyclerUsuarios.adapter = usuariosAdapter
        }
    }

    private fun onUsuarioSeleccionado(usuario: Usuario) {
        val sessionManager = (application as PictoCommApplication).sessionManager

        if (usuario.tipo == TipoUsuario.HIJO) {
            // HIJO: Ir directo a MainActivity
            sessionManager.guardarUsuarioActivo(usuario.id, usuario.nombre, usuario.tipo.name)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // PADRE: Ir a verificar PIN
            val intent = Intent(this, PinActivity::class.java)
            intent.putExtra("USUARIO_ID", usuario.id)
            intent.putExtra("USUARIO_NOMBRE", usuario.nombre)
            startActivity(intent)
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
