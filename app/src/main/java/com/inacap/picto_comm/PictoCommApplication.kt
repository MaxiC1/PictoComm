package com.inacap.picto_comm

import android.app.Application
import com.inacap.picto_comm.data.repository.FirebaseRepository
import com.inacap.picto_comm.data.repository.SessionManager

/**
 * Clase Application de PictoComm
 *
 * Se inicializa una sola vez cuando la aplicación arranca
 * Responsable de crear e inicializar:
 * - Firebase (Auth, Firestore, Storage)
 * - FirebaseRepository (datos en la nube)
 * - SessionManager (gestión de sesión activa)
 *
 * ARQUITECTURA:
 * Application -> Firebase -> Repository -> ViewModel -> UI
 */
class PictoCommApplication : Application() {

    // Repositorio principal con Firebase
    val firebaseRepository by lazy {
        FirebaseRepository()
    }

    // Gestor de sesión (SharedPreferences)
    val sessionManager by lazy {
        SessionManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Aquí puedes agregar inicializaciones adicionales si es necesario
    }
}
