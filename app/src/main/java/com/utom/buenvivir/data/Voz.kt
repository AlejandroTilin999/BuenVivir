package com.utom.buenvivir.data

import java.text.Normalizer

/**
 * Entiende lo que el paciente dice en voz alta al agregar una medicina:
 * cuánto toma, cuándo y a qué hora. Si no entiende, regresa null y se le vuelve a preguntar.
 */
object Voz {

    private val NUMEROS = mapOf(
        "un" to 1, "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
        "trece" to 13, "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17,
        "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21, "veintidos" to 22,
        "veintitres" to 23, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50
    )

    /** Minúsculas, sin acentos y con los números escritos como dígitos ("ocho" -> "8"). */
    fun limpiar(texto: String): String {
        val t = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        return t.split(Regex("\\s+")).joinToString(" ") { p -> NUMEROS[p]?.toString() ?: p }
    }

    private fun primerNumero(t: String): Int? = Regex("\\d+").find(t)?.value?.toIntOrNull()

    /** "Sí", "claro", "otra" -> true; "no", "ya", "nada más" -> false; otra cosa -> null. */
    fun siONo(texto: String): Boolean? {
        val t = limpiar(texto)
        return when {
            Regex("\\b(no|ya|nada|ninguna|es todo|listo)\\b").containsMatchIn(t) -> false
            Regex("\\b(si|claro|otra|tambien|correcto)\\b").containsMatchIn(t) -> true
            else -> null
        }
    }

    /** "antes de comer", "con la comida" o "después de comer". */
    fun momento(texto: String): String? {
        val t = limpiar(texto)
        return when {
            "antes" in t || "ayunas" in t -> "antes de comer"
            "despues" in t -> "después de comer"
            Regex("\\b(con|durante|al comer|junto)\\b").containsMatchIn(t) -> "con la comida"
            else -> null
        }
    }

    /**
     * Cuántas pastillas: "media" -> "½ tableta", "una" -> "1 tableta", "dos" -> "2 tabletas",
     * "una y media" -> "1 ½ tabletas". Para la insulina, solo el número de unidades.
     */
    fun cantidad(texto: String, esInyeccion: Boolean): String? {
        val t = limpiar(texto)
        val n = primerNumero(t)
        if (esInyeccion) return n?.toString()
        val media = "media" in t || "medio" in t
        return when {
            n == null && media -> "½ tableta"
            n == null -> null
            media -> "$n ½ tabletas"
            n == 1 -> "1 tableta"
            else -> "$n tabletas"
        }
    }

    /** De las presentaciones ("500 mg", "850 mg"…), la que mencionó ("la de 850"). */
    fun presentacion(texto: String, opciones: List<String>): String? {
        val t = limpiar(texto).replace(".", "").replace(",", "")
        val numeros = Regex("\\d+").findAll(t).map { it.value }.toSet()
        return opciones.firstOrNull { op -> Regex("\\d+").findAll(op.replace(".", "")).any { it.value in numeros } }
    }

    /**
     * La hora en minutos desde la medianoche. Entiende "8 de la mañana", "a las 8 y media de la noche",
     * "20:00", "8:30 p. m.", "mediodía", "cuarto para las 9"… null si no la entiende.
     */
    fun hora(texto: String): Int? {
        val t = limpiar(texto)
        if ("mediodia" in t) return 12 * 60
        if ("medianoche" in t) return 0

        val reloj = Regex("(\\d{1,2})\\s*[:.]\\s*(\\d{2})").find(t)
        var h: Int
        var m = 0
        if (reloj != null) {
            h = reloj.groupValues[1].toInt()
            m = reloj.groupValues[2].toInt()
        } else {
            h = primerNumero(t) ?: return null
            when {
                "y media" in t -> m = 30
                "y cuarto" in t -> m = 15
                "menos cuarto" in t || "cuarto para" in t -> { h -= 1; m = 45 }
            }
        }

        val tarde = Regex("p\\.? ?m|tarde|noche").containsMatchIn(t)
        val manana = Regex("a\\.? ?m|manana|madrugada").containsMatchIn(t)
        when {
            tarde && h in 1..11 -> h += 12
            "noche" in t && h == 12 -> h = 0
            manana && h == 12 -> h = 0
        }
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
