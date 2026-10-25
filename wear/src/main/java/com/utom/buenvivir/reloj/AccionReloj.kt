package com.utom.buenvivir.reloj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Ya me la tomé" desde la notificación del reloj: se manda al celular sin abrir la app. */
class AccionReloj : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Aviso.ACCION_TOMADA) return
        val toma = Pendiente.leer(context) ?: run { Aviso.quitar(context); return }
        Telefono.responder(context, toma, tomada = true, motivo = null)
    }
}
