package com.utom.buenvivir.alarmas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.utom.buenvivir.data.RepositorioLocal
import com.utom.buenvivir.data.db.HorarioEntity
import java.time.LocalDate
import java.time.ZoneId

/** Programa las alarmas de las pastillas con AlarmManager. */
object Programador {
    const val EXTRA_HORARIO = "horario"
    const val EXTRA_FECHA = "fecha"
    const val EXTRA_INTENTO = "intento"
    const val EXTRA_PRUEBA = "prueba"

    /** El aviso se repite durante una hora; después se registra "No hubo respuesta". */
    const val MINUTOS_ESPERA = 60
    const val MOTIVO_SIN_RESPUESTA = "No hubo respuesta"

    /** Cuántas repeticiones caben en la hora de espera: cada 5 min = 12, cada 10 = 6, cada 15 = 4. */
    fun repeticiones(minutos: Int): Int = (MINUTOS_ESPERA / minutos.coerceAtLeast(1)).coerceAtLeast(1)

    // Códigos distintos para que una alarma no reemplace a otra
    private const val BASE_REPETICION = 100_000
    private const val BASE_PRUEBA = 200_000

    /** Programa la siguiente alarma de cada horario. Se llama al abrir la app y al prender el celular. */
    suspend fun programarTodas(context: Context) {
        RepositorioLocal.get(context).horarios().forEach { programarSiguiente(context, it) }
    }

    /** La próxima vez que toca este horario: hoy si todavía no pasa la hora, si no, mañana. */
    fun programarSiguiente(context: Context, h: HorarioEntity) {
        val hoy = LocalDate.now()
        val fecha = if (millisDe(hoy, h.minutos) > System.currentTimeMillis()) hoy else hoy.plusDays(1)
        programarPara(context, h, fecha)
    }

    fun programarPara(context: Context, h: HorarioEntity, fecha: LocalDate) {
        poner(context, millisDe(fecha, h.minutos), pendiente(context, h.id.toInt(), h.id, fecha, 0, false))
    }

    /** Vuelve a avisar en unos minutos si no respondió. */
    fun programarRepeticion(context: Context, horarioId: Long, fecha: LocalDate, intento: Int, minutos: Int) {
        poner(
            context,
            System.currentTimeMillis() + minutos * 60_000L,
            pendiente(context, BASE_REPETICION + horarioId.toInt(), horarioId, fecha, intento, false)
        )
    }

    fun cancelarRepeticion(context: Context, horarioId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pendiente(context, BASE_REPETICION + horarioId.toInt(), horarioId, LocalDate.now(), 0, false))
    }

    /** Para probar: suena en 1 minuto con la pastilla que toca ahora. */
    fun programarPrueba(context: Context, horarioId: Long, fecha: LocalDate) {
        poner(
            context,
            System.currentTimeMillis() + 60_000L,
            pendiente(context, BASE_PRUEBA + horarioId.toInt(), horarioId, fecha, 0, true)
        )
    }

    private fun millisDe(fecha: LocalDate, minutos: Int): Long =
        fecha.atTime(minutos / 60, minutos % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun pendiente(
        context: Context, codigo: Int, horarioId: Long, fecha: LocalDate, intento: Int, prueba: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmaReceiver::class.java)
            .putExtra(EXTRA_HORARIO, horarioId)
            .putExtra(EXTRA_FECHA, fecha.toString())
            .putExtra(EXTRA_INTENTO, intento)
            .putExtra(EXTRA_PRUEBA, prueba)
        return PendingIntent.getBroadcast(
            context, codigo, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun poner(context: Context, cuando: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        // Exacta si el celular lo permite; si no, aproximada (puede llegar unos minutos tarde)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }
}
