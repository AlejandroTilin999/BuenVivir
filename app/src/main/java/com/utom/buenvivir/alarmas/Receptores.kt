package com.utom.buenvivir.alarmas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.utom.buenvivir.data.AjustesStore
import com.utom.buenvivir.data.RepositorioLocal
import com.utom.buenvivir.reloj.Reloj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Corre un trabajo corto en segundo plano sin que Android cierre el receptor antes de tiempo. */
private fun BroadcastReceiver.enSegundoPlano(trabajo: suspend () -> Unit) {
    val pendiente = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            trabajo()
        } finally {
            pendiente.finish()
        }
    }
}

/** Llega la hora de una pastilla (o una repetición, o la prueba). */
class AlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = enSegundoPlano {
        val ctx = context.applicationContext
        val horarioId = intent.getLongExtra(Programador.EXTRA_HORARIO, -1)
        val fecha = intent.getStringExtra(Programador.EXTRA_FECHA)?.let(LocalDate::parse) ?: LocalDate.now()
        val intento = intent.getIntExtra(Programador.EXTRA_INTENTO, 0)
        val prueba = intent.getBooleanExtra(Programador.EXTRA_PRUEBA, false)

        val repo = RepositorioLocal.get(ctx)
        val h = repo.horario(horarioId) ?: return@enSegundoPlano

        // La alarma normal deja programada la de mañana
        if (!prueba && intento == 0) Programador.programarPara(ctx, h, fecha.plusDays(1))

        // Si ya respondió, no molesta (la prueba siempre suena)
        if (!prueba && repo.toma(horarioId, fecha) != null) return@enSegundoPlano

        val ajustes = AjustesStore(ctx).ajustes.first()
        val maximo = Programador.repeticiones(ajustes.repetirMinutos)

        // Pasó la hora de espera y nunca respondió: se registra como "No hubo respuesta"
        if (intento > maximo) {
            repo.registrar(h, fecha, tomada = false, motivo = Programador.MOTIVO_SIN_RESPUESTA)
            Notificaciones.quitar(ctx, horarioId)
            return@enSegundoPlano
        }

        val m = repo.medicina(h.medicinaId) ?: return@enSegundoPlano
        Notificaciones.mostrar(ctx, h, m, fecha, ajustes)
        // También al reloj, solo en el primer aviso (las repeticiones ya las tiene en pantalla)
        android.util.Log.i("Reloj", "Alarma: avisarEnReloj=${ajustes.avisarEnReloj}, intento=$intento")
        if (ajustes.avisarEnReloj && intento == 0) Reloj.avisar(ctx, h, m, fecha, ajustes.vibrarEnReloj, ajustes.sonarEnReloj, prueba)

        // Siguiente aviso (el último solo revisa si respondió)
        Programador.programarRepeticion(ctx, horarioId, fecha, intento + 1, ajustes.repetirMinutos)
    }
}

/** El botón "Ya me la tomé" de la notificación: guarda sin abrir la app. */
class AccionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = enSegundoPlano {
        if (intent.action != Notificaciones.ACCION_TOMADA) return@enSegundoPlano
        val ctx = context.applicationContext
        val horarioId = intent.getLongExtra(Programador.EXTRA_HORARIO, -1)
        val fecha = intent.getStringExtra(Programador.EXTRA_FECHA)?.let(LocalDate::parse) ?: LocalDate.now()
        val repo = RepositorioLocal.get(ctx)
        val h = repo.horario(horarioId) ?: return@enSegundoPlano
        repo.registrar(h, fecha, tomada = true, motivo = null)
        Notificaciones.quitar(ctx, horarioId)
        Programador.cancelarRepeticion(ctx, horarioId)
    }
}

/** Al prender el celular, Android borra las alarmas: aquí se vuelven a programar. */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        enSegundoPlano { Programador.programarTodas(context.applicationContext) }
    }
}
