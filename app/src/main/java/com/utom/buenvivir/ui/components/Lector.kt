package com.utom.buenvivir.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { terminar(id) }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { terminar(id) }
                override fun onStop(id: String?, interrupted: Boolean) { terminar(id) }
            })
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

    // Lo que hay que hacer cuando termine de decir una frase (para el asistente de voz)
    private val principal = Handler(Looper.getMainLooper())
    private val despues = mutableMapOf<String, () -> Unit>()
    private var contador = 0

    private fun terminar(id: String?) {
        val accion = synchronized(despues) { despues.remove(id) } ?: return
        principal.post(accion)
    }

    /** Dice [texto] y, cuando termina de hablar, hace [luego] (por ejemplo, abrir el micrófono). */
    fun decirYLuego(texto: String, luego: () -> Unit) {
        if (!listo) { principal.postDelayed(luego, 300); return }
        val id = "pregunta${contador++}"
        synchronized(despues) { despues.clear(); despues[id] = luego }
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    /** Solo dice el texto (aunque ya esté hablando, lo reemplaza). */
    fun decir(texto: String) {
        if (!listo) { pendiente = texto; return }
        synchronized(despues) { despues.clear() }
        hablar(texto)
    }

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
