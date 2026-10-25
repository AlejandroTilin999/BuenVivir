package com.utom.buenvivir.ui

import com.utom.buenvivir.data.DiaSemana
import com.utom.buenvivir.data.EstadoDia
import com.utom.buenvivir.data.Medicina
import com.utom.buenvivir.data.ProximaToma
import com.utom.buenvivir.data.Toma
import com.utom.buenvivir.data.TomaPasada
import com.utom.buenvivir.data.db.HorarioEntity
import com.utom.buenvivir.data.db.MedicinaConHorarios
import com.utom.buenvivir.data.db.MedicinaEntity
import com.utom.buenvivir.data.db.TomaEntity
import com.utom.buenvivir.data.diaTexto
import com.utom.buenvivir.data.frecuenciaTexto
import com.utom.buenvivir.data.horaDeRegistro
import com.utom.buenvivir.data.horaTexto
import com.utom.buenvivir.data.nombreDia
import com.utom.buenvivir.ui.screens.EstadoToma
import com.utom.buenvivir.ui.screens.TomaDelDia
import java.time.LocalDate
import java.time.LocalDateTime

/** Minutos antes de la hora en que la pastilla ya aparece como "Ahora". */
private const val MARGEN_MINUTOS = 30

/** Todo lo que muestran las pantallas, calculado con lo que hay en la base de datos. */
data class Panorama(
    val hoy: LocalDate,
    val horario: HorarioEntity,          // la toma que se muestra en Inicio
    val toma: Toma,
    val estado: EstadoToma,
    val horaTomada: String?,             // hora real en que registró la toma de Inicio
    val siguiente: ProximaToma,
    val tomadasHoy: Int,
    val totalHoy: Int,
    val semana: List<DiaSemana>,
    val tomadasSemana: Int,
    val totalSemana: Int,
    val tomasHoy: List<TomaDelDia>,
    val medicinas: List<Medicina>
)

private data class HorarioDe(val medicina: MedicinaEntity, val horario: HorarioEntity)

fun calcularPanorama(
    meds: List<MedicinaConHorarios>,
    tomas: List<TomaEntity>,
    ahora: LocalDateTime
): Panorama? {
    val horarios = meds
        .flatMap { m -> m.horarios.map { HorarioDe(m.medicina, it) } }
        .sortedBy { it.horario.minutos }
    if (horarios.isEmpty()) return null

    val hoy = ahora.toLocalDate()
    val minutosAhora = ahora.hour * 60 + ahora.minute
    val porClave = tomas.associateBy { it.horarioId to it.fecha }

    fun estadoDe(h: HorarioEntity, fecha: LocalDate): EstadoToma =
        porClave[h.id to fecha.toString()]?.let {
            if (it.tomada) EstadoToma.Tomada else EstadoToma.Omitida(it.motivo ?: "Otro motivo")
        } ?: EstadoToma.Pendiente

    // La toma de "ahora": la última cuya hora ya llegó; antes de la primera del día, la primera.
    val indice = horarios.indexOfLast { it.horario.minutos <= minutosAhora + MARGEN_MINUTOS }.coerceAtLeast(0)
    val actual = horarios[indice]

    val siguiente = if (indice + 1 < horarios.size) {
        val s = horarios[indice + 1]
        ProximaToma(s.medicina.nombre, "Hoy · ${horaTexto(s.horario.minutos)}")
    } else {
        val s = horarios.first()
        ProximaToma(s.medicina.nombre, "Mañana · ${horaTexto(s.horario.minutos)}")
    }

    // Semana de lunes a domingo
    val lunes = hoy.minusDays(hoy.dayOfWeek.value - 1L)
    val letras = listOf("L", "M", "M", "J", "V", "S", "D")
    val semana = (0..6).map { i ->
        val fecha = lunes.plusDays(i.toLong())
        val estado = when {
            fecha.isAfter(hoy) -> EstadoDia.Futuro
            fecha == hoy -> EstadoDia.Hoy
            horarios.all { estadoDe(it.horario, fecha) == EstadoToma.Tomada } -> EstadoDia.Completo
            else -> EstadoDia.Falto
        }
        DiaSemana(letras[i], if (fecha == hoy) "Hoy" else nombreDia(fecha), estado)
    }
    val diasTranscurridos = (hoy.dayOfWeek.value)        // lunes = 1 ... domingo = 7
    val tomadasSemana = tomas.count { it.tomada && it.fecha >= lunes.toString() && it.fecha <= hoy.toString() }

    return Panorama(
        hoy = hoy,
        horario = actual.horario,
        toma = Toma(
            id = actual.horario.id.toInt(),
            medicina = actual.medicina.nombre,
            para = actual.medicina.para,
            hora = horaTexto(actual.horario.minutos),
            cantidad = actual.medicina.cantidad,
            indicacion = actual.horario.indicacion
        ),
        estado = estadoDe(actual.horario, hoy),
        horaTomada = porClave[actual.horario.id to hoy.toString()]
            ?.takeIf { it.tomada }?.let { horaDeRegistro(it.registradaEn) },
        siguiente = siguiente,
        tomadasHoy = horarios.count { estadoDe(it.horario, hoy) == EstadoToma.Tomada },
        totalHoy = horarios.size,
        semana = semana,
        tomadasSemana = tomadasSemana,
        totalSemana = horarios.size * diasTranscurridos,
        tomasHoy = horarios.map {
            val registro = porClave[it.horario.id to hoy.toString()]
            TomaDelDia(
                hora = horaTexto(it.horario.minutos),
                medicina = it.medicina.nombre,
                estado = estadoDe(it.horario, hoy),
                horaReal = registro?.takeIf { r -> r.tomada }?.let { r -> horaDeRegistro(r.registradaEn) }
            )
        },
        medicinas = meds.map { m ->
            Medicina(
                nombre = m.medicina.nombre,
                frecuencia = frecuenciaTexto(m.horarios.size),
                para = m.medicina.para,
                cantidad = m.medicina.cantidad,
                indicacion = m.medicina.indicacion,
                horarios = m.horarios.sortedBy { it.minutos }.map { horaTexto(it.minutos) },
                video = m.medicina.video,
                id = m.medicina.id
            )
        }
    )
}

/** Convierte las últimas tomas guardadas en filas para "Tus últimas tomas". */
fun historialDe(tomas: List<TomaEntity>, hoy: LocalDate): List<TomaPasada> = tomas.map {
    TomaPasada(
        dia = diaTexto(LocalDate.parse(it.fecha), hoy),
        hora = horaTexto(it.minutos),
        motivo = if (it.tomada) null else (it.motivo ?: "Sin motivo"),
        horaReal = if (it.tomada) horaDeRegistro(it.registradaEn) else null
    )
}

/** La semana cuando todavía no tiene medicinas: todos los días vacíos y hoy marcado. */
fun semanaVacia(hoy: java.time.LocalDate): List<DiaSemana> {
    val lunes = hoy.minusDays(hoy.dayOfWeek.value - 1L)
    val letras = listOf("L", "M", "M", "J", "V", "S", "D")
    return (0..6).map { i ->
        val fecha = lunes.plusDays(i.toLong())
        DiaSemana(letras[i], if (fecha == hoy) "Hoy" else nombreDia(fecha), if (fecha == hoy) EstadoDia.Hoy else EstadoDia.Futuro)
    }
}
