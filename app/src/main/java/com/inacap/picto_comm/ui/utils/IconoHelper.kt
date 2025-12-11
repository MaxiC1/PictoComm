package com.inacap.picto_comm.ui.utils

import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.Categoria

/**
 * Helper object para mapear nombres de recursos de pictogramas a drawable resources
 *
 * Similar a IconosPictogramas.kt de Compose, pero para XML
 * Mapea los nombres de recursos (ic_person_yo, ic_action_want, etc.) a R.drawable.xxx
 *
 * IMPORTANTE: Muchos pictogramas comparten el mismo icono por simplicidad
 * En una versión completa, cada pictograma tendría su propio icono único
 */
object IconoHelper {

    /**
     * Obtiene el resource ID del drawable para un pictograma
     *
     * @param nombreRecurso El nombre del recurso desde PictogramaSimple.recursoImagen
     * @return El resource ID del drawable (ej: R.drawable.ic_person)
     */
    fun obtenerIconoParaPictograma(nombreRecurso: String): Int {
        return when (nombreRecurso) {
            // PERSONAS
            "ic_person_yo", "ic_person_tu", "ic_person_el_ella", "ic_person_nosotros",
            "ic_person_mama", "ic_person_papa", "ic_person_teacher", "ic_person_friend"
            -> R.drawable.ic_person

            // ACCIONES - Aquí usamos iconos genéricos por ahora
            "ic_action_want" -> R.drawable.ic_favorite
            "ic_action_have" -> R.drawable.ic_person
            "ic_action_need" -> R.drawable.ic_info
            "ic_action_like" -> R.drawable.ic_favorite
            "ic_action_go", "ic_action_eat", "ic_action_drink", "ic_action_play",
            "ic_action_sleep", "ic_action_bathroom", "ic_action_watch", "ic_action_listen"
            -> R.drawable.ic_category

            // COSAS
            "ic_thing_icecream", "ic_thing_water", "ic_thing_food", "ic_thing_toy",
            "ic_thing_book", "ic_thing_ball", "ic_thing_tv", "ic_thing_music",
            "ic_thing_tablet", "ic_thing_cookies"
            -> R.drawable.ic_category

            // CUALIDADES
            "ic_quality_hungry", "ic_quality_thirsty", "ic_quality_sleepy",
            "ic_quality_happy", "ic_quality_sad", "ic_quality_angry",
            "ic_quality_tired", "ic_quality_big", "ic_quality_small"
            -> R.drawable.ic_info

            // LUGARES
            "ic_place_home", "ic_place_school", "ic_place_park", "ic_place_hospital",
            "ic_place_bathroom", "ic_place_kitchen", "ic_place_bedroom"
            -> R.drawable.ic_person

            // TIEMPO
            "ic_time_now", "ic_time_later", "ic_time_tomorrow", "ic_time_today", "ic_time_yesterday"
            -> R.drawable.ic_history

            // Default
            else -> R.drawable.ic_category
        }
    }

    /**
     * Obtiene el resource ID del drawable para una categoría
     *
     * @param categoria La categoría (PERSONAS, ACCIONES, etc.)
     * @return El resource ID del drawable
     */
    fun obtenerIconoParaCategoria(categoria: Categoria): Int {
        return when (categoria) {
            Categoria.PERSONAS -> R.drawable.ic_person
            Categoria.ACCIONES -> R.drawable.ic_play
            Categoria.COSAS -> R.drawable.ic_category
            Categoria.CUALIDADES -> R.drawable.ic_favorite
            Categoria.LUGARES -> R.drawable.ic_person
            Categoria.TIEMPO -> R.drawable.ic_history
        }
    }
}
