package com.inacap.picto_comm.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla inicial (Splash Screen)
 * 
 * Verifica si existen usuarios y redirige:
 * - Si NO hay usuarios -> ConfiguracionInicialActivity
 * - Si hay usuarios -> SeleccionPerfilActivity
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val repository = (application as PictoCommApplication).repository

        lifecycleScope.launch {
            // Pequeño delay para mostrar el splash
            delay(1500)

            // Verificar si existen usuarios
            val existenUsuarios = repository.existenUsuarios()

            val intent = if (existenUsuarios) {
                // Ya hay usuarios, ir a selección de perfil
                Intent(this@SplashActivity, SeleccionPerfilActivity::class.java)
            } else {
                // No hay usuarios, ir a configuración inicial
                Intent(this@SplashActivity, ConfiguracionInicialActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }
}
