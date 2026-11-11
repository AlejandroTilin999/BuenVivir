package com.utom.buenvivir.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

/** Una hora en que toca la medicina. [clave] la identifica aunque cambie la hora ("h0", "h1"...). */
data class HorarioNube(val clave: String, val minutos: Int, val indicacion: String)

/** Un cambio de dosis: qué tenía antes, qué tiene ahora, cuándo y quién lo cambió. */
data class CambioDosis(
    val fecha: Long,
    val antes: String,
    val despues: String,
    val quien: String,
    val rol: String = ""      // "medico", "familiar" o "paciente": quién lo cambió
)

/**
 * Una medicina del paciente como se guarda en Firebase. La puede agregar su médico,
 * un familiar o el mismo paciente ([agregadaPor] = "medico", "familiar" o "paciente").
 *
 *   pacientes/{uid}/medicinas/{id} -> nombre, para, cantidad, indicacion, horarios[], agregadaPor, quien, actualizada
 */
data class MedicinaNube(
    val id: String,
    val nombre: String,
    val para: String,
    val cantidad: String,
    val indicacion: String,
    val horarios: List<HorarioNube>,
    val agregadaPor: String = "",
    val quien: String = "",
    val cambios: List<CambioDosis> = emptyList(),  // historial de cambios de dosis (el más nuevo al final)
    val creadaEn: Long = 0,     // cuándo se agregó (milisegundos); 0 = medicina de antes
    val inicio: String = "",    // primer día en que toca ("2026-11-10"); vacío = desde siempre
    val dias: Int = 0           // por cuántos días; 0 = siempre
)

/** Leer y escribir las medicinas de un paciente en Firebase (lo usan el médico y el paciente). */
object Recetas {
    private const val TAG = "Recetas"
    private val db get() = FirebaseFirestore.getInstance()

    fun coleccion(pacienteId: String) =
        db.collection("pacientes").document(pacienteId).collection("medicinas")

    /** Un id nuevo al azar, igual a los que crea Firebase. */
    fun nuevoId(): String = db.collection("pacientes").document().id

    fun datos(m: MedicinaNube) = mapOf(
        "nombre" to m.nombre,
        "para" to m.para,
        "cantidad" to m.cantidad,
        "indicacion" to m.indicacion,
        "horarios" to m.horarios.map {
            mapOf("clave" to it.clave, "minutos" to it.minutos, "indicacion" to it.indicacion)
        },
        "agregadaPor" to m.agregadaPor,
        "quien" to m.quien,
        "cambios" to m.cambios.takeLast(MAX_CAMBIOS).map {
            mapOf(
                "fecha" to Timestamp(Date(it.fecha)), "antes" to it.antes, "despues" to it.despues,
                "quien" to it.quien, "rol" to it.rol
            )
        },
        "creadaEn" to m.creadaEn,
        "inicio" to m.inicio,
        "dias" to m.dias,
        "actualizada" to Timestamp.now()
    )

    private const val MAX_CAMBIOS = 20

    /** "1 tableta de 850 mg después de comer · 8:00 a. m. · 8:00 p. m." */
    fun dosisTexto(m: MedicinaNube): String =
        "${m.cantidad} ${m.indicacion} · " + m.horarios.joinToString(" · ") { horaTexto(it.minutos) } +
            (if (m.dias > 0) " · ${duracionTexto(m.dias).lowercase()}" else "")

    /**
     * Al guardar una medicina que ya existía: si cambió la dosis, la cantidad o las horas,
     * se agrega al historial para que el médico vea cómo ha cambiado.
     */
    fun conCambio(anterior: MedicinaNube?, nueva: MedicinaNube): MedicinaNube {
        if (anterior == null) return nueva.copy(cambios = emptyList())
        val antes = dosisTexto(anterior)
        val despues = dosisTexto(nueva)
        val cambios = if (antes == despues) anterior.cambios
        else anterior.cambios + CambioDosis(System.currentTimeMillis(), antes, despues, nueva.quien, nueva.agregadaPor)
        return nueva.copy(cambios = cambios)
    }

    fun leer(doc: DocumentSnapshot): MedicinaNube? {
        val nombre = doc.getString("nombre") ?: return null
        @Suppress("UNCHECKED_CAST")
        val horarios = (doc.get("horarios") as? List<Map<String, Any?>>).orEmpty().mapNotNull { h ->
            val minutos = (h["minutos"] as? Number)?.toInt() ?: return@mapNotNull null
            HorarioNube(
                clave = h["clave"] as? String ?: "m$minutos",
                minutos = minutos,
                indicacion = h["indicacion"] as? String ?: ""
            )
        }
        return MedicinaNube(
            id = doc.id,
            nombre = nombre,
            para = doc.getString("para") ?: "",
            cantidad = doc.getString("cantidad") ?: "",
            indicacion = doc.getString("indicacion") ?: "",
            horarios = horarios.sortedBy { it.minutos },
            agregadaPor = doc.getString("agregadaPor") ?: "",
            quien = doc.getString("quien") ?: "",
            creadaEn = doc.getLong("creadaEn") ?: 0L,
            inicio = doc.getString("inicio") ?: "",
            dias = (doc.getLong("dias") ?: 0L).toInt(),
            cambios = (doc.get("cambios") as? List<*>).orEmpty().mapNotNull { c ->
                val mapa = c as? Map<*, *> ?: return@mapNotNull null
                CambioDosis(
                    fecha = (mapa["fecha"] as? Timestamp)?.toDate()?.time ?: 0L,
                    antes = mapa["antes"] as? String ?: "",
                    despues = mapa["despues"] as? String ?: "",
                    quien = mapa["quien"] as? String ?: "",
                    rol = mapa["rol"] as? String ?: ""
                )
            }
        )
    }

    /**
     * Las medicinas de un paciente, en vivo. Manda null si hubo un error al leer
     * (así quien escucha no confunde "no se pudo leer" con "no tiene medicinas").
     */
    fun escuchar(pacienteId: String): Flow<List<MedicinaNube>?> = callbackFlow {
        val registro = coleccion(pacienteId).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "No se pudieron leer las medicinas", error)
                trySend(null)
                return@addSnapshotListener
            }
            snap ?: return@addSnapshotListener
            // Una lista vacía que todavía no viene del servidor no cuenta: podría borrar medicinas por error
            if (snap.isEmpty && snap.metadata.isFromCache) return@addSnapshotListener
            trySend(snap.documents.mapNotNull(::leer).sortedBy { it.nombre })
        }
        awaitClose { registro.remove() }
    }

    /** Agrega o corrige una medicina (si ya existe ese id, la reemplaza). */
    suspend fun guardar(pacienteId: String, m: MedicinaNube): Resultado = try {
        coleccion(pacienteId).document(m.id).set(datos(m)).esperar()
        Resultado.Bien
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo guardar la medicina", e)
        Resultado.Mal(Cuenta.mensajeDe(e))
    }

    /** Quita una medicina: al paciente se le borra de su celular y dejan de sonar sus alarmas. */
    suspend fun quitar(pacienteId: String, medicinaId: String): Resultado = try {
        coleccion(pacienteId).document(medicinaId).delete().esperar()
        Resultado.Bien
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo quitar la medicina", e)
        Resultado.Mal(Cuenta.mensajeDe(e))
    }
}
