package com.utom.buenvivir.reloj

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.utom.buenvivir.data.RepositorioLocal
import java.time.LocalTime
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.data.db.HorarioEntity
import com.utom.buenvivir.data.db.MedicinaEntity
import com.utom.buenvivir.data.horaTexto
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Habla con la app del reloj (Wear OS) por Bluetooth, a través del celular.
 *
 *   Celular -> Reloj   "/toma"      la pastilla que toca: medicina, cantidad, hora y motivos
 *   Celular -> Reloj   "/listo"     ya respondió (en el celular o en el reloj): se quita el aviso
 *   Celular -> Reloj   "/horarios"  todas las horas de sus pastillas: el reloj programa sus propias
 *                                   alarmas y suena aunque no esté cerca del celular
 *   Reloj   -> Celular "/respuesta/…" "Ya me la tomé" o "No me la tomé" + motivo (ver RelojServicio).
 *                                   Va como dato: si están desconectados, llega cuando se vuelvan a conectar
 *
 * El reloj no guarda nada ni usa internet: todo lo guarda el celular, como siempre.
 */
object Reloj {
    private const val TAG = "Reloj"
    const val TOMA = "/toma"
    const val LISTO = "/listo"
    const val RESPUESTA = "/respuesta"
    const val SIGUIENTE = "/siguiente"
    const val HORARIOS = "/horarios"

    /** Avisa al reloj que llegó la hora de una pastilla. */
    fun avisar(context: Context, h: HorarioEntity, m: MedicinaEntity, fecha: LocalDate, vibrar: Boolean, sonar: Boolean, prueba: Boolean = false) {
        val datos = JSONObject()
            .put("horarioId", h.id)
            .put("fecha", fecha.toString())
            .put("medicina", m.nombre)
            .put("cantidad", m.cantidad)
            .put("indicacion", h.indicacion)
            .put("hora", horaTexto(h.minutos))
            .put("vibrar", vibrar)
            .put("sonar", sonar)
            .put("prueba", prueba)   // la prueba siempre suena, aunque esa pastilla ya haya sonado hoy
            .put("motivos", JSONArray(Repositorio.motivos))
        mandar(context, TOMA, datos)
    }

    /** Ya respondió: el reloj quita el aviso de esa pastilla. */
    fun listo(context: Context, horarioId: Long, fecha: LocalDate) {
        mandar(context, LISTO, JSONObject().put("horarioId", horarioId).put("fecha", fecha.toString()))
    }

    /**
     * Le dice al reloj cuál es la siguiente pastilla (la primera que falta hoy, o la primera de mañana).
     * Va como "dato" y no como mensaje: si el reloj está desconectado, le llega cuando se conecte.
     */
    suspend fun enviarSiguiente(context: Context) {
        val ctx = context.applicationContext
        val repo = RepositorioLocal.get(ctx)
        val horarios = repo.horarios().sortedBy { it.minutos }
        val ahora = LocalTime.now().let { it.hour * 60 + it.minute }
        val hoy = horarios.firstOrNull { it.minutos > ahora }
        val h = hoy ?: horarios.firstOrNull()
        val m = h?.let { repo.medicina(it.medicinaId) }

        val pedido = PutDataMapRequest.create(SIGUIENTE)
        pedido.dataMap.putString("medicina", m?.nombre ?: "")
        pedido.dataMap.putString("cantidad", m?.cantidad ?: "")
        pedido.dataMap.putString("cuando", if (h == null) "" else (if (hoy != null) "Hoy" else "Mañana") + " · " + horaTexto(h.minutos))
        pedido.dataMap.putLong("enviado", System.currentTimeMillis()) // para que siempre cuente como cambio
        Wearable.getDataClient(ctx).putDataItem(pedido.asPutDataRequest().setUrgent())
            .addOnFailureListener { Log.w(TAG, "No se pudo mandar la siguiente pastilla al reloj", it) }
    }

    /**
     * Manda al reloj todas las horas de sus pastillas y cómo avisar (vibrar/sonar).
     * El reloj las guarda y programa sus alarmas, así suena aunque se aleje del celular.
     */
    suspend fun enviarHorarios(context: Context, avisar: Boolean, vibrar: Boolean, sonar: Boolean) {
        val ctx = context.applicationContext
        val repo = RepositorioLocal.get(ctx)
        val lista = JSONArray()
        if (avisar) {
            repo.horarios().forEach { h ->
                val m = repo.medicina(h.medicinaId) ?: return@forEach
                lista.put(
                    JSONObject()
                        .put("horarioId", h.id)
                        .put("minutos", h.minutos)
                        .put("hora", horaTexto(h.minutos))
                        .put("medicina", m.nombre)
                        .put("cantidad", m.cantidad)
                        .put("indicacion", h.indicacion)
                )
            }
        }
        val datos = JSONObject()
            .put("horarios", lista)
            .put("vibrar", vibrar)
            .put("sonar", sonar)
            .put("motivos", JSONArray(Repositorio.motivos))
        val pedido = PutDataMapRequest.create(HORARIOS)
        pedido.dataMap.putString("datos", datos.toString())
        Wearable.getDataClient(ctx).putDataItem(pedido.asPutDataRequest().setUrgent())
            .addOnSuccessListener { Log.i(TAG, "Horarios al reloj: ${lista.length()}") }
            .addOnFailureListener { Log.w(TAG, "No se pudieron mandar los horarios al reloj", it) }
    }

    /** Manda el mensaje a todos los relojes conectados. Si no hay reloj, no pasa nada. */
    private fun mandar(context: Context, ruta: String, datos: JSONObject) {
        val ctx = context.applicationContext
        val bytes = datos.toString().toByteArray(Charsets.UTF_8)
        Wearable.getNodeClient(ctx).connectedNodes
            .addOnSuccessListener { nodos ->
                Log.i(TAG, "Relojes conectados: ${nodos.size} ${nodos.map { it.displayName }}")
                nodos.forEach { nodo ->
                    Wearable.getMessageClient(ctx).sendMessage(nodo.id, ruta, bytes)
                        .addOnSuccessListener { Log.i(TAG, "Se mandó $ruta a ${nodo.displayName}") }
                        .addOnFailureListener { Log.w(TAG, "No se pudo mandar $ruta al reloj", it) }
                }
            }
            .addOnFailureListener { Log.w(TAG, "No se pudo buscar el reloj", it) }
    }
}
