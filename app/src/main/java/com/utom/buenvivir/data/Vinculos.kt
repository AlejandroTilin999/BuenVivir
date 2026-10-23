package com.utom.buenvivir.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
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
 *   pacientes/{uidPaciente}/marcas/medico        -> uid, desde  (existe si el paciente tiene médico;
 *                                                   así el cuidador ya no puede cambiar sus medicinas)
 */
object Vinculos {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val miId get() = auth.currentUser?.uid ?: ""

    private fun miLista() = db.collection("usuarios").document(miId).collection("pacientes")

    private fun marcaMedico(pacienteId: String) =
        db.collection("pacientes").document(pacienteId).collection("marcas").document("medico")

    /** El médico deja la marca "este paciente ya tiene médico" (al vincularse y cada vez que lo abre). */
    suspend fun marcarComoSuMedico(pacienteId: String) {
        try {
            marcaMedico(pacienteId).set(mapOf("uid" to miId, "desde" to Timestamp.now())).esperar()
        } catch (_: Exception) {
            // Sin internet o sin permiso: se intenta otra vez la próxima vez que lo abra
        }
    }

    /** Para el cuidador: true si el paciente tiene médico (entonces solo el médico cambia medicinas). */
    fun tieneMedico(pacienteId: String): Flow<Boolean?> = callbackFlow {
        val registro = marcaMedico(pacienteId).addSnapshotListener { snap, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            snap ?: return@addSnapshotListener
            trySend(snap.exists())
        }
        awaitClose { registro.remove() }
    }

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
                    "rol" to (perfil?.rol ?: SesionStore.MEDICO),
                    "nombre" to (perfil?.nombre ?: ""),
                    // Lo que ve el paciente debajo del nombre: dónde da consulta, o "Hija", "Nieto"...
                    "detalle" to (if (perfil?.esFamiliar == true) perfil.parentesco else perfil?.lugar ?: ""),
                    "codigo" to p.codigo,
                    "desde" to ahora
                )
            )
            // Su lista de pacientes
            .set(miLista().document(p.id), mapOf("nombre" to p.nombre, "codigo" to p.codigo, "desde" to ahora))
            .commit()
            .esperar()
        if (perfil?.esFamiliar != true) marcarComoSuMedico(p.id)
        Resultado.Bien
    } catch (e: Exception) {
        Resultado.Mal(Cuenta.mensajeDe(e))
    }

    suspend fun desvincular(pacienteId: String): Resultado = try {
        // Si era su médico, se quita la marca para que su cuidador pueda volver a agregar medicinas
        val eraSuMedico = try {
            marcaMedico(pacienteId).get().esperar().getString("uid") == miId
        } catch (_: Exception) {
            false
        }
        val lote = db.batch()
        if (eraSuMedico) lote.delete(marcaMedico(pacienteId))
        lote.delete(db.collection("pacientes").document(pacienteId).collection("equipo").document(miId))
            .delete(miLista().document(pacienteId))
            .commit()
            .esperar()
        Resultado.Bien
    } catch (e: Exception) {
        Resultado.Mal(Cuenta.mensajeDe(e))
    }

    /**
     * Su lista de pacientes, en vivo. null mientras carga.
     * El nombre se lee del paciente mismo: si lo cambia en su app, aquí se actualiza solo.
     */
    fun misPacientes(): Flow<List<PacienteVinculado>?> = callbackFlow {
        trySend(null)
        var lista = emptyList<PacienteVinculado>()
        val nombres = mutableMapOf<String, String>()
        val escuchas = mutableMapOf<String, ListenerRegistration>()

        fun enviar() {
            trySend(lista.map { p -> p.copy(nombre = nombres[p.id] ?: p.nombre) }.sortedBy { it.nombre })
        }

        val registro = miLista().addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            lista = snap.documents.map {
                PacienteVinculado(
                    id = it.id,
                    nombre = it.getString("nombre") ?: "Paciente",
                    desde = it.getTimestamp("desde")?.toDate()?.time ?: 0L
                )
            }
            val ids = lista.map { it.id }.toSet()
            // Deja de escuchar a los que ya no están
            (escuchas.keys - ids).forEach { id -> escuchas.remove(id)?.remove(); nombres.remove(id) }
            // Escucha el nombre de los nuevos
            ids.filter { it !in escuchas }.forEach { id ->
                escuchas[id] = db.collection("pacientes").document(id).addSnapshotListener { doc, _ ->
                    val nombre = doc?.getString("nombre")
                    if (!nombre.isNullOrBlank() && nombres[id] != nombre) {
                        nombres[id] = nombre
                        enviar()
                    }
                }
            }
            enviar()
        }
        awaitClose {
            registro.remove()
            escuchas.values.forEach { it.remove() }
        }
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
