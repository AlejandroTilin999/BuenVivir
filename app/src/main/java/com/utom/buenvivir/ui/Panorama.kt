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
import com.utom.buenvivir.data.hastaTexto
import com.utom.buenvivir.data.tocaEn
import com.utom.buenvivir.data.ultimoDia
import com.utom.buenvivir.ui.screens.EstadoToma
import com.utom.buenvivir.ui.screens.TomaDelDia
import java.time.LocalDate
import java.time.LocalDateTime

/** Minutos antes de la hora en que la pastilla ya aparece como "Ahora". */
private const val MARGEN_MINUTOS = 30

/** Minutos antes de la hora en que ya puede marcar "Tomar pastilla" o "No me la tomé". */
private const val MINUTOS_PARA_REGISTRAR = 5

/** Una pastilla sin registrar se queda arriba hasta 3 horas después de su hora (luego la alarma la marca sola). */
private const val MINUTOS_PENDIENTE = 180

/** Todo lo que muestran las pantallas, calculado con lo que hay en la base de datos. */
data class Panorama(
    val hoy: LocalDate,
    val horario: HorarioEntity,          // la toma que se muestra en Inicio
    val toma: Toma,
    val estado: EstadoToma,
    val horaTomada: String?,             // hora real en que registró la toma de Inicio
    val siguiente: ProximaToma,
    val siguientes: List<ProximaToma>,   // las que vienen después, en orden de hora (en círculo)
    val atrasada: Boolean,               // la toma de Inicio ya pasó hace más de 30 min y no se registra
    val fechaToma: LocalDate,            // el día de la toma de Inicio (hoy, o mañana si hoy ya no le toca nada)
    val manana: Boolean,                 // true si la toma de Inicio es de mañana
    val registrarDesde: String?,         // null = ya puede registrarla; si no, desde qué hora ("10:55 a. m.")
    val tomadasHoy: Int,
    val totalHoy: Int,
    val semana: List<DiaSemana>,
    val tomadasSemana: Int,
    val totalSemana: Int,
    val tomasHoy: List<TomaDelDia>,
    val medicinas: List<Medicina>
)

private data class HorarioDe(val medicina: MedicinaEntity, val horario: HorarioEntity)
private data class Evento(val fecha: LocalDate, val de: HorarioDe)

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
    val manana = hoy.plusDays(1)
    val minutosAhora = ahora.hour * 60 + ahora.minute
    val porClave = tomas.associateBy { it.horarioId to it.fecha }

    fun estadoDe(h: HorarioEntity, fecha: LocalDate): EstadoToma =
        porClave[h.id to fecha.toString()]?.let {
            if (it.tomada) EstadoToma.Tomada else EstadoToma.Omitida(it.motivo ?: "Otro motivo")
        } ?: EstadoToma.Pendiente

    // Solo cuentan las pastillas de ese día: no las de antes de agregar la medicina ni las de después de que termine
    fun delDia(fecha: LocalDate) = horarios.filter { it.medicina.tocaEn(fecha, it.horario.minutos) }
    val deHoy = delDia(hoy)

    // Las tomas de hoy y de mañana, en orden de hora
    val eventos = deHoy.map { Evento(hoy, it) } + delDia(manana).map { Evento(manana, it) }
    if (eventos.isEmpty()) return null

    // La toma de "ahora", entre las de hoy cuya hora ya llegó (o llega en 30 min):
    //  - si hay alguna sin registrar de las últimas 3 horas, la más temprana (así, si dos tocan a la misma hora
    //    o casi juntas, primero sale una y al registrarla sale la otra);
    //  - si no, la última.
    // Antes de la primera, la primera que sigue (de hoy, o de mañana si hoy ya no le toca nada).
    val llegaron = eventos.indices.filter { eventos[it].fecha == hoy && eventos[it].de.horario.minutos <= minutosAhora + MARGEN_MINUTOS }
    val sinRegistrar = llegaron.firstOrNull { i ->
        val e = eventos[i]
        e.de.horario.minutos >= minutosAhora - MINUTOS_PENDIENTE && estadoDe(e.de.horario, hoy) == EstadoToma.Pendiente
    }
    val indice = sinRegistrar ?: llegaron.lastOrNull() ?: 0
    val actualEvento = eventos[indice]
    val actual = actualEvento.de
    val fechaToma = actualEvento.fecha

    // Las que siguen, en orden de hora: las que faltan hoy y luego las de mañana.
    // 30 minutos antes de su hora, la primera de esta lista sube a la tarjeta de arriba (ver MARGEN_MINUTOS).
    val siguientes = eventos.drop(indice + 1).take(maxOf(1, horarios.size - 1)).map { e ->
        val cuando = if (e.fecha == hoy) "Hoy" else "Mañana"
        ProximaToma(e.de.medicina.nombre, "$cuando · ${horaTexto(e.de.horario.minutos)}")
    }
    val siguiente = siguientes.firstOrNull()
        ?: ProximaToma(actual.medicina.nombre, "Mañana · ${horaTexto(actual.horario.minutos)}")

    // Semana de lunes a domingo
    val lunes = hoy.minusDays(hoy.dayOfWeek.value - 1L)
    val letras = listOf("L", "M", "M", "J", "V", "S", "D")
    val semana = (0..6).map { i ->
        val fecha = lunes.plusDays(i.toLong())
        val del = delDia(fecha)
        val estado = when {
            fecha.isAfter(hoy) -> EstadoDia.Futuro
            fecha == hoy -> EstadoDia.Hoy
            del.isEmpty() -> EstadoDia.Futuro            // ese día todavía no tenía medicinas
            del.all { estadoDe(it.horario, fecha) == EstadoToma.Tomada } -> EstadoDia.Completo
            else -> EstadoDia.Falto
        }
        DiaSemana(letras[i], if (fecha == hoy) "Hoy" else nombreDia(fecha), estado)
    }
    val totalSemana = (0 until hoy.dayOfWeek.value).sumOf { delDia(lunes.plusDays(it.toLong())).size }
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
        estado = estadoDe(actual.horario, fechaToma),
        horaTomada = porClave[actual.horario.id to fechaToma.toString()]
            ?.takeIf { it.tomada }?.let { horaDeRegistro(it.registradaEn) },
        siguiente = siguiente,
        siguientes = siguientes,
        atrasada = fechaToma == hoy && actual.horario.minutos < minutosAhora - MARGEN_MINUTOS,
        fechaToma = fechaToma,
        manana = fechaToma != hoy,
        registrarDesde = (actual.horario.minutos - MINUTOS_PARA_REGISTRAR).let { desde ->
            if (fechaToma == hoy && minutosAhora >= desde) null else horaTexto(Math.floorMod(desde, 24 * 60))
        },
        tomadasHoy = deHoy.count { estadoDe(it.horario, hoy) == EstadoToma.Tomada },
        totalHoy = deHoy.size,
        semana = semana,
        tomadasSemana = tomadasSemana,
        totalSemana = totalSemana,
        tomasHoy = deHoy.map {
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
                frecuencia = frecuenciaTexto(m.horarios.size) +
                    (m.medicina.ultimoDia()?.let { fin -> " · ${hastaTexto(fin)}" } ?: ""),
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
