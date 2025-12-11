package com.inacap.picto_comm

import android.app.Application
import com.inacap.picto_comm.data.database.PictoCommDatabase
import com.inacap.picto_comm.data.repository.RoomRepository
import com.inacap.picto_comm.data.repository.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Clase Application de PictoComm
 *
 * Se inicializa una sola vez cuando la aplicación arranca
 * Responsable de crear e inicializar:
 * - Room Database
 * - RoomRepository (con usuarios y pictogramas)
 * - SessionManager (gestión de sesión activa)
 * - CoroutineScope de la aplicación
 * 
 * ARQUITECTURA:
 * Application -> Database -> Repository -> ViewModel -> UI
 */
class PictoCommApplication : Application() {

    // CoroutineScope de la aplicación (vive mientras la app vive)
    private val applicationScope = CoroutineScope(SupervisorJob())

    // Database (lazy: se crea solo cuando se accede por primera vez)
    val database by lazy {
        PictoCommDatabase.getDatabase(this, applicationScope)
    }

    // Repositorio principal con Room Database
    val repository by lazy {
        RoomRepository(
            pictogramaDao = database.pictogramaDao(),
            oracionDao = database.oracionDao(),
            usuarioDao = database.usuarioDao()
        )
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
