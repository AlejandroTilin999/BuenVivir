package com.utom.buenvivir.data

import com.utom.buenvivir.data.db.MedicinaEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Cuándo toca una medicina: desde el día en que se agregó y, si es por un tiempo
 * (por ejemplo 7 días), hasta su último día. Las medicinas de antes no tienen fechas
 * y siguen tocando siempre, como hasta ahora.
 */

/** El último día de la medicina, o null si es para siempre. */
fun ultimoDia(inicio: String, dias: Int): LocalDate? =
    if (dias <= 0 || inicio.isEmpty()) null else LocalDate.parse(inicio).plusDays(dias - 1L)

fun MedicinaEntity.ultimoDia(): LocalDate? = ultimoDia(inicio, dias)

/** true si ya pasó su último día. */
fun MedicinaEntity.terminada(fecha: LocalDate): Boolean = ultimoDia()?.isBefore(fecha) == true

/**
 * true si esa pastilla cuenta ese día: no antes de que se agregara la medicina
 * (si la agregó a las 4 p. m., la de las 8 a. m. de hoy no cuenta) ni después de su último día.
 */
fun MedicinaEntity.tocaEn(fecha: LocalDate, minutos: Int): Boolean {
    if (inicio.isNotEmpty() && fecha.isBefore(LocalDate.parse(inicio))) return false
    if (terminada(fecha)) return false
    if (creadaEn > 0) {
        val momento = fecha.atTime(minutos / 60, minutos % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (momento < creadaEn) return false
    }
    return true
}

/** Primer día de una medicina nueva: hoy si todavía le falta alguna hora; si no, mañana. */
fun inicioPara(horarios: List<Int>, ahora: LocalDateTime = LocalDateTime.now()): String {
    val minutos = ahora.hour * 60 + ahora.minute
    val dia = if (horarios.any { it > minutos }) ahora.toLocalDate() else ahora.toLocalDate().plusDays(1)
    return dia.toString()
}

private val FORMATO_DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "MX"))

/** "hasta el 17 de noviembre" */
fun hastaTexto(fin: LocalDate): String = "hasta el ${fin.format(FORMATO_DIA)}"

/** "Por 7 días" / "Siempre" */
fun duracionTexto(dias: Int): String = when {
    dias <= 0 -> "Siempre"
    dias == 1 -> "Por 1 día"
    else -> "Por $dias días"
}
