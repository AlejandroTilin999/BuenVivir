package com.utom.buenvivir.reloj

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Manda al celular lo que contestó en el reloj. El celular lo guarda y lo sube a Firebase.
 * Va como "dato": si el reloj está lejos del celular, se guarda y se manda solo al volver a conectarse.
 */
object Telefono {
    private const val TAG = "Telefono"
    const val TOMA = "/toma"
    const val LISTO = "/listo"

    fun responder(context: Context, toma: Toma, tomada: Boolean, motivo: String?) {
        val ctx = context.applicationContext
        val pedido = PutDataMapRequest.create("/respuesta/${toma.horarioId}_${toma.fecha}")
        pedido.dataMap.putLong("horarioId", toma.horarioId)
        pedido.dataMap.putString("fecha", toma.fecha)
        pedido.dataMap.putBoolean("tomada", tomada)
        pedido.dataMap.putString("motivo", motivo ?: "")
        pedido.dataMap.putLong("cuando", System.currentTimeMillis()) // la hora real en que contestó
        Wearable.getDataClient(ctx).putDataItem(pedido.asPutDataRequest().setUrgent())
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar la respuesta", it) }
        // En el reloj ya no está pendiente
        Horarios.marcarAvisada(ctx, toma.horarioId, toma.fecha)
        Pendiente.borrar(ctx, toma.horarioId, toma.fecha)
        Aviso.quitar(ctx)
        Alerta.detener()
    }
}
