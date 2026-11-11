package com.utom.buenvivir.alarmas

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.utom.buenvivir.MainActivity
import com.utom.buenvivir.R
import com.utom.buenvivir.data.RepositorioLocal
import com.utom.buenvivir.data.db.HorarioEntity
import com.utom.buenvivir.data.db.MedicinaEntity
import com.utom.buenvivir.data.horaTexto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Aviso 20 minutos antes de cada pastilla: "En 20 minutos te toca tu Metformina".
 * Usa el mismo receptor de las alarmas (AlarmaReceiver) con la marca [EXTRA].
 */
object PreAvisos {
    const val MINUTOS = 20
    const val EXTRA = "preaviso"
    private const val BASE = 600_000          // código de la alarma (distinto a los de Programador)
    private const val BASE_NOTIFICACION = 700_000
    private const val CANAL = "preavisos"

    /** Programa el próximo aviso de cada horario. Se llama junto con Programador.programarTodas. */
    suspend fun programarTodos(context: Context) {
        RepositorioLocal.get(context).horarios().forEach { programarSiguiente(context, it) }
    }

    /** El próximo aviso que todavía no pasa: el de la pastilla de hoy, o el de mañana. */
    fun programarSiguiente(context: Context, h: HorarioEntity) {
        val hoy = LocalDate.now()
        val fecha = if (momento(hoy, h.minutos).isAfter(LocalDateTime.now())) hoy else hoy.plusDays(1)
        programarPara(context, h, fecha)
    }

    /** Aviso para la pastilla de ese día (20 minutos antes de su hora). */
    fun programarPara(context: Context, h: HorarioEntity, fecha: LocalDate) {
        val cuando = momento(fecha, h.minutos).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (cuando <= System.currentTimeMillis()) return
        val intent = Intent(context, AlarmaReceiver::class.java)
            .putExtra(Programador.EXTRA_HORARIO, h.id)
            .putExtra(Programador.EXTRA_FECHA, fecha.toString())
            .putExtra(EXTRA, true)
        val pi = PendingIntent.getBroadcast(
            context, BASE + h.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }

    /** 20 minutos antes de la hora de la pastilla (si es 12:10 a. m., el aviso llega el día anterior). */
    private fun momento(fecha: LocalDate, minutos: Int): LocalDateTime =
        fecha.atTime(minutos / 60, minutos % 60).minusMinutes(MINUTOS.toLong())

    /** La notificación suave (sin alarma insistente): solo avisa que ya casi le toca. */
    fun mostrar(context: Context, h: HorarioEntity, m: MedicinaEntity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Avisos antes de tu pastilla", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Te avisa $MINUTOS minutos antes de que te toque una pastilla"
                }
            )
        }
        val abrir = PendingIntent.getActivity(
            context, BASE_NOTIFICACION + h.id.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val texto = "Toma ${m.cantidad} ${h.indicacion} a las ${horaTexto(h.minutos)}."
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setColor(0xFF004EE0.toInt())
            .setContentTitle("En $MINUTOS minutos te toca tu ${m.nombre}")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICACION + h.id.toInt(), notificacion)
    }

    /** Quita el aviso previo cuando ya suena la alarma de la pastilla. */
    fun quitar(context: Context, horarioId: Long) {
        NotificationManagerCompat.from(context).cancel(BASE_NOTIFICACION + horarioId.toInt())
    }
}
