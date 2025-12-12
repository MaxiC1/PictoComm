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
 * Verifica si existen usuarios en Firebase y redirige:
 * - Si NO hay usuarios -> GoogleSignInActivity (padre debe registrarse primero)
 * - Si hay usuarios -> SeleccionPerfilActivity (elegir padre o hijo)
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            // Pequeño delay para mostrar el splash
            delay(1500)

            // Verificar si existen usuarios en Firebase
            val existenUsuarios = firebaseRepository.existenUsuarios()

            val intent = if (existenUsuarios) {
                // Ya hay usuarios, ir a selección de perfil
                Intent(this@SplashActivity, SeleccionPerfilActivity::class.java)
            } else {
                // No hay usuarios, padre debe registrarse con Google Sign-In
                Intent(this@SplashActivity, GoogleSignInActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }
}
