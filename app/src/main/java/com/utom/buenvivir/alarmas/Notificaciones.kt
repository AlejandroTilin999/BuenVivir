package com.utom.buenvivir.alarmas

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.utom.buenvivir.MainActivity
import com.utom.buenvivir.R
import com.utom.buenvivir.data.Ajustes
import com.utom.buenvivir.data.db.HorarioEntity
import com.utom.buenvivir.data.db.MedicinaEntity
import com.utom.buenvivir.data.horaTexto
import java.time.LocalDate

/** La notificación de "hora de tu pastilla", con sus botones. */
object Notificaciones {
    const val ACCION_TOMADA = "com.utom.buenvivir.TOMADA"
    const val EXTRA_ABRIR_MOTIVO = "abrir_motivo"

    private const val BASE_TOMADA = 300_000
    private const val BASE_MOTIVO = 400_000
    private const val BASE_ABRIR = 500_000

    fun mostrar(context: Context, h: HorarioEntity, m: MedicinaEntity, fecha: LocalDate, ajustes: Ajustes) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val id = h.id.toInt()
        val texto = "Toma ${m.cantidad} ${h.indicacion} (${horaTexto(h.minutos)})."

        val tomada = PendingIntent.getBroadcast(
            context, BASE_TOMADA + id,
            Intent(context, AccionReceiver::class.java)
                .setAction(ACCION_TOMADA)
                .putExtra(Programador.EXTRA_HORARIO, h.id)
                .putExtra(Programador.EXTRA_FECHA, fecha.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val noTomada = PendingIntent.getActivity(
            context, BASE_MOTIVO + id,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ABRIR_MOTIVO, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val abrir = PendingIntent.getActivity(
            context, BASE_ABRIR + id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, canal(context, ajustes.sonido, ajustes.vibracion))
            .setSmallIcon(R.drawable.ic_pastilla)
            .setColor(0xFF004EE0.toInt())
            .setContentTitle("Hora de tu ${m.nombre}")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .addAction(0, "Ya me la tomé", tomada)
            .addAction(0, "No me la tomé", noTomada)
            .build()

        // Con sonido, el tono se repite hasta que responda o quite la notificación
        if (ajustes.sonido) {
            notificacion.flags = notificacion.flags or Notification.FLAG_INSISTENT
            ajustarVolumen(context, ajustes.volumen)
        }

        NotificationManagerCompat.from(context).notify(id, notificacion)
    }

    /**
     * Pone el volumen de alarma del celular según Ajustes (0 Bajo, 1 Medio, 2 Alto),
     * para que suene aunque el celular esté bajito. El tono usa el canal de alarma.
     */
    private fun ajustarVolumen(context: Context, nivel: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val maximo = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val porcentaje = when (nivel) {
            0 -> 0.4f
            1 -> 0.7f
            else -> 1f
        }
        val volumen = (maximo * porcentaje).toInt().coerceIn(1, maximo)
        try {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, volumen, 0)
        } catch (_: SecurityException) {
            // Con "No molestar" activo, Android puede no dejar cambiarlo
        }
    }

    private var prueba: Ringtone? = null
    private val manejador = Handler(Looper.getMainLooper())
    private val detenerPrueba = Runnable { prueba?.stop(); prueba = null }

    /** Suena el tono de alarma unos 2 segundos con el volumen elegido, para que lo escuche. */
    fun probarVolumen(context: Context, nivel: Int) {
        manejador.removeCallbacks(detenerPrueba)
        prueba?.stop()
        ajustarVolumen(context, nivel)
        val tono = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        prueba = RingtoneManager.getRingtone(context.applicationContext, tono)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
        manejador.postDelayed(detenerPrueba, 2_000)
    }

    fun quitar(context: Context, horarioId: Long) {
        NotificationManagerCompat.from(context).cancel(horarioId.toInt())
    }

    /**
     * Android no deja cambiar el sonido de un canal ya creado, así que hay un canal
     * por cada combinación de Ajustes (sonido sí/no, vibración sí/no).
     */
    private fun canal(context: Context, sonido: Boolean, vibracion: Boolean): String {
        val id = "pastillas_" + (if (sonido) "s" else "x") + (if (vibracion) "v" else "x")
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            val nombre = "Recordatorios de pastillas" + when {
                sonido && vibracion -> ""
                sonido -> " (sin vibración)"
                vibracion -> " (sin sonido)"
                else -> " (silencio)"
            }
            val c = NotificationChannel(id, nombre, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Te avisa cuando toca tomar una pastilla"
                if (sonido) {
                    val tono = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        tono,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    setSound(null, null)
                }
                enableVibration(vibracion)
                if (vibracion) vibrationPattern = longArrayOf(0, 700, 300, 700, 300, 700)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(c)
        }
        return id
    }
}
