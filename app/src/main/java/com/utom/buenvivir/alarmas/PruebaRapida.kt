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
