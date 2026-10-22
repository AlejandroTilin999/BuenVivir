package com.utom.buenvivir.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Lee en voz alta con el motor de voz del celular, en español y un poco más lento. */
class Lector(context: Context) {
    private var listo = false
    private var pendiente: String? = null
    private var velocidad = 0.92f

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val mx = tts.setLanguage(Locale.forLanguageTag("es-MX"))
            if (mx == TextToSpeech.LANG_MISSING_DATA || mx == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.forLanguageTag("es-US"))
            }
            elegirVozNatural()
            tts.setSpeechRate(velocidad)
            tts.setPitch(1.05f)      // tono ligeramente más alto, más cálido
            listo = true
            pendiente?.let { hablar(it) }
            pendiente = null
        }
    }

    /**
     * Escoge la voz en español de mejor calidad que tenga el celular.
     * Prefiere México, luego Estados Unidos (latino), y las que no necesitan internet.
     */
    private fun elegirVozNatural() {
        val voces = runCatching { tts.voices }.getOrNull()?.filter {
            it.locale.language == "es" &&
                !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        } ?: return
        val mejor = voces.maxByOrNull { v ->
            val region = when (v.locale.country) { "MX" -> 300; "US" -> 200; "419" -> 150; else -> 0 }
            val sinInternet = if (v.isNetworkConnectionRequired) 0 else 50
            region + v.quality + sinInternet
        } ?: return
        tts.voice = mejor
    }

    /** Si ya está hablando, lo calla; si no, lee el texto. */
    fun leer(texto: String) {
        if (!listo) { pendiente = texto; return }
        if (tts.isSpeaking) tts.stop() else hablar(texto)
    }

    /** Lenta para adultos mayores, o normal. */
    fun usarVozLenta(lenta: Boolean) {
        velocidad = if (lenta) 0.85f else 1.0f
        if (listo) tts.setSpeechRate(velocidad)
    }

    fun callar() { if (listo) tts.stop() }

    private fun hablar(texto: String) {
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "buenvivir")
    }

    fun liberar() { tts.stop(); tts.shutdown() }
}

@Composable
fun rememberLector(): Lector {
    val context = LocalContext.current
    val lector = remember { Lector(context) }
    DisposableEffect(lector) { onDispose { lector.liberar() } }
    return lector
}
