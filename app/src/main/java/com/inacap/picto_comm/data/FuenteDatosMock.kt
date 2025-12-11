package com.inacap.picto_comm.data

import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple

/**
 * Fuente de datos SIMULADA (Mock) para el demo
 *
 * PROPOSITO:
 * - Proporcionar datos de ejemplo sin necesidad de base de datos
 * - Simular el comportamiento de una base de datos real
 * - Facilitar pruebas y demostraciones
 *
 * EN EL DEMO:
 * - Los datos estan precargados en codigo
 * - NO se guardan cambios al cerrar la app
 * - Todos los pictogramas son temporales
 *
 * EN LA VERSION COMPLETA:
 * - Esto se reemplazaria por Room Database
 * - Los datos persistirian entre sesiones
 * - Se podrian anadir/eliminar pictogramas dinamicamente
 *
 * CONTENIDO:
 * - 51 pictogramas precargados
 * - Distribuidos en 6 categorias
 * - Cubren necesidades basicas de comunicacion
 */
object FuenteDatosMock {

    /**
     * Retorna la lista completa de pictogramas del sistema
     *
     * DISTRIBUCION POR CATEGORIA:
     * - PERSONAS: 8 pictogramas
     * - ACCIONES: 12 pictogramas
     * - COSAS: 10 pictogramas
     * - CUALIDADES: 9 pictogramas
     * - LUGARES: 7 pictogramas
     * - TIEMPO: 5 pictogramas
     *
     * TOTAL: 51 pictogramas
     *
     * @return Lista de todos los pictogramas disponibles
     */
    fun obtenerTodosPictogramas(): List<PictogramaSimple> {
        return listOf(
            // ====================================================================
            // CATEGORIA: PERSONAS (Verde)
            // Sujetos que realizan acciones: Yo, Tu, Mama, Papa, etc.
            // ====================================================================
            PictogramaSimple(
                id = "1",
                texto = "Yo",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_yo"
            ),
            PictogramaSimple(
                id = "2",
                texto = "Tu",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_tu"
            ),
            PictogramaSimple(
                id = "3",
                texto = "El/Ella",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_el_ella"
            ),
            PictogramaSimple(
                id = "4",
                texto = "Nosotros",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_nosotros"
            ),
            PictogramaSimple(
                id = "5",
                texto = "Mama",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_mama"
            ),
            PictogramaSimple(
                id = "6",
                texto = "Papa",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_papa"
            ),
            PictogramaSimple(
                id = "7",
                texto = "Profesor/a",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_teacher"
            ),
            PictogramaSimple(
                id = "8",
                texto = "Amigo/a",
                categoria = Categoria.PERSONAS,
                recursoImagen = "ic_person_friend"
            ),

            // ====================================================================
            // CATEGORIA: ACCIONES (Azul)
            // Verbos y acciones: Quiero, Tengo, Comer, Jugar, etc.
            // ====================================================================
            PictogramaSimple(
                id = "9",
                texto = "Quiero",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_want"
            ),
            PictogramaSimple(
                id = "10",
                texto = "Tengo",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_have"
            ),
            PictogramaSimple(
                id = "11",
                texto = "Necesito",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_need"
            ),
            PictogramaSimple(
                id = "12",
                texto = "Me gusta",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_like"
            ),
            PictogramaSimple(
                id = "13",
                texto = "Voy",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_go"
            ),
            PictogramaSimple(
                id = "14",
                texto = "Comer",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_eat"
            ),
            PictogramaSimple(
                id = "15",
                texto = "Beber",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_drink"
            ),
            PictogramaSimple(
                id = "16",
                texto = "Jugar",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_play"
            ),
            PictogramaSimple(
                id = "17",
                texto = "Dormir",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_sleep"
            ),
            PictogramaSimple(
                id = "18",
                texto = "Ir al bano",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_bathroom"
            ),
            PictogramaSimple(
                id = "19",
                texto = "Ver",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_watch"
            ),
            PictogramaSimple(
                id = "20",
                texto = "Escuchar",
                categoria = Categoria.ACCIONES,
                recursoImagen = "ic_action_listen"
            ),

            // ====================================================================
            // CATEGORIA: COSAS (Amarillo)
            // Objetos y sustantivos: Helado, Agua, Comida, etc.
            // ====================================================================
            PictogramaSimple(
                id = "21",
                texto = "Helado",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_icecream"
            ),
            PictogramaSimple(
                id = "22",
                texto = "Agua",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_water"
            ),
            PictogramaSimple(
                id = "23",
                texto = "Comida",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_food"
            ),
            PictogramaSimple(
                id = "24",
                texto = "Juguete",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_toy"
            ),
            PictogramaSimple(
                id = "25",
                texto = "Libro",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_book"
            ),
            PictogramaSimple(
                id = "26",
                texto = "Pelota",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_ball"
            ),
            PictogramaSimple(
                id = "27",
                texto = "Television",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_tv"
            ),
            PictogramaSimple(
                id = "28",
                texto = "Musica",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_music"
            ),
            PictogramaSimple(
                id = "29",
                texto = "Tablet",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_tablet"
            ),
            PictogramaSimple(
                id = "30",
                texto = "Galletas",
                categoria = Categoria.COSAS,
                recursoImagen = "ic_thing_cookies"
            ),

            // ====================================================================
            // CATEGORIA: CUALIDADES (Naranja)
            // Adjetivos y estados: Hambre, Sed, Feliz, Triste, etc.
            // ====================================================================
            PictogramaSimple(
                id = "31",
                texto = "Hambre",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_hungry"
            ),
            PictogramaSimple(
                id = "32",
                texto = "Sed",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_thirsty"
            ),
            PictogramaSimple(
                id = "33",
                texto = "Sueno",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_sleepy"
            ),
            PictogramaSimple(
                id = "34",
                texto = "Feliz",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_happy"
            ),
            PictogramaSimple(
                id = "35",
                texto = "Triste",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_sad"
            ),
            PictogramaSimple(
                id = "36",
                texto = "Enojado/a",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_angry"
            ),
            PictogramaSimple(
                id = "37",
                texto = "Cansado/a",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_tired"
            ),
            PictogramaSimple(
                id = "38",
                texto = "Grande",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_big"
            ),
            PictogramaSimple(
                id = "39",
                texto = "Pequeno/a",
                categoria = Categoria.CUALIDADES,
                recursoImagen = "ic_quality_small"
            ),

            // ====================================================================
            // CATEGORIA: LUGARES (Morado)
            // Ubicaciones: Casa, Escuela, Parque, etc.
            // ====================================================================
            PictogramaSimple(
                id = "40",
                texto = "Casa",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_home"
            ),
            PictogramaSimple(
                id = "41",
                texto = "Escuela",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_school"
            ),
            PictogramaSimple(
                id = "42",
                texto = "Parque",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_park"
            ),
            PictogramaSimple(
                id = "43",
                texto = "Hospital",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_hospital"
            ),
            PictogramaSimple(
                id = "44",
                texto = "Bano",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_bathroom"
            ),
            PictogramaSimple(
                id = "45",
                texto = "Cocina",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_kitchen"
            ),
            PictogramaSimple(
                id = "46",
                texto = "Habitacion",
                categoria = Categoria.LUGARES,
                recursoImagen = "ic_place_bedroom"
            ),

            // ====================================================================
            // CATEGORIA: TIEMPO (Cian)
            // Indicadores temporales: Ahora, Despues, Manana, etc.
            // ====================================================================
            PictogramaSimple(
                id = "47",
                texto = "Ahora",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_now"
            ),
            PictogramaSimple(
                id = "48",
                texto = "Despues",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_later"
            ),
            PictogramaSimple(
                id = "49",
                texto = "Manana",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_tomorrow"
            ),
            PictogramaSimple(
                id = "50",
                texto = "Hoy",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_today"
            ),
            PictogramaSimple(
                id = "51",
                texto = "Ayer",
                categoria = Categoria.TIEMPO,
                recursoImagen = "ic_time_yesterday"
            )
        )
    }

