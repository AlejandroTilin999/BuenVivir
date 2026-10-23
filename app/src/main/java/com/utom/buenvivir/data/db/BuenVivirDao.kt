package com.utom.buenvivir.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BuenVivirDao {

    @Transaction
    @Query("SELECT * FROM medicinas ORDER BY id")
    fun medicinas(): Flow<List<MedicinaConHorarios>>

    /** Las tomas desde una fecha (por ejemplo, desde el lunes de esta semana). */
    @Query("SELECT * FROM tomas WHERE fecha >= :desde ORDER BY fecha, minutos")
    fun tomasDesde(desde: String): Flow<List<TomaEntity>>

    /** Las últimas tomas de una medicina, de la más nueva a la más vieja. */
    @Query("SELECT * FROM tomas WHERE medicinaId = :medicinaId ORDER BY fecha DESC, minutos DESC LIMIT :limite")
    fun ultimasTomas(medicinaId: Long, limite: Int): Flow<List<TomaEntity>>

    /** Guarda una toma; si ya había una para ese horario y día, la reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarToma(toma: TomaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTomas(tomas: List<TomaEntity>)

    @Insert
    suspend fun insertarMedicina(medicina: MedicinaEntity): Long

    @Insert
    suspend fun insertarHorarios(horarios: List<HorarioEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM medicinas")
    suspend fun contarMedicinas(): Int

    // ---- Lecturas de una sola vez (para las alarmas) ----

    @Query("SELECT * FROM horarios ORDER BY minutos")
    suspend fun horarios(): List<HorarioEntity>

    @Query("SELECT * FROM horarios WHERE id = :id")
    suspend fun horario(id: Long): HorarioEntity?

    @Query("SELECT * FROM medicinas WHERE id = :id")
    suspend fun medicina(id: Long): MedicinaEntity?

    @Query("SELECT * FROM tomas WHERE horarioId = :horarioId AND fecha = :fecha LIMIT 1")
    suspend fun toma(horarioId: Long, fecha: String): TomaEntity?

    @Query("DELETE FROM tomas")
    suspend fun borrarTomas()

    // ---- Para copiar las medicinas que vienen de Firebase (receta del médico) ----

    @Query("SELECT * FROM medicinas")
    suspend fun todasMedicinas(): List<MedicinaEntity>

    @Query("SELECT * FROM horarios WHERE medicinaId = :medicinaId")
    suspend fun horariosDe(medicinaId: Long): List<HorarioEntity>

    @Update
    suspend fun actualizarMedicina(medicina: MedicinaEntity)

    @Update
    suspend fun actualizarHorario(horario: HorarioEntity)

    @Insert
    suspend fun insertarHorario(horario: HorarioEntity): Long

    @Query("DELETE FROM horarios WHERE id = :id")
    suspend fun borrarHorario(id: Long)

    /** Sus horarios se borran solos (CASCADE). */
    @Query("DELETE FROM medicinas WHERE id = :id")
    suspend fun borrarMedicina(id: Long)
}
