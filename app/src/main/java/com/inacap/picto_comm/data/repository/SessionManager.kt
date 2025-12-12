package com.inacap.picto_comm.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.data.model.Usuario

/**
 * Gestor de sesión de usuario
 * 
 * RESPONSABILIDADES:
 * - Guardar y recuperar el usuario activo
 * - Gestionar el estado de la sesión
 * - Almacenar preferencias del usuario
 * 
 * Usa SharedPreferences para persistir datos localmente
 */
class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "pictocomm_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FIRST_TIME = "first_time"
    }
    
    /**
     * Guarda la sesión del usuario
     */
    fun guardarSesion(usuario: Usuario) {
        prefs.edit().apply {
            putString(KEY_USER_ID, usuario.id)
            putString(KEY_USER_NAME, usuario.nombre)
            putString(KEY_USER_TYPE, usuario.tipo.name)
            putString(KEY_USER_EMAIL, usuario.email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * Guarda el usuario activo (versión simplificada)
     */
    fun guardarUsuarioActivo(id: String, nombre: String, tipo: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_TYPE, tipo)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Guarda el usuario activo con Long ID (versión sobrecargada)
     */
    fun guardarUsuarioActivo(userId: Long, nombre: String, tipo: String) {
        guardarUsuarioActivo(userId.toString(), nombre, tipo)
    }

    /**
     * Guarda el email del usuario
     */
    fun guardarEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    /**
     * Guarda el Firebase UID del usuario
     */
    fun guardarFirebaseUid(uid: String) {
        prefs.edit().putString("firebase_uid", uid).apply()
    }

    /**
     * Obtiene el Firebase UID del usuario
     */
    fun obtenerFirebaseUid(): String? {
        return prefs.getString("firebase_uid", null)
    }
    
    /**
     * Obtiene el ID del usuario activo
     */
    fun obtenerUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Obtiene el ID del usuario activo como Long
     */
    fun obtenerUsuarioId(): Long {
        val id = prefs.getString(KEY_USER_ID, "0") ?: "0"
        return id.toLongOrNull() ?: 0L
    }
    
    /**
     * Obtiene el nombre del usuario activo
     */
    fun obtenerUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
    
    /**
     * Obtiene el nombre del usuario activo (alias)
     */
    fun obtenerNombreUsuario(): String {
        return prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
    }
    
    /**
     * Obtiene el tipo de usuario activo
     */
    fun obtenerUserType(): TipoUsuario? {
        val typeString = prefs.getString(KEY_USER_TYPE, null) ?: return null
        return try {
            TipoUsuario.valueOf(typeString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Obtiene el tipo de usuario activo como String
     */
    fun obtenerTipoUsuario(): String {
        return prefs.getString(KEY_USER_TYPE, TipoUsuario.HIJO.name) ?: TipoUsuario.HIJO.name
    }
    
    /**
     * Obtiene el email del usuario activo
     */
    fun obtenerUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Verifica si hay una sesión activa
     */
    fun haySesionActiva(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Verifica si hay un usuario activo (alias)
     */
    fun hayUsuarioActivo(): Boolean {
        return haySesionActiva()
    }
    
    /**
     * Verifica si el usuario activo es padre
     */
    fun esUsuarioPadre(): Boolean {
        return obtenerUserType() == TipoUsuario.PADRE
    }
    
    /**
     * Verifica si el usuario activo es hijo
     */
    fun esUsuarioHijo(): Boolean {
        return obtenerUserType() == TipoUsuario.HIJO
    }
    
    /**
     * Cierra la sesión actual
     */
    fun cerrarSesion() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_TYPE)
            remove(KEY_USER_EMAIL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    
    /**
     * Verifica si es la primera vez que se abre la app
     */
    fun esPrimeraVez(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }
    
    /**
     * Marca que ya no es la primera vez
     */
    fun marcarNoEsPrimeraVez() {
        prefs.edit().putBoolean(KEY_FIRST_TIME, false).apply()
    }
    
    /**
     * Limpia todos los datos de la sesión
     */
    fun limpiarTodo() {
        prefs.edit().clear().apply()
    }
}
