package com.utom.buenvivir.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Una medicina que Gemini leyó de la foto de la receta. La persona la revisa antes de guardarla. */
data class MedicinaLeida(
    val nombre: String,
    val para: String,
    val presentacion: String,   // "850 mg"
    val cantidad: String,       // "1 tableta"
    val momento: String,        // "antes de comer", "con la comida", "después de comer" o ""
    val horas: List<Int>,       // minutos del día: 8:00 p. m. = 1200
    val dias: Int               // 0 = la receta no dice por cuánto tiempo
)

sealed interface Lectura {
    data class Bien(val medicinas: List<MedicinaLeida>) : Lectura
    data class Mal(val mensaje: String) : Lectura
}

/**
 * Lee una receta (foto de la cámara o de la galería) con Gemini, por medio de Firebase AI Logic.
 * Solo saca los datos que usa la app: medicina, cantidad, cuándo, horas y por cuántos días.
 * Lo demás (dieta, ejercicio, citas) lo ignora.
 */
object LectorReceta {
    private const val TAG = "LectorReceta"

    /**
     * Modelos que se prueban en orden. Primero el ligero (responde más rápido);
     * si falla, se intenta con el siguiente. Si Google retira uno, solo se cambia aquí.
     */
    private val MODELOS = listOf("gemini-3.5-flash-lite", "gemini-3.8-flash")

    /** Máximo de espera por intento, para que la app no se quede minutos cargando. */
    private const val ESPERA_MS = 25_000L

    private const val LADO_MAXIMO = 1024   // px: suficiente para leer letra impresa y la petición pesa menos

    private val INSTRUCCION = """
        Eres un asistente que lee recetas médicas mexicanas para una app de recordatorios de medicamentos.
        Lee la imagen y responde SOLO con un JSON con esta forma exacta:
        {"esReceta": true, "medicinas": [{"nombre": "", "para": "", "presentacion": "", "cantidad": "", "momento": "", "horas": [], "dias": 0}]}

        Reglas:
        - "nombre": solo el nombre genérico con mayúscula inicial, sin dosis (ejemplo: "Metformina").
        - "para": para qué sirve, en palabras sencillas que entienda un adulto mayor. Si la receta lo dice, úsalo; si no, escribe el uso más común de ese medicamento (ejemplo: Metformina = "Para controlar el azúcar"). Si no lo sabes con seguridad, "".
        - "presentacion": la concentración (ejemplo: "850 mg"); si no viene, "".
        - "cantidad": cuánto toma cada vez, en este formato: "½ tableta", "1 tableta", "2 tabletas", "1 cápsula" o "10 unidades" para insulina.
        - "momento": exactamente uno de "antes de comer", "con la comida", "después de comer". Desayuno, comida y cena cuentan como comer. Si no lo dice, "".
        - "horas": las horas en formato 24 h "HH:mm". Si la receta dice las horas, úsalas. Si solo dice "cada 8/12/24 horas", propón horas empezando a las 08:00 (cada 12 h: 08:00 y 20:00; cada 8 h: 06:00, 14:00 y 22:00; cada 24 h: 08:00).
        - "dias": por cuántos días la toma (ejemplo: "por 30 días" = 30, "por una semana" = 7). Si no lo dice, 0.
        - Ignora indicaciones que no sean medicamentos (dieta, ejercicio, estudios, próxima cita).
        - No inventes medicamentos. Si algo no se lee, déjalo vacío.
        - Si la imagen no es una receta médica, responde {"esReceta": false, "medicinas": []}.
    """.trimIndent()

    private fun modelo(nombre: String) =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = nombre,
            generationConfig = generationConfig { responseMimeType = "application/json" },
            requestOptions = RequestOptions(timeoutInMillis = ESPERA_MS)
        )

    suspend fun leer(context: Context, foto: Uri): Lectura {
        val imagen = withContext(Dispatchers.IO) { cargar(context, foto) }
            ?: return Lectura.Mal("No se pudo abrir la foto. Intenta tomarla otra vez.")
        var ultimoError: Exception? = null
        for (nombre in MODELOS) {
            try {
                val respuesta = modelo(nombre).generateContent(content {
                    image(imagen)
                    text(INSTRUCCION)
                })
                val texto = respuesta.text ?: continue
                Log.i(TAG, "Receta leída con $nombre")
                return interpretar(texto)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini ($nombre) no pudo leer la receta", e)
                ultimoError = e
            }
        }
        val mensaje = ultimoError?.message.orEmpty()
        return if (mensaje.contains("Deadline", ignoreCase = true) || mensaje.contains("timeout", ignoreCase = true)) {
            Lectura.Mal("El servicio para leer recetas está tardando. Inténtalo otra vez o registra la medicina a mano.")
        } else {
            Lectura.Mal("No se pudo leer la receta. Inténtalo otra vez o registra la medicina a mano.")
        }
    }

    private fun interpretar(texto: String): Lectura = try {
        val json = JSONObject(texto.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
        if (!json.optBoolean("esReceta", true)) {
            Lectura.Mal("Eso no parece una receta. Toma la foto de frente y con buena luz.")
        } else {
            val lista = json.optJSONArray("medicinas")
            val medicinas = (0 until (lista?.length() ?: 0)).mapNotNull { i ->
                val m = lista!!.getJSONObject(i)
                val nombre = m.optString("nombre").trim()
                if (nombre.isEmpty()) return@mapNotNull null
                val horas = m.optJSONArray("horas")
                MedicinaLeida(
                    nombre = nombre,
                    para = m.optString("para").trim(),
                    presentacion = m.optString("presentacion").trim(),
                    cantidad = m.optString("cantidad").trim(),
                    momento = m.optString("momento").trim().lowercase(),
                    horas = (0 until (horas?.length() ?: 0)).mapNotNull { j -> minutosDe(horas!!.optString(j)) }
                        .distinct().sorted(),
                    dias = m.optInt("dias", 0).coerceIn(0, 365)
                )
            }
            if (medicinas.isEmpty()) Lectura.Mal("No encontré medicinas en la foto. Intenta con otra más clara.")
            else Lectura.Bien(medicinas)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Respuesta que no se entendió: $texto", e)
        Lectura.Mal("No se pudo leer la receta. Intenta con otra foto.")
    }

    /** "20:00" -> 1200 */
    private fun minutosDe(hora: String): Int? {
        val partes = hora.trim().split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }

    /** Abre la foto y la achica si es muy grande. */
    private fun cargar(context: Context, uri: Uri): Bitmap? = try {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
        var muestra = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        bitmap?.let {
            val lado = maxOf(it.width, it.height)
            if (lado <= LADO_MAXIMO) it
            else Bitmap.createScaledBitmap(it, it.width * LADO_MAXIMO / lado, it.height * LADO_MAXIMO / lado, true)
        }
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo abrir la foto", e)
        null
    }
}
