package com.utom.buenvivir.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.utom.buenvivir.data.db.TomaEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

/** Alguien que acompaña al paciente: su médico o un familiar (lo agregan con su código). */
data class MiembroEquipo(val id: String, val nombre: String, val rol: String, val detalle: String) {
    val esMedico get() = rol == SesionStore.MEDICO
}

/**
 * Manda las tomas del paciente a Firebase (Firestore) para que después las vean su familia y su médico.
 * Si no hay internet, Firestore las guarda en el celular y las sube solo cuando regresa la conexión.
 *
 * Cómo queda en la nube:
 *   pacientes/{uid}                      -> nombre, ultimaConexion
 *   pacientes/{uid}/tomas/{horario_fecha} -> medicina, fecha, hora, tomada, motivo, registradaEn
 *   pacientes/{uid}/equipo/{uidMedico}   -> rol, nombre, detalle, codigo, desde
 */
object Nube {
    private const val TAG = "Nube"
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    // Para no crear dos usuarios si la app pide entrar dos veces al mismo tiempo
    private var entrando: Task<AuthResult>? = null

    /** Entra con un usuario anónimo (sin correo ni contraseña) y luego corre [despues] con su id. */
    @Synchronized
    private fun conUsuario(alFallar: () -> Unit = {}, despues: (String) -> Unit) {
        auth.currentUser?.let { despues(it.uid); return }
        val tarea = entrando ?: auth.signInAnonymously().also { entrando = it }
        tarea
            .addOnSuccessListener { r -> r.user?.uid?.let(despues) ?: alFallar() }
            .addOnFailureListener {
                entrando = null
                Log.w(TAG, "No se pudo entrar a Firebase", it)
                alFallar()
            }
    }

    // Sin 0, O, 1, I ni L, para que no se confundan al leerlos o dictarlos
    private const val LETRAS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    /**
     * El código del paciente para que su médico o su familia se vinculen.
     * La primera vez se crea uno al azar (necesita internet); después siempre es el mismo.
     * Llama a [listo] con el código, o con null si no se pudo.
     *
     *   codigos/{codigo} -> paciente (su uid)
     */
    fun miCodigo(nombre: String, listo: (String?) -> Unit) = conUsuario(alFallar = { listo(null) }) { uid ->
        db.collection("pacientes").document(uid).get()
            .addOnSuccessListener { doc ->
                val guardado = doc.getString("codigo")
                if (guardado != null) {
                    // El código guarda el nombre, para que el médico vea "Se encontró a Don José"
                    db.collection("codigos").document(guardado)
                        .set(mapOf("paciente" to uid, "nombre" to nombre), SetOptions.merge())
                    listo(guardado)
                } else crearCodigo(uid, nombre, intentos = 5, listo)
            }
            .addOnFailureListener {
                Log.w(TAG, "No se pudo leer el código", it)
                listo(null)
            }
    }

    private fun crearCodigo(uid: String, nombre: String, intentos: Int, listo: (String?) -> Unit) {
        if (intentos == 0) { listo(null); return }
        val codigo = (1..6).map { LETRAS.random() }.joinToString("")
        val refCodigo = db.collection("codigos").document(codigo)
        val refPaciente = db.collection("pacientes").document(uid)
        db.runTransaction { tx ->
            // Si por casualidad ya existe ese código, se intenta con otro
            if (tx.get(refCodigo).exists()) return@runTransaction false
            tx.set(refCodigo, mapOf("paciente" to uid, "nombre" to nombre, "creado" to Timestamp.now()))
            tx.set(refPaciente, mapOf("codigo" to codigo), SetOptions.merge())
            true
        }.addOnSuccessListener { creado ->
            if (creado) listo(codigo) else crearCodigo(uid, nombre, intentos - 1, listo)
        }.addOnFailureListener {
            Log.w(TAG, "No se pudo crear el código", it)
            listo(null)
        }
    }

    /** Su médico y sus familiares, en vivo: si alguien se vincula o se quita, se actualiza solo. */
    fun escucharEquipo(): Flow<List<MiembroEquipo>> = callbackFlow {
        var registro: ListenerRegistration? = null
        conUsuario(alFallar = { trySend(emptyList()) }) { uid ->
            registro = db.collection("pacientes").document(uid).collection("equipo")
                .addSnapshotListener { snap, error ->
                    if (error != null) Log.w(TAG, "No se pudo leer el equipo", error)
                    snap ?: return@addSnapshotListener
                    trySend(snap.documents.map {
                        MiembroEquipo(
                            id = it.id,
                            nombre = it.getString("nombre") ?: "",
                            rol = it.getString("rol") ?: "",
                            detalle = it.getString("detalle") ?: ""
                        )
                    })
                }
        }
        awaitClose { registro?.remove() }
    }

    /** El paciente quita a alguien: ya no podrá ver sus tomas. */
    fun quitarDelEquipo(id: String) = conUsuario { uid ->
        db.collection("pacientes").document(uid).collection("equipo").document(id).delete()
            .addOnFailureListener { Log.w(TAG, "No se pudo quitar", it) }
    }

    private fun tomasDe(uid: String) =
        db.collection("pacientes").document(uid).collection("tomas")

    private fun datos(t: TomaEntity, medicina: String) = mapOf(
        "medicina" to medicina,
        "medicinaId" to t.medicinaId,
        "horarioId" to t.horarioId,
        "fecha" to t.fecha,               // "2026-10-19", sirve para buscar por días
        "hora" to horaTexto(t.minutos),   // la hora que tocaba: "8:00 p. m."
        "minutos" to t.minutos,
        "tomada" to t.tomada,
        "motivo" to t.motivo,
        "registradaEn" to Timestamp(Date(t.registradaEn))
    )

    /** Al abrir la app: entra y guarda el nombre del paciente. */
    fun entrar(nombre: String) = conUsuario { uid ->
        db.collection("pacientes").document(uid)
            .set(mapOf("nombre" to nombre, "ultimaConexion" to Timestamp.now()), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar el paciente", it) }
    }

    /** Sube una toma. Un documento por horario y día: si corrige su respuesta, se reemplaza. */
    fun subirToma(t: TomaEntity, medicina: String) = conUsuario { uid ->
        tomasDe(uid).document("${t.horarioId}_${t.fecha}")
            .set(datos(t, medicina))
            .addOnFailureListener { Log.w(TAG, "No se pudo subir la toma", it) }
    }

    /** Borra todas las tomas de la nube y sube [nuevas] (para reiniciar la demostración). */
    fun reemplazarTomas(nuevas: List<Pair<TomaEntity, String>>) = conUsuario { uid ->
        val col = tomasDe(uid)
        col.get().addOnSuccessListener { actuales ->
            val lote = db.batch()
            actuales.forEach { lote.delete(it.reference) }
            nuevas.forEach { (t, medicina) -> lote.set(col.document("${t.horarioId}_${t.fecha}"), datos(t, medicina)) }
            lote.commit().addOnFailureListener { Log.w(TAG, "No se pudo reiniciar en la nube", it) }
        }.addOnFailureListener { Log.w(TAG, "No se pudieron leer las tomas", it) }
    }
}
