package com.inacap.picto_comm.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.inacap.picto_comm.MainActivity
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.data.model.Usuario
import kotlinx.coroutines.launch

/**
 * Configuración inicial de la app (primera vez)
 * 
 * Permite crear:
 * - Usuario PADRE (con PIN)
 * - Usuario HIJO (sin PIN)
 */
class ConfiguracionInicialActivity : AppCompatActivity() {

    private lateinit var etNombrePadre: EditText
    private lateinit var etPinPadre: EditText
    private lateinit var etConfirmarPin: EditText
    private lateinit var etNombreHijo: EditText
    private lateinit var btnCrear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion_inicial)

        // Inicializar vistas
        etNombrePadre = findViewById(R.id.et_nombre_padre)
        etPinPadre = findViewById(R.id.et_pin_padre)
        etConfirmarPin = findViewById(R.id.et_confirmar_pin)
        etNombreHijo = findViewById(R.id.et_nombre_hijo)
        btnCrear = findViewById(R.id.btn_crear)

        btnCrear.setOnClickListener {
            crearUsuarios()
        }
    }

    private fun crearUsuarios() {
        val nombrePadre = etNombrePadre.text.toString().trim()
        val pin = etPinPadre.text.toString().trim()
        val confirmarPin = etConfirmarPin.text.toString().trim()
        val nombreHijo = etNombreHijo.text.toString().trim()

        // Validaciones
        if (nombrePadre.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del padre/madre", Toast.LENGTH_SHORT).show()
            return
        }

        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            Toast.makeText(this, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
            return
        }

        if (pin != confirmarPin) {
            Toast.makeText(this, "Los PINs no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (nombreHijo.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del hijo/a", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear usuarios
        val repository = (application as PictoCommApplication).repository
        val sessionManager = (application as PictoCommApplication).sessionManager

        lifecycleScope.launch {
            try {
                // Crear usuario PADRE
                val padre = Usuario(
                    nombre = nombrePadre,
                    tipo = TipoUsuario.PADRE,
                    pin = pin
                )
                val padreId = repository.crearUsuario(padre)

                // Crear usuario HIJO
                val hijo = Usuario(
                    nombre = nombreHijo,
                    tipo = TipoUsuario.HIJO
                )
                val hijoId = repository.crearUsuario(hijo)

                // Guardar sesión del hijo por defecto
                sessionManager.guardarUsuarioActivo(hijoId.toString(), nombreHijo, TipoUsuario.HIJO.name)

                Toast.makeText(this@ConfiguracionInicialActivity, "Usuarios creados exitosamente", Toast.LENGTH_SHORT).show()

                // Ir a MainActivity
                val intent = Intent(this@ConfiguracionInicialActivity, MainActivity::class.java)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionInicialActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
