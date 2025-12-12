package com.inacap.picto_comm.data.repository

import android.util.Log
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen

/**
 * Helper para migrar datos iniciales a Firebase
 *
 * Contiene funciones para popular Firebase Firestore con pictogramas del sistema
 */
class FirebaseMigrationHelper(
    private val firebaseRepository: FirebaseRepository
) {

    companion object {
        private const val TAG = "FirebaseMigrationHelper"
    }

    /**
     * Popula Firebase con pictogramas iniciales del sistema
     * Llamar esta función cuando el padre se registra por primera vez
     */
    suspend fun popularPictogramasIniciales(padreId: String): Result<Int> {
        return try {
            val pictogramasIniciales = obtenerPictogramasDelSistema(padreId)
            var contador = 0

            pictogramasIniciales.forEach { pictograma ->
                try {
                    firebaseRepository.crearPictograma(pictograma)
                    contador++
                    Log.d(TAG, "Pictograma creado: ${pictograma.texto}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al crear pictograma: ${pictograma.texto}", e)
                }
            }

            Log.i(TAG, "Migración completada: $contador pictogramas creados")
            Result.success(contador)
        } catch (e: Exception) {
            Log.e(TAG, "Error en migración", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene la lista de pictogramas del sistema
     * Estos son los pictogramas básicos que vienen con la app
     */
    private fun obtenerPictogramasDelSistema(padreId: String): List<PictogramaSimple> {
        return listOf(
            // ===== PERSONAS =====
            PictogramaSimple(
                texto = "Yo",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_yo",
                aprobado = true,
                creadoPor = "", // Sistema
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Tú",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Mamá",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Papá",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Hermano",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Amigo",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),

            // ===== ACCIONES =====
            PictogramaSimple(
                texto = "Quiero",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_want",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Comer",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_eat",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Beber",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_drink",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Jugar",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_play",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Dormir",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_sleep",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Ver",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_watch",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Ir",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_go",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Hacer",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_do",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Leer",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_read",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),

            // ===== COSAS =====
            PictogramaSimple(
                texto = "Agua",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_water",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Comida",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_food",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Juguete",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_toy",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Libro",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_book",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Televisión",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_tv",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Teléfono",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_phone",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Pelota",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_ball",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Cama",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_bed",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),

            // ===== CUALIDADES =====
            PictogramaSimple(
                texto = "Grande",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_big",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Pequeño",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_small",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Feliz",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_happy",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Triste",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_sad",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Bueno",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_good",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Bonito",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_pretty",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Nuevo",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_new",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),

            // ===== LUGARES =====
            PictogramaSimple(
                texto = "Casa",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_home",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Escuela",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_school",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Parque",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_park",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Baño",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_bathroom",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Cocina",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_kitchen",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Habitación",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_room",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),

            // ===== TIEMPO =====
            PictogramaSimple(
                texto = "Ahora",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_now",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Después",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_later",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Hoy",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_today",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Mañana",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_tomorrow",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Noche",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_night",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            ),
            PictogramaSimple(
                texto = "Día",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_day",
                aprobado = true,
                creadoPor = "",
                tipoImagen = TipoImagen.ICONO,
                padreId = padreId
            )
        )
    }
}
