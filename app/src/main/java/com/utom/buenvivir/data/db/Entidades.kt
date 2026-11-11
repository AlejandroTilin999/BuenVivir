package com.utom.buenvivir.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Una medicina del paciente. */
@Entity(tableName = "medicinas")
data class MedicinaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val para: String,           // "Para controlar el azúcar"
    val cantidad: String,       // "1 pastilla blanca"
    val indicacion: String,     // "después del desayuno y después de la cena"
    val video: String,          // título del video de la medicina
    // Id de esta medicina en Firebase (pacientes/{uid}/medicinas/{nubeId}); vacío si aún no se sube
    @ColumnInfo(defaultValue = "") val nubeId: String = "",
    // Cuándo se agregó (milisegundos): las horas de ese día que ya habían pasado no cuentan. 0 = medicina de antes
    @ColumnInfo(defaultValue = "0") val creadaEn: Long = 0,
    // Primer día en que toca ("2026-11-10"); vacío = desde siempre
    @ColumnInfo(defaultValue = "") val inicio: String = "",
    // Por cuántos días se toma; 0 = siempre
    @ColumnInfo(defaultValue = "0") val dias: Int = 0
)

/** Cada hora en que toca una medicina. [minutos] es la hora del día: 8:00 p. m. = 1200. */
@Entity(
    tableName = "horarios",
    foreignKeys = [ForeignKey(
        entity = MedicinaEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicinaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicinaId")]
)
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicinaId: Long,
    val minutos: Int,
    val indicacion: String,     // "después de cenar"
    // Cómo se llama este horario en Firebase ("h0", "h1"...), para no perder su historial al editarlo
    @ColumnInfo(defaultValue = "") val clave: String = ""
)

/**
 * Lo que pasó con una toma: si se la tomó o no, y por qué.
 * Solo puede haber un registro por horario y por día; si se corrige, se reemplaza.
 */
@Entity(
    tableName = "tomas",
    indices = [Index(value = ["horarioId", "fecha"], unique = true), Index("medicinaId")]
)
data class TomaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicinaId: Long,
    val horarioId: Long,
    val fecha: String,          // "2026-10-18"
    val minutos: Int,
    val tomada: Boolean,
    val motivo: String?,        // solo si no se la tomó
    val registradaEn: Long = System.currentTimeMillis()
)

/** Una medicina junto con todos sus horarios. */
data class MedicinaConHorarios(
    @Embedded val medicina: MedicinaEntity,
    @Relation(parentColumn = "id", entityColumn = "medicinaId")
    val horarios: List<HorarioEntity>
)
