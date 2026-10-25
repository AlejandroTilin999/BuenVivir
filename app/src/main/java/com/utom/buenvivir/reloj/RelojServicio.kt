package com.utom.buenvivir.reloj

import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.utom.buenvivir.alarmas.Notificaciones
import com.utom.buenvivir.alarmas.Programador
import com.utom.buenvivir.data.RepositorioLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

/**
 * Recibe lo que Don José contestó en el reloj y lo guarda igual que si lo hubiera tocado en el celular:
 * en la base del celular y en Firebase (así lo ven su médico y quienes lo cuidan).
 *
 * Llega como "dato" ("/respuesta/{horario}_{fecha}"): si el reloj contestó sin conexión,
 * Android lo entrega cuando se vuelven a conectar.
 */
class RelojServicio : WearableListenerService() {
    private val trabajo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(eventos: DataEventBuffer) {
        eventos.forEach { e ->
            if (e.type != DataEvent.TYPE_CHANGED) return@forEach
            val uri = e.dataItem.uri
            if (uri.path?.startsWith(Reloj.RESPUESTA) != true) return@forEach
            val d = DataMapItem.fromDataItem(e.dataItem).dataMap
            guardar(
                horarioId = d.getLong("horarioId", -1),
                fechaTexto = d.getString("fecha") ?: "",
                tomada = d.getBoolean("tomada", false),
                motivo = d.getString("motivo"),
                cuando = d.getLong("cuando", System.currentTimeMillis()),
                borrar = uri
            )
        }
    }

    /** Por si llega como mensaje (versión anterior de la app del reloj). */
    override fun onMessageReceived(evento: MessageEvent) {
        if (evento.path != Reloj.RESPUESTA) return
        val o = runCatching { JSONObject(String(evento.data, Charsets.UTF_8)) }.getOrNull() ?: return
        guardar(o.optLong("horarioId", -1), o.optString("fecha"), o.optBoolean("tomada"), o.optString("motivo"), System.currentTimeMillis(), null)
    }

    private fun guardar(horarioId: Long, fechaTexto: String, tomada: Boolean, motivo: String?, cuando: Long, borrar: Uri?) {
        val fecha = runCatching { LocalDate.parse(fechaTexto) }.getOrNull() ?: return
        val ctx = applicationContext
        trabajo.launch {
            val repo = RepositorioLocal.get(ctx)
            val h = repo.horario(horarioId)
            if (h != null) {
                val porque = motivo?.takeIf { it.isNotBlank() } ?: "Otro motivo"
                repo.registrar(h, fecha, tomada, if (tomada) null else porque, registradaEn = cuando)
                // Ya respondió: se quita la alarma del celular y deja de repetirse
                Notificaciones.quitar(ctx, horarioId)
                Programador.cancelarRepeticion(ctx, horarioId)
                Reloj.enviarSiguiente(ctx)
            } else {
                Log.w("Reloj", "Respuesta de un horario que ya no existe: $horarioId")
            }
            // Ya se guardó: se borra el dato para que no se acumulen
            borrar?.let { Wearable.getDataClient(ctx).deleteDataItems(it) }
        }
    }

    override fun onDestroy() {
        trabajo.cancel()
        super.onDestroy()
    }
}
