package com.inacap.picto_comm.data.model

/**
 * Categorias gramaticales para organizar los pictogramas
 *
 * PROPOSITO:
 * - Facilitar la busqueda de pictogramas
 * - Identificar visualmente por color
 * - Estructurar oraciones gramaticalmente correctas
 *
 * CATEGORIAS DISPONIBLES:
 * 1. PERSONAS (Verde): Sujetos -> Yo, Tu, Mama, Papa
 * 2. ACCIONES (Azul): Verbos -> Quiero, Tengo, Comer, Jugar
 * 3. COSAS (Amarillo): Objetos -> Helado, Agua, Comida, Juguete
 * 4. CUALIDADES (Naranja): Adjetivos/Estados -> Hambre, Sed, Feliz, Triste
 * 5. LUGARES (Morado): Ubicaciones -> Casa, Escuela, Parque
 * 6. TIEMPO (Cian): Temporales -> Ahora, Despues, Manana, Hoy
 *
 * CODIFICACION POR COLORES:
 * Cada categoria tiene un color distintivo para:
 * - Facilitar identificacion visual rapida
 * - Ayudar a usuarios con dificultades de lectura
 * - Crear una interfaz mas intuitiva
 */
enum class Categoria(
    val nombreMostrar: String,  // Nombre a mostrar en la UI
    val color: Long             // Color en formato hexadecimal (0xFFRRGGBB)
) {
    // Verde - Sujetos y personas que realizan acciones
    PERSONAS("Personas", 0xFF4CAF50),

    // Azul - Verbos y acciones que se pueden realizar
    ACCIONES("Acciones", 0xFF2196F3),

    // Amarillo - Objetos y sustantivos
    COSAS("Cosas", 0xFFFFC107),

    // Naranja - Adjetivos, estados emocionales y cualidades
    CUALIDADES("Cualidades", 0xFFFF5722),

    // Morado - Ubicaciones y lugares
    LUGARES("Lugares", 0xFF9C27B0),

    // Cian - Indicadores temporales
    TIEMPO("Tiempo", 0xFF00BCD4);

    companion object {
        /**
         * Convierte un String al enum Categoria correspondiente
         *
         * EJEMPLO:
         * Categoria.desdeTexto("PERSONAS") -> Categoria.PERSONAS
         * Categoria.desdeTexto("INVALIDO") -> Categoria.COSAS (por defecto)
         *
         * @param valor Nombre de la categoria como String
         * @return Enum Categoria correspondiente, COSAS si no se encuentra
         */
        fun desdeTexto(valor: String): Categoria {
            return values().find { it.name == valor } ?: COSAS
        }
    }
}
