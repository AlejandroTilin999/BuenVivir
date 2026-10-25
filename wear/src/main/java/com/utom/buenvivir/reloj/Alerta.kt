package com.utom.buenvivir.reloj

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibra y/o suena en el reloj una y otra vez, como una alarma, hasta que conteste
 * (o hasta 2 minutos, para no gastar la batería si no lo trae puesto).
 */
object Alerta {
    private const val DURACION_MAXIMA_MS = 2 * 60_000L
    private val manejador = Handler(Looper.getMainLooper())
    private var vibrador: Vibrator? = null
    private var tono: Ringtone? = null
    private val parar = Runnable { detener() }

    private val atributos = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun iniciar(context: Context, vibrar: Boolean, sonar: Boolean) {
        detener()
        val ctx = context.applicationContext
        if (vibrar) {
            val v = if (Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Vibrator::class.java)
            }
            // Vibra 0.8 s, descansa 0.6 s, y se repite
            @Suppress("DEPRECATION")
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0), atributos)
            vibrador = v
        }
        if (sonar) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            tono = RingtoneManager.getRingtone(ctx, uri)?.apply {
                audioAttributes = atributos
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        }
        manejador.postDelayed(parar, DURACION_MAXIMA_MS)
    }

    fun detener() {
        manejador.removeCallbacks(parar)
        vibrador?.cancel()
        vibrador = null
        tono?.stop()
        tono = null
    }
}
