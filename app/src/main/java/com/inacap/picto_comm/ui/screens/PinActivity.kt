package com.inacap.picto_comm.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.inacap.picto_comm.MainActivity
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.TipoUsuario
import kotlinx.coroutines.launch

/**
 * Pantalla de verificación de PIN
 * 
 * Verifica el PIN del usuario PADRE antes de permitir acceso
 */
class PinActivity : AppCompatActivity() {

    private lateinit var tvTitulo: TextView
    private lateinit var etPin: TextInputEditText
    private lateinit var btnVerificar: Button
    private lateinit var btnCancelar: Button

    private var usuarioId: String = ""
    private var usuarioNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        // Obtener datos del intent
        usuarioId = intent.getStringExtra("USUARIO_ID") ?: ""
        usuarioNombre = intent.getStringExtra("USUARIO_NOMBRE") ?: ""

        // Inicializar vistas
        tvTitulo = findViewById(R.id.tv_titulo_pin)
        etPin = findViewById(R.id.et_pin)
        btnVerificar = findViewById(R.id.btn_verificar)
        btnCancelar = findViewById(R.id.btn_cancelar)

        tvTitulo.text = "Hola $usuarioNombre\nIngresa tu PIN"

        btnVerificar.setOnClickListener {
            verificarPin()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun verificarPin() {
        val pin = etPin.text.toString().trim()

        if (pin.length != 4) {
            Toast.makeText(this, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
            return
        }

        val repository = (application as PictoCommApplication).repository
        val sessionManager = (application as PictoCommApplication).sessionManager

        lifecycleScope.launch {
            try {
                val usuarioIdLong = usuarioId.toLongOrNull() ?: 0
                val usuario = repository.verificarPin(usuarioIdLong, pin)

                if (usuario != null) {
                    // PIN correcto
                    sessionManager.guardarUsuarioActivo(usuario.id, usuario.nombre, TipoUsuario.PADRE.name)
                    Toast.makeText(this@PinActivity, "Bienvenido/a", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@PinActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // PIN incorrecto
                    Toast.makeText(this@PinActivity, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                    etPin.text?.clear()
                    etPin.requestFocus()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PinActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
