package com.utom.buenvivir.trabajos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.utom.buenvivir.MainActivity
import com.utom.buenvivir.R
import com.utom.buenvivir.alarmas.Programador
import com.utom.buenvivir.data.AjustesStore
import com.utom.buenvivir.data.Recetas
import com.utom.buenvivir.data.RepositorioLocal
import com.utom.buenvivir.data.SesionStore
import com.utom.buenvivir.data.esperar
import com.utom.buenvivir.data.horaTexto
import com.utom.buenvivir.reloj.Reloj
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Trabajos que Android corre cada 15 minutos aunque la app esté cerrada (lo más seguido que permite).
 *
 *  - Paciente: revisa si su médico o su cuidador le cambiaron las medicinas y programa sus alarmas.
 *  - Médico y cuidador: revisan si algún paciente no se tomó una pastilla y les avisa con una notificación.
 */
object Trabajos {
    private const val PACIENTE = "sincronizar_paciente"
    private const val EQUIPO = "avisos_equipo"

    private val conInternet = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun programarPaciente(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(EQUIPO)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PACIENTE, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SincronizarPaciente>(15, TimeUnit.MINUTES).setConstraints(conInternet).build()
        )
    }

    fun programarEquipo(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PACIENTE)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EQUIPO, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AvisarEquipo>(15, TimeUnit.MINUTES).setConstraints(conInternet).build()
        )
    }

    /** Al cerrar sesión o cambiar de rol, ya no hay nada que revisar. */
    fun cancelarTodos(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PACIENTE)
        WorkManager.getInstance(context).cancelUniqueWork(EQUIPO)
    }
}

/** Paciente: trae sus medicinas de Firebase, actualiza el celular, sus alarmas y el reloj. */
class SincronizarPaciente(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (SesionStore(ctx).rol.first() != SesionStore.PACIENTE) return Result.success()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        return try {
            val docs = Recetas.coleccion(uid).get(Source.SERVER).esperar()
            val lista = docs.documents.mapNotNull(Recetas::leer)
            val repo = RepositorioLocal.get(ctx)
            if (repo.sincronizar(lista)) {
                Programador.programarTodas(ctx)
                val a = AjustesStore(ctx).ajustes.first()
                Reloj.enviarHorarios(ctx, a.avisarEnReloj, a.vibrarEnReloj, a.sonarEnReloj)
                Reloj.enviarSiguiente(ctx)
                Log.i("Trabajos", "Medicinas actualizadas en segundo plano")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("Trabajos", "No se pudo sincronizar", e)
            Result.retry()
        }
    }
}

/**
 * Médico y cuidador: busca tomas "No me la tomé" (o "No hubo respuesta") de sus pacientes
 * de ayer y hoy, y avisa una sola vez por cada una.
 */
class AvisarEquipo(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val rol = SesionStore(ctx).rol.first()
        if (rol != SesionStore.MEDICO && rol != SesionStore.FAMILIAR) return Result.success()
        return try {
            AvisosEquipo.revisar(ctx)
            Result.success()
        } catch (e: Exception) {
            Log.w("Trabajos", "No se pudieron revisar las tomas", e)
            Result.retry()
        }
    }
}

object AvisosEquipo {
    private const val CANAL = "avisos_equipo"
    private const val PREFS = "avisos_equipo"
    private const val VISTAS = "vistas"
    private const val INICIADO = "iniciado"

    /** Revisa las tomas de todos sus pacientes. También se llama cada minuto mientras la app está abierta. */
    suspend fun revisar(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val vistas = prefs.getStringSet(VISTAS, emptySet()).orEmpty().toMutableSet()
        // La primera vez solo se marcan como vistas (para no llenar de avisos viejos)
        val primeraVez = !prefs.getBoolean(INICIADO, false)
        val ayer = LocalDate.now().minusDays(1).toString()

        val pacientes = db.collection("usuarios").document(uid).collection("pacientes").get().esperar()
        for (p in pacientes.documents) {
            val nombre = p.getString("nombre") ?: "Tu paciente"
            val tomas = runCatching {
                db.collection("pacientes").document(p.id).collection("tomas")
                    .whereGreaterThanOrEqualTo("fecha", ayer).get().esperar()
            }.getOrNull() ?: continue   // si ya lo quitó de su equipo, no hay permiso: se salta
            val nombreActual = runCatching {
                db.collection("pacientes").document(p.id).get().esperar().getString("nombre")
            }.getOrNull() ?: nombre
            for (t in tomas.documents) {
                if (t.getBoolean("tomada") != false) continue
                val clave = "${p.id}/${t.id}"
                if (clave in vistas) continue
                vistas += clave
                if (!primeraVez) {
                    avisar(
                        context,
                        id = clave.hashCode(),
                        titulo = "$nombreActual no se tomó su ${t.getString("medicina") ?: "pastilla"}",
                        texto = buildString {
                            append(t.getString("motivo") ?: "Sin motivo")
                            t.getLong("minutos")?.let { append(" · ").append(horaTexto(it.toInt())) }
                            if (t.getString("fecha") == ayer) append(" de ayer")
                        }
                    )
                }
            }
        }
        // Solo se guardan las de los últimos días
        val recientes = vistas.filter { it.substringAfterLast('_') >= ayer }.toSet()
        prefs.edit().putStringSet(VISTAS, recientes).putBoolean(INICIADO, true).apply()
    }

    private fun avisar(context: Context, id: Int, titulo: String, texto: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Pastillas no tomadas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Te avisa cuando alguien que acompañas no se tomó una pastilla"
                }
            )
        }
        val abrir = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setColor(0xFF004EE0.toInt())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
