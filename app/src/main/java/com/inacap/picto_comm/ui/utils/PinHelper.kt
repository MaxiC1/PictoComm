package com.inacap.picto_comm.ui.utils

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.inacap.picto_comm.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helper para manejar diálogos de PIN
 */
object PinHelper {

    /**
     * Muestra un diálogo para configurar un nuevo PIN
     * @return El PIN configurado o null si se cancela
     */
    suspend fun solicitarConfiguracionPin(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pin, null)

        val tvTitulo = view.findViewById<TextView>(R.id.tv_titulo_pin)
        val tvMensaje = view.findViewById<TextView>(R.id.tv_mensaje_pin)
        val etPin = view.findViewById<TextInputEditText>(R.id.et_pin)
        val tvError = view.findViewById<TextView>(R.id.tv_error_pin)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btn_cancelar_pin)
        val btnConfirmar = view.findViewById<MaterialButton>(R.id.btn_confirmar_pin)

        tvTitulo.text = "Configurar PIN de Seguridad"
        tvMensaje.text = "Configura un PIN de 4 dígitos para proteger las funciones de administrador"

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        // Validación en tiempo real
        etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = android.view.View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancelar.setOnClickListener {
            dialog.dismiss()
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }

        btnConfirmar.setOnClickListener {
            val pin = etPin.text.toString()

            when {
                pin.length != 4 -> {
                    tvError.text = "El PIN debe tener 4 dígitos"
                    tvError.visibility = android.view.View.VISIBLE
                }
                !pin.all { it.isDigit() } -> {
                    tvError.text = "El PIN solo debe contener números"
                    tvError.visibility = android.view.View.VISIBLE
                }
                else -> {
                    dialog.dismiss()
                    if (continuation.isActive) {
                        continuation.resume(pin)
                    }
                }
            }
        }

        dialog.setOnDismissListener {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Muestra un diálogo para verificar el PIN
     * @param pinCorrecto El PIN que debe coincidir
     * @return true si el PIN es correcto, false si se cancela o es incorrecto
     */
    suspend fun verificarPin(context: Context, pinCorrecto: String): Boolean = suspendCancellableCoroutine { continuation ->
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pin, null)

        val tvTitulo = view.findViewById<TextView>(R.id.tv_titulo_pin)
        val tvMensaje = view.findViewById<TextView>(R.id.tv_mensaje_pin)
        val etPin = view.findViewById<TextInputEditText>(R.id.et_pin)
        val tvError = view.findViewById<TextView>(R.id.tv_error_pin)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btn_cancelar_pin)
        val btnConfirmar = view.findViewById<MaterialButton>(R.id.btn_confirmar_pin)

        tvTitulo.text = "Verificación de Administrador"
        tvMensaje.text = "Ingresa tu PIN de 4 dígitos"

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        // Validación en tiempo real
        etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = android.view.View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancelar.setOnClickListener {
            dialog.dismiss()
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }

        btnConfirmar.setOnClickListener {
            val pin = etPin.text.toString()

            when {
                pin.length != 4 -> {
                    tvError.text = "El PIN debe tener 4 dígitos"
                    tvError.visibility = android.view.View.VISIBLE
                }
                pin != pinCorrecto -> {
                    tvError.text = "PIN incorrecto"
                    tvError.visibility = android.view.View.VISIBLE
                    // Limpiar el campo después de un error
                    etPin.text?.clear()
                }
                else -> {
                    dialog.dismiss()
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }
            }
        }

        dialog.setOnDismissListener {
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }

        continuation.invokeOnCancellation {
            dialog.dismiss()
        }

        dialog.show()
    }
}
