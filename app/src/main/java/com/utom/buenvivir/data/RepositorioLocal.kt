package com.utom.buenvivir.data

import android.content.Context
import com.utom.buenvivir.data.db.BuenVivirDao
import com.utom.buenvivir.data.db.BuenVivirDb
import com.utom.buenvivir.data.db.HorarioEntity
import com.utom.buenvivir.data.db.MedicinaEntity
import com.utom.buenvivir.data.db.TomaEntity
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lee y guarda en la base de datos del celular (Room).
 * Cada toma también se manda a Firebase (ver [Nube]) para la familia y el médico.
 */
class RepositorioLocal private constructor(private val dao: BuenVivirDao) {

    val medicinas = dao.medicinas()

    fun tomasDesde(fecha: LocalDate) = dao.tomasDesde(fecha.toString())

    fun ultimasTomas(medicinaId: Long, limite: Int = 5) = dao.ultimasTomas(medicinaId, limite)

    /** Registra si se tomó o no una pastilla. Si ya estaba registrada, la corrige. */
    suspend fun registrar(horario: HorarioEntity, fecha: LocalDate, tomada: Boolean, motivo: String?) {
        val toma = TomaEntity(
            medicinaId = horario.medicinaId,
            horarioId = horario.id,
            fecha = fecha.toString(),
            minutos = horario.minutos,
            tomada = tomada,
            motivo = if (tomada) null else motivo
        )
        dao.guardarToma(toma)
        // También a la nube, para la familia y el médico
        Nube.subirToma(toma, dao.medicina(horario.medicinaId)?.nombre ?: "")
    }

    suspend fun horarios() = dao.horarios()
    suspend fun horario(id: Long) = dao.horario(id)
    suspend fun medicina(id: Long) = dao.medicina(id)
    suspend fun toma(horarioId: Long, fecha: LocalDate) = dao.toma(horarioId, fecha.toString())

    /** Borra todas las tomas y vuelve a cargar la semana de demostración (para la feria). */
    suspend fun reiniciarDemo() {
        dao.borrarTomas()
        val hs = dao.horarios()
        val demo = if (CARGAR_DEMO && hs.size >= 2) cargarDemo(hs[0].medicinaId, hs[0].id, hs[1].id) else emptyList()
        Nube.reemplazarTomas(conNombre(demo))
    }

    /** Junta cada toma con el nombre de su medicina, como se guarda en la nube. */
    private suspend fun conNombre(tomas: List<TomaEntity>): List<Pair<TomaEntity, String>> {
        val nombres = tomas.map { it.medicinaId }.distinct()
            .associateWith { dao.medicina(it)?.nombre ?: "" }
        return tomas.map { it to (nombres[it.medicinaId] ?: "") }
    }

    /** La primera vez que se abre la app, carga la medicina (y la semana de demostración). */
    suspend fun prepararSiVacia() {
        if (dao.contarMedicinas() > 0) return

        val medicinaId = dao.insertarMedicina(
            MedicinaEntity(
                nombre = "Metformina",
                para = "Para controlar el azúcar",
                cantidad = "1 pastilla blanca",
                indicacion = "después del desayuno y después de la cena",
                video = "¿Para qué sirve la metformina?"
            )
        )
        val idsHorarios = dao.insertarHorarios(
            listOf(
                HorarioEntity(medicinaId = medicinaId, minutos = 8 * 60, indicacion = "después del desayuno"),
                HorarioEntity(medicinaId = medicinaId, minutos = 20 * 60, indicacion = "después de cenar")
            )
        )
        if (CARGAR_DEMO) Nube.reemplazarTomas(conNombre(cargarDemo(medicinaId, idsHorarios[0], idsHorarios[1])))
    }

    /**
     * Semana de DEMOSTRACIÓN para la feria (no son datos reales): los últimos 6 días tomadas,
     * menos una mañana ("No tenía la medicina") y una noche ("Se me olvidó"). Hoy queda pendiente.
     */
    private suspend fun cargarDemo(medicinaId: Long, manana: Long, noche: Long): List<TomaEntity> {
        val hoy = LocalDate.now()
        // Hora real de ejemplo: unos minutos después de la hora que tocaba
        fun registro(fecha: LocalDate, minutos: Int, retraso: Int): Long =
            fecha.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + (minutos + retraso) * 60_000L
        val tomas = mutableListOf<TomaEntity>()
        for (diasAtras in 6 downTo 1) {
            val dia = hoy.minusDays(diasAtras.toLong())
            val fecha = dia.toString()
            val faltoManana = diasAtras == 6
            val faltoNoche = diasAtras == 1
            tomas += TomaEntity(
                medicinaId = medicinaId, horarioId = manana, fecha = fecha, minutos = 8 * 60,
                tomada = !faltoManana, motivo = if (faltoManana) "No tenía la medicina" else null,
                registradaEn = registro(dia, 8 * 60, 5 + diasAtras * 3)
            )
            tomas += TomaEntity(
                medicinaId = medicinaId, horarioId = noche, fecha = fecha, minutos = 20 * 60,
                tomada = !faltoNoche, motivo = if (faltoNoche) "Se me olvidó" else null,
                registradaEn = registro(dia, 20 * 60, 10 + diasAtras * 4)
            )
        }
        // Hoy queda sin registrar, para que al reiniciar siempre aparezca "Tomar pastilla"
        dao.guardarTomas(tomas)
        return tomas
    }

    companion object {
        /** Cambia a false para empezar sin historial. */
        const val CARGAR_DEMO = true

        @Volatile private var instancia: RepositorioLocal? = null

        fun get(context: Context): RepositorioLocal = instancia ?: synchronized(this) {
            instancia ?: RepositorioLocal(BuenVivirDb.get(context).dao()).also { instancia = it }
        }
    }
}
