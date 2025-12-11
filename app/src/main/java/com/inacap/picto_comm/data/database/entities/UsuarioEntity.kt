package com.inacap.picto_comm.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.data.model.Usuario

/**
 * Entidad de Room para almacenar usuarios en la base de datos local
 * 
 * TABLA: usuarios
 * 
 * CAMPOS:
 * - id: ID único del usuario (auto-generado)
 * - nombre: Nombre del usuario
 * - tipo: Tipo de usuario (PADRE o HIJO)
 * - pin: PIN de 4 dígitos (solo para PADRE)
 * - email: Email (opcional, para PADRE)
 * - fechaCreacion: Timestamp de creación
 * - activo: Si el usuario está activo
 */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: String, // "PADRE" o "HIJO"
    val pin: String = "",
    val email: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val activo: Boolean = true
) {
    /**
     * Convierte la entidad a modelo de dominio
     */
    fun toUsuario(): Usuario {
        return Usuario(
            id = id.toString(),
            nombre = nombre,
            tipo = try {
                TipoUsuario.valueOf(tipo)
            } catch (e: Exception) {
                TipoUsuario.HIJO
            },
            pin = pin,
            email = email,
            fechaCreacion = fechaCreacion,
            activo = activo
        )
    }

    companion object {
        /**
         * Crea una entidad desde el modelo de dominio
         */
        fun fromUsuario(usuario: Usuario): UsuarioEntity {
            return UsuarioEntity(
                id = usuario.id.toLongOrNull() ?: 0,
                nombre = usuario.nombre,
                tipo = usuario.tipo.name,
                pin = usuario.pin,
                email = usuario.email,
                fechaCreacion = usuario.fechaCreacion,
                activo = usuario.activo
            )
        }
    }
}
