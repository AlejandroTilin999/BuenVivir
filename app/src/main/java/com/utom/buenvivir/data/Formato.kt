package com.utom.buenvivir.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private val ES_MX: Locale = Locale.forLanguageTag("es-MX")

/** 1200 -> "8:00 p. m." */
fun horaTexto(minutos: Int): String {
    val h = minutos / 60
    val m = minutos % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    val sufijo = if (h < 12) "a. m." else "p. m."
    return "%d:%02d %s".format(h12, m, sufijo)
}

/** "Hoy", "Ayer" o "Lunes 12". */
fun diaTexto(fecha: LocalDate, hoy: LocalDate): String = when (fecha) {
    hoy -> "Hoy"
    hoy.minusDays(1) -> "Ayer"
    else -> nombreDia(fecha) + " " + fecha.dayOfMonth
}

/** "Lunes", "Martes"... */
fun nombreDia(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES_MX).replaceFirstChar { it.uppercase() }

/** La hora real en que se registró algo: 1792... -> "10:15 p. m." */
fun horaDeRegistro(millis: Long): String {
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return horaTexto(t.hour * 60 + t.minute)
}

fun frecuenciaTexto(vecesAlDia: Int): String =
    if (vecesAlDia == 1) "1 vez al día" else "$vecesAlDia veces al día"