    /**
     * Filtra pictogramas por categoria especifica
     *
     * @param categoria Categoria a filtrar (PERSONAS, ACCIONES, etc.)
     * @return Lista de pictogramas que pertenecen a esa categoria
     *
     * EJEMPLO:
     * obtenerPictogramasPorCategoria(Categoria.PERSONAS)
     * -> [Yo, Tu, Mama, Papa, ...]
     */
    fun obtenerPictogramasPorCategoria(categoria: Categoria): List<PictogramaSimple> {
        return obtenerTodosPictogramas().filter { it.categoria == categoria }
    }

    /**
     * Obtiene los pictogramas mas usados (simulado)
     *
     * En el DEMO: Retorna los primeros N pictogramas
     * En la VERSION COMPLETA: Ordenaria por contador de frecuencia real
     *
     * @param limite Numero maximo de pictogramas a retornar
     * @return Lista de pictogramas "mas usados"
     */
    fun obtenerPictogramasMasUsados(limite: Int = 20): List<PictogramaSimple> {
        return obtenerTodosPictogramas().take(limite)
    }

    /**
     * Filtra solo los pictogramas marcados como favoritos
     *
     * NOTA: En el demo los favoritos NO persisten al cerrar la app
     *
     * @param todosPictogramas Lista completa de pictogramas
     * @return Lista filtrada solo con favoritos
     */
    fun obtenerPictogramasFavoritos(todosPictogramas: List<PictogramaSimple>): List<PictogramaSimple> {
        return todosPictogramas.filter { it.esFavorito }
    }
}
