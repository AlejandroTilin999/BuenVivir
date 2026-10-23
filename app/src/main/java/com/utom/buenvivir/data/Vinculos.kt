package com.utom.buenvivir.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate

/** Un paciente que el médico ya vinculó. */
data class PacienteVinculado(val id: String, val nombre: String, val desde: Long)

/** Una toma tal como el paciente la subió a la nube. */
data class TomaNube(
    val fecha: LocalDate,
    val minutos: Int,
    val medicina: String,
    val tomada: Boolean,
    val motivo: String?,
    val registradaEn: Long
)

/** Lo que pasó al buscar un código. */
sealed interface Busqueda {
    data class Encontrado(val id: String, val nombre: String, val codigo: String) : Busqueda
    data class Mal(val mensaje: String) : Busqueda
}

/** Las tomas de un paciente, vistas por su médico. */
sealed interface TomasPaciente {
    data object Cargando : TomasPaciente
    data class Listas(val tomas: List<TomaNube>) : TomasPaciente
    /** El paciente lo quitó de su equipo: ya no puede ver sus datos. */
    data object SinPermiso : TomasPaciente
}

/**
 * Lo que hace el médico con sus pacientes: buscar el código, vincularse, ver sus tomas y quitarlo.
 *
 *   usuarios/{uidMedico}/pacientes/{uidPaciente} -> nombre, codigo, desde   (su lista)
 *   pacientes/{uidPaciente}/equipo/{uidMedico}  -> rol, nombre, detalle, codigo, desde (el permiso)
 */
object Vinculos {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val miId get() = auth.currentUser?.uid ?: ""

    private fun miLista() = db.collection("usuarios").document(miId).collection("pacientes")

    suspend fun buscar(codigoEscrito: String): Busqueda {
        // Acepta "k7m 2q9", "K7M2Q9"... y lo deja como "K7M2Q9"
        val codigo = codigoEscrito.uppercase().filter { it.isLetterOrDigit() }
        if (codigo.length != 6) return Busqueda.Mal("El código tiene 6 letras y números.")
        return try {
            val doc = db.collection("codigos").document(codigo).get().esperar()
            val paciente = doc.getString("paciente")
                ?: return Busqueda.Mal("No encontramos ese código. Revisa que esté bien escrito.")
            if (miLista().document(paciente).get().esperar().exists()) {
                return Busqueda.Mal("Ya tienes vinculado a este paciente.")
            }
            Busqueda.Encontrado(paciente, doc.getString("nombre") ?: "Paciente", codigo)
        } catch (e: Exception) {
            Busqueda.Mal(Cuenta.mensajeDe(e))
        }
    }

    suspend fun vincular(p: Busqueda.Encontrado): Resultado = try {
        val perfil = Cuenta.miPerfil()
        val ahora = Timestamp.now()
        db.batch()
            // El permiso: con esto las reglas de Firebase le dejan leer sus tomas
            .set(
                db.collection("pacientes").document(p.id).collection("equipo").document(miId),
                mapOf(
                    "rol" to SesionStore.MEDICO,
                    "nombre" to (perfil?.nombre ?: ""),
                    "detalle" to (perfil?.lugar ?: ""),
                    "codigo" to p.codigo,
                    "desde" to ahora
                )
            )
            // Su lista de pacientes
            .set(miLista().document(p.id), mapOf("nombre" to p.nombre, "codigo" to p.codigo, "desde" to ahora))
            .commit()
            .esperar()
        Resultado.Bien
    } catch (e: Exception) {
        Resultado.Mal(Cuenta.mensajeDe(e))
    }

    suspend fun desvincular(pacienteId: String): Resultado = try {
        db.batch()
            .delete(db.collection("pacientes").document(pacienteId).collection("equipo").document(miId))
            .delete(miLista().document(pacienteId))
            .commit()
            .esperar()
        Resultado.Bien
    } catch (e: Exception) {
        Resultado.Mal(Cuenta.mensajeDe(e))
    }

    /** Su lista de pacientes, en vivo. null mientras carga. */
    fun misPacientes(): Flow<List<PacienteVinculado>?> = callbackFlow {
        trySend(null)
        val registro = miLista().addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            trySend(snap.documents.map {
                PacienteVinculado(
                    id = it.id,
                    nombre = it.getString("nombre") ?: "Paciente",
                    desde = it.getTimestamp("desde")?.toDate()?.time ?: 0L
                )
            }.sortedBy { it.nombre })
        }
        awaitClose { registro.remove() }
    }

    /** Las tomas del paciente desde [desde], en vivo: si marca "No me la tomé", aparece al instante. */
    fun tomas(pacienteId: String, desde: LocalDate): Flow<TomasPaciente> = callbackFlow {
        trySend(TomasPaciente.Cargando)
        val registro = db.collection("pacientes").document(pacienteId).collection("tomas")
            .whereGreaterThanOrEqualTo("fecha", desde.toString())
            .addSnapshotListener { snap, error ->
                if (error?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(TomasPaciente.SinPermiso)
                    return@addSnapshotListener
                }
                snap ?: return@addSnapshotListener
                val lista = snap.documents.mapNotNull { d ->
                    val fecha = d.getString("fecha")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: return@mapNotNull null
                    TomaNube(
                        fecha = fecha,
                        minutos = d.getLong("minutos")?.toInt() ?: 0,
                        medicina = d.getString("medicina") ?: "",
                        tomada = d.getBoolean("tomada") ?: false,
                        motivo = d.getString("motivo"),
                        registradaEn = d.getTimestamp("registradaEn")?.toDate()?.time ?: 0L
                    )
                }
                trySend(TomasPaciente.Listas(lista))
            }
        awaitClose { registro.remove() }
    }
}
