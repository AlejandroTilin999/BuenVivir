package com.utom.buenvivir.reloj

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** La pastilla que toca, tal como la manda el celular. */
data class Toma(
    val horarioId: Long,
    val fecha: String,
    val medicina: String,
    val cantidad: String,
    val indicacion: String,
    val hora: String,
    val vibrar: Boolean,
    val motivos: List<String>,
    val sonar: Boolean = false,
    val prueba: Boolean = false
) {
    fun aJson(): String = JSONObject()
        .put("horarioId", horarioId)
        .put("fecha", fecha)
        .put("medicina", medicina)
        .put("cantidad", cantidad)
        .put("indicacion", indicacion)
        .put("hora", hora)
        .put("vibrar", vibrar)
        .put("sonar", sonar)
        .put("motivos", JSONArray(motivos))
        .toString()

    companion object {
        fun deJson(texto: String?): Toma? = runCatching {
            val o = JSONObject(texto ?: return null)
            val lista = o.optJSONArray("motivos") ?: JSONArray()
            Toma(
                horarioId = o.getLong("horarioId"),
                fecha = o.getString("fecha"),
                medicina = o.optString("medicina"),
                cantidad = o.optString("cantidad"),
                indicacion = o.optString("indicacion"),
                hora = o.optString("hora"),
                vibrar = o.optBoolean("vibrar", true),
                motivos = (0 until lista.length()).map { lista.getString(it) }
                    .ifEmpty { listOf("Se me olvidó", "No tenía la medicina", "Otro motivo") },
                sonar = o.optBoolean("sonar", false),
                prueba = o.optBoolean("prueba", false)
            )
        }.getOrNull()
    }
}

/** La siguiente pastilla que le toca (para mostrarla mientras espera). */
data class Siguiente(val medicina: String, val cantidad: String, val cuando: String)

/** Guarda la pastilla pendiente en el reloj, para mostrarla aunque se abra la app más tarde. */
object Pendiente {
    private const val ARCHIVO = "pendiente"
    const val CLAVE = "toma"
    const val CLAVE_SIGUIENTE = "siguiente"

    fun leerSiguiente(context: Context): Siguiente? = runCatching {
        val o = JSONObject(preferencias(context).getString(CLAVE_SIGUIENTE, null) ?: return null)
        Siguiente(o.getString("medicina"), o.optString("cantidad"), o.optString("cuando"))
    }.getOrNull()?.takeIf { it.medicina.isNotBlank() }

    fun guardarSiguiente(context: Context, s: Siguiente?) {
        val texto = s?.let {
            JSONObject().put("medicina", it.medicina).put("cantidad", it.cantidad).put("cuando", it.cuando).toString()
        }
        preferencias(context).edit().putString(CLAVE_SIGUIENTE, texto).apply()
    }

    fun preferencias(context: Context) = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun leer(context: Context): Toma? = Toma.deJson(preferencias(context).getString(CLAVE, null))

    fun guardar(context: Context, toma: Toma) {
        preferencias(context).edit().putString(CLAVE, toma.aJson()).apply()
    }

    /** Borra la pendiente solo si es esa misma toma (por si ya llegó otra). */
    fun borrar(context: Context, horarioId: Long, fecha: String) {
        val actual = leer(context) ?: return
        if (actual.horarioId == horarioId && actual.fecha == fecha) {
            preferencias(context).edit().remove(CLAVE).apply()
        }
    }
}
