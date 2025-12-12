package com.inacap.picto_comm.data.model

/**
 * Modelo simplificado de un pictograma para la UI
 *
 * PROPOSITO:
 * - Representar un pictograma de forma ligera y eficiente
 * - Contener solo la informacion necesaria para mostrar en pantalla
 * - Facilitar la comunicacion entre capas de la app
 * - Compatible con Firebase Firestore
 *
 * CAMPOS:
 * - id: Identificador unico del pictograma
 * - texto: Palabra o frase que representa (ej: "Yo", "Quiero", "Helado")
 * - categoria: Categoria gramatical (PERSONAS, ACCIONES, COSAS, etc.)
 * - recursoImagen: Nombre del recurso drawable (ej: "ic_person_yo")
 * - esFavorito: Si el usuario lo marco como favorito
 * - aprobado: Si el pictograma fue aprobado por el padre (true por defecto para pictogramas del sistema)
 * - creadoPor: ID del usuario que creó el pictograma (vacío para pictogramas del sistema)
 * - tipoImagen: Tipo de imagen (ICONO o FOTO)
 * - urlImagen: URL de Firebase Storage (para fotos personalizadas)
 *
 * EJEMPLO DE USO:
 * val pictograma = PictogramaSimple(
 *     id = "1",
 *     texto = "Yo",
 *     categoria = Categoria.PERSONAS,
 *     recursoImagen = "ic_person_yo",
 *     esFavorito = false,
 *     aprobado = true,
 *     creadoPor = "",
 *     tipoImagen = TipoImagen.ICONO
 * )
 */

/**
 * Tipos de imagen para pictogramas
 */
enum class TipoImagen {
    ICONO,  // Icono del sistema (drawable)
    FOTO    // Foto personalizada (Firebase Storage)
}

data class PictogramaSimple(
    val id: String = "",                    // Cambiado de Long a String para Firebase
    val texto: String = "",
    val categoria: Categoria = Categoria.COSAS,
    val recursoImagen: String = "",
    var esFavorito: Boolean = false,
    val aprobado: Boolean = true,           // Por defecto aprobado (pictogramas del sistema)
    val creadoPor: String = "",             // ID del usuario creador (vacío = sistema)
    val tipoImagen: TipoImagen = TipoImagen.ICONO,
    val urlImagen: String = "",             // URL de Firebase Storage para fotos
    val fechaCreacion: Long = System.currentTimeMillis(),
    val frecuenciaUso: Int = 0,             // Contador de veces usado
    val padreId: String = ""                // ID del padre (para filtrar por familia)
) {
    /**
     * Convierte el pictograma a un Map para Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "texto" to texto,
            "categoria" to categoria.name,
            "recursoImagen" to recursoImagen,
            "esFavorito" to esFavorito,
            "aprobado" to aprobado,
            "creadoPor" to creadoPor,
            "tipoImagen" to tipoImagen.name,
            "urlImagen" to urlImagen,
            "fechaCreacion" to fechaCreacion,
            "frecuenciaUso" to frecuenciaUso,
            "padreId" to padreId
        )
    }

    companion object {
        /**
         * Crea un PictogramaSimple desde un Map de Firestore
         */
        fun fromMap(map: Map<String, Any>): PictogramaSimple {
            return PictogramaSimple(
                id = map["id"] as? String ?: "",
                texto = map["texto"] as? String ?: "",
                categoria = try {
                    Categoria.valueOf(map["categoria"] as? String ?: "COSAS")
                } catch (e: Exception) {
                    Categoria.COSAS
                },
                recursoImagen = map["recursoImagen"] as? String ?: "",
                esFavorito = map["esFavorito"] as? Boolean ?: false,
                aprobado = map["aprobado"] as? Boolean ?: true,
                creadoPor = map["creadoPor"] as? String ?: "",
                tipoImagen = try {
                    TipoImagen.valueOf(map["tipoImagen"] as? String ?: "ICONO")
                } catch (e: Exception) {
                    TipoImagen.ICONO
                },
                urlImagen = map["urlImagen"] as? String ?: "",
                fechaCreacion = map["fechaCreacion"] as? Long ?: System.currentTimeMillis(),
                frecuenciaUso = (map["frecuenciaUso"] as? Long)?.toInt() ?: 0,
                padreId = map["padreId"] as? String ?: ""
            )
        }
    }

    /**
     * Verifica si el pictograma es del sistema (no creado por usuario)
     */
    fun esPictogramaSistema(): Boolean {
        return creadoPor.isEmpty()
    }

    /**
     * Verifica si el pictograma está pendiente de aprobación
     */
    fun estaPendienteAprobacion(): Boolean {
        return !aprobado && creadoPor.isNotEmpty()
    }

    /**
     * Verifica si usa una foto personalizada
     */
    fun usaFotoPersonalizada(): Boolean {
        return tipoImagen == TipoImagen.FOTO && urlImagen.isNotEmpty()
    }
}
