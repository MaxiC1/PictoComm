package com.inacap.picto_comm.data.model

/**
 * Tipos de usuario en el sistema
 */
enum class TipoUsuario {
    PADRE,  // Administrador con acceso completo
    HIJO    // Usuario limitado, solo puede agregar pictogramas
}

/**
 * Modelo de datos para Usuario
 * Se almacena en Firestore en la colección "usuarios"
 */
@com.google.firebase.firestore.IgnoreExtraProperties
data class Usuario(
    val id: String = "",                    // ID único del usuario (Firebase Auth UID para padre, auto-generado para hijos)
    val nombre: String = "",                // Nombre del usuario
    val tipo: TipoUsuario = TipoUsuario.HIJO, // Tipo de usuario
    val pin: String = "",                   // PIN de 4 dígitos (DEPRECATED - usar Google Sign-In)
    val email: String = "",                 // Email (requerido para PADRE con Google)
    val photoUrl: String = "",              // URL de foto de perfil (de Google)
    val padreId: String = "",               // ID del padre (para usuarios HIJO)
    val activo: Boolean = true              // Si el usuario está activo
) {
    /**
     * Convierte el Usuario a un Map para Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "nombre" to nombre,
            "tipo" to tipo.name,
            "pin" to pin,
            "email" to email,
            "photoUrl" to photoUrl,
            "padreId" to padreId,
            "activo" to activo
        )
    }

    companion object {
        /**
         * Crea un Usuario desde un Map de Firestore
         */
        fun fromMap(map: Map<String, Any>): Usuario {
            return Usuario(
                id = map["id"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                tipo = try {
                    TipoUsuario.valueOf(map["tipo"] as? String ?: "HIJO")
                } catch (e: Exception) {
                    TipoUsuario.HIJO
                },
                pin = map["pin"] as? String ?: "",
                email = map["email"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                padreId = map["padreId"] as? String ?: "",
                activo = map["activo"] as? Boolean ?: true
            )
        }
    }

    /**
     * Verifica si el PIN proporcionado es correcto
     */
    fun verificarPin(pinIngresado: String): Boolean {
        return pin == pinIngresado
    }

    /**
     * Verifica si el usuario es padre/administrador
     */
    fun esPadre(): Boolean {
        return tipo == TipoUsuario.PADRE
    }

    /**
     * Verifica si el usuario es hijo
     */
    fun esHijo(): Boolean {
        return tipo == TipoUsuario.HIJO
    }
}
