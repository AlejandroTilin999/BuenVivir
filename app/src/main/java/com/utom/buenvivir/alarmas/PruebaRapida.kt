package com.utom.buenvivir.alarmas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate

/** "Probar recordatorio": suena en [SEGUNDOS] segundos con la pastilla que toca ahora (para la demostración). */
object PruebaRapida {
    const val SEGUNDOS = 10
    private const val CODIGO = 250_000

    fun programar(context: Context, horarioId: Long, fecha: LocalDate) {
        ModoPrueba.marcar(context, horarioId, fecha)
        val intent = Intent(context, AlarmaReceiver::class.java)
            .putExtra(Programador.EXTRA_HORARIO, horarioId)
            .putExtra(Programador.EXTRA_FECHA, fecha.toString())
            .putExtra(Programador.EXTRA_INTENTO, 0)
            .putExtra(Programador.EXTRA_PRUEBA, true)
        val pi = PendingIntent.getBroadcast(
            context, CODIGO + horarioId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(AlarmManager::class.java)
        val cuando = System.currentTimeMillis() + SEGUNDOS * 1_000L
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }
}

/**
 * Recuerda que la alarma de esta pastilla es de prueba, para que la respuesta no se guarde
 * como toma real (ni en el celular, ni en Firebase, ni en lo que ve el médico).
 * Vale por [MINUTOS] minutos y solo para la primera respuesta.
 */
object ModoPrueba {
    private const val MINUTOS = 10
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("modo_prueba", Context.MODE_PRIVATE)

    private fun clave(horarioId: Long, fecha: LocalDate) = "$horarioId|$fecha"

    fun marcar(context: Context, horarioId: Long, fecha: LocalDate) {
        val vence = System.currentTimeMillis() + PruebaRapida.SEGUNDOS * 1_000L + MINUTOS * 60_000L
        prefs(context).edit().putLong(clave(horarioId, fecha), vence).apply()
    }

    /** true si la respuesta a esta pastilla es de una prueba (sin borrar la marca). */
    fun activa(context: Context, horarioId: Long, fecha: LocalDate): Boolean =
        prefs(context).getLong(clave(horarioId, fecha), 0L) > System.currentTimeMillis()

    /** Igual que [activa], pero borra la marca: la siguiente respuesta ya cuenta como real. */
    fun consumir(context: Context, horarioId: Long, fecha: LocalDate): Boolean {
        val era = activa(context, horarioId, fecha)
        prefs(context).edit().remove(clave(horarioId, fecha)).apply()
        return era
    }
}
