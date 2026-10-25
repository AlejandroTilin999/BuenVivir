package com.utom.buenvivir.reloj

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

/**
 * Escucha al celular: "/toma" = llegó la hora de una pastilla; "/listo" = ya respondió;
 * "/siguiente" = cuál es la próxima pastilla (llega aunque el reloj estuviera desconectado).
 */
class RelojListener : WearableListenerService() {
    override fun onDataChanged(eventos: DataEventBuffer) {
        eventos.forEach { e ->
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == "/horarios") {
                DataMapItem.fromDataItem(e.dataItem).dataMap.getString("datos")?.let { Horarios.guardar(this, it) }
            }
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == "/siguiente") {
                val d = DataMapItem.fromDataItem(e.dataItem).dataMap
                val medicina = d.getString("medicina") ?: ""
                Pendiente.guardarSiguiente(
                    this,
                    if (medicina.isBlank()) null
                    else Siguiente(medicina, d.getString("cantidad") ?: "", d.getString("cuando") ?: "")
                )
            }
        }
    }

    override fun onMessageReceived(evento: MessageEvent) {
        val texto = String(evento.data, Charsets.UTF_8)
        Log.i("Reloj", "Llegó del celular: ${evento.path}")
        when (evento.path) {
            Telefono.TOMA -> {
                val toma = Toma.deJson(texto) ?: return
                // Si el reloj ya avisó por su cuenta (con su propia alarma), no vuelve a sonar
                Horarios.avisar(this, toma)
            }
            Telefono.LISTO -> {
                val o = runCatching { JSONObject(texto) }.getOrNull() ?: return
                val antes = Pendiente.leer(this)
                Pendiente.borrar(this, o.optLong("horarioId"), o.optString("fecha"))
                // Si ya no hay pendiente, se quita la notificación
                if (antes != null && Pendiente.leer(this) == null) {
                    Aviso.quitar(this)
                    Alerta.detener()
                }
            }
        }
    }
}
