package com.utom.buenvivir.reloj

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** La notificación del reloj: vibra y deja responder sin abrir la app. */
object Aviso {
    private const val ID = 1
    const val ACCION_TOMADA = "com.utom.buenvivir.reloj.TOMADA"
    const val EXTRA_MOTIVO = "abrir_motivo"

    fun mostrar(context: Context, toma: Toma) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        crearCanal(context)

        val abrir = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val tomada = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, AccionReloj::class.java).setAction(ACCION_TOMADA),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val noTomada = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_MOTIVO, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val texto = "Toma ${toma.cantidad} ${toma.indicacion}."
        // Abre la app a pantalla completa y prende la pantalla, como una alarma
        val pantallaCompleta = PendingIntent.getActivity(
            context, 4,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setColor(0xFF004EE0.toInt())
            .setContentTitle("Hora de tu ${toma.medicina}")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(abrir)
            .setFullScreenIntent(pantallaCompleta, true)
            .setOngoing(true)
            .setAutoCancel(true)
            .addAction(0, "Ya me la tomé", tomada)
            .addAction(0, "No me la tomé", noTomada)
            .build()
        NotificationManagerCompat.from(context).notify(ID, n)

        // Prende la pantalla del reloj unos segundos, aunque esté apagada
        runCatching {
            val pm = context.getSystemService(PowerManager::class.java)
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "BuenVivir:alarma"
            ).acquire(15_000L)
        }
        // Y abre la app con la pastilla (si Android no lo permite, queda la notificación a pantalla completa)
        runCatching { pantallaCompleta.send() }
    }

    fun quitar(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }

    /** Canal sin sonido ni vibración propios: de eso se encarga [Alerta], que repite hasta que conteste. */
    private const val CANAL = "pastillas_alarma"

    fun crearCanal(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Recordatorios de pastillas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Te avisa en el reloj cuando toca tomar una pastilla"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }
}
