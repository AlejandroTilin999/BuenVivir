package com.utom.buenvivir.reloj

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Una hora de pastilla guardada en el reloj. */
data class HorarioReloj(
    val horarioId: Long,
    val minutos: Int,
    val hora: String,
    val medicina: String,
    val cantidad: String,
    val indicacion: String
)

/**
 * Los horarios que mandó el celular, guardados en el reloj. Con ellos el reloj programa
 * sus propias alarmas: suena a la hora aunque no esté cerca del celular.
 */
object Horarios {
    private const val ARCHIVO = "horarios"
    private const val CLAVE = "datos"
    private const val AVISADAS = "avisadas"
    private const val BASE = 600_000

    private fun prefs(context: Context) = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /** Guarda lo que mandó el celular y vuelve a programar todas las alarmas del reloj. */
    fun guardar(context: Context, datos: String) {
        cancelarTodas(context)
        prefs(context).edit().putString(CLAVE, datos).apply()
        programarTodas(context)
    }

    private fun datos(context: Context): JSONObject? =
        runCatching { JSONObject(prefs(context).getString(CLAVE, null) ?: return null) }.getOrNull()

    fun lista(context: Context): List<HorarioReloj> {
        val arr = datos(context)?.optJSONArray("horarios") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HorarioReloj(
                o.getLong("horarioId"), o.getInt("minutos"), o.optString("hora"),
                o.optString("medicina"), o.optString("cantidad"), o.optString("indicacion")
            )
        }
    }

    fun vibrar(context: Context) = datos(context)?.optBoolean("vibrar", true) ?: true
    fun sonar(context: Context) = datos(context)?.optBoolean("sonar", false) ?: false
    fun motivos(context: Context): List<String> {
        val arr = datos(context)?.optJSONArray("motivos") ?: JSONArray()
        return (0 until arr.length()).map { arr.getString(it) }
    }

    // ---- Para no sonar dos veces por la misma pastilla (alarma del reloj + aviso del celular) ----

    private fun clave(horarioId: Long, fecha: String) = "${horarioId}_$fecha"

    fun yaAvisada(context: Context, horarioId: Long, fecha: String): Boolean =
        clave(horarioId, fecha) in (prefs(context).getStringSet(AVISADAS, emptySet()) ?: emptySet())

    fun marcarAvisada(context: Context, horarioId: Long, fecha: String) {
        val actuales = prefs(context).getStringSet(AVISADAS, emptySet()).orEmpty()
        // Solo se guardan las de los últimos días, para que no crezca
        val hace3 = LocalDate.now().minusDays(3).toString()
        val nuevas = actuales.filter { it.substringAfter('_') >= hace3 }.toSet() + clave(horarioId, fecha)
        prefs(context).edit().putStringSet(AVISADAS, nuevas).apply()
    }

    // ---- Alarmas del reloj ----

    fun programarTodas(context: Context) = lista(context).forEach { programarSiguiente(context, it) }

    private fun programarSiguiente(context: Context, h: HorarioReloj) {
        val hoy = LocalDate.now()
        val fecha = if (millis(hoy, h.minutos) > System.currentTimeMillis()) hoy else hoy.plusDays(1)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendiente(context, h.horarioId, fecha.toString())
        val cuando = millis(fecha, h.minutos)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        }
    }

    private fun cancelarTodas(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        lista(context).forEach { am.cancel(pendiente(context, it.horarioId, "")) }
    }

    private fun pendiente(context: Context, horarioId: Long, fecha: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, BASE + horarioId.toInt(),
            Intent(context, AlarmaReloj::class.java)
                .putExtra("horarioId", horarioId)
                .putExtra("fecha", fecha),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun millis(fecha: LocalDate, minutos: Int): Long =
        fecha.atTime(minutos / 60, minutos % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Avisa de una pastilla (venga de la alarma del reloj o del celular), una sola vez. */
    fun avisar(context: Context, toma: Toma) {
        if (!toma.prueba && yaAvisada(context, toma.horarioId, toma.fecha)) return
        marcarAvisada(context, toma.horarioId, toma.fecha)
        Pendiente.guardar(context, toma)
        Aviso.mostrar(context, toma)                     // prende la pantalla y abre la app
        Alerta.iniciar(context, toma.vibrar, toma.sonar) // vibra/suena hasta que conteste
    }
}

/** Llega la hora de una pastilla en el reloj (aunque esté lejos del celular). */
class AlarmaReloj : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val horarioId = intent.getLongExtra("horarioId", -1)
        val fecha = intent.getStringExtra("fecha")?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()
        // Deja programada la de mañana
        Horarios.programarTodas(context)
        val h = Horarios.lista(context).firstOrNull { it.horarioId == horarioId } ?: return
        Horarios.avisar(
            context,
            Toma(
                horarioId = h.horarioId, fecha = fecha, medicina = h.medicina, cantidad = h.cantidad,
                indicacion = h.indicacion, hora = h.hora, vibrar = Horarios.vibrar(context),
                motivos = Horarios.motivos(context).ifEmpty { listOf("Se me olvidó", "No tenía la medicina", "Otro motivo") },
                sonar = Horarios.sonar(context)
            )
        )
    }
}

/** Al prender el reloj, Android borra las alarmas: aquí se vuelven a programar. */
class ArranqueReloj : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Horarios.programarTodas(context)
    }
}
