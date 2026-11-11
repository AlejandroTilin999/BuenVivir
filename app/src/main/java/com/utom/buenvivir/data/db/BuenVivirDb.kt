package com.utom.buenvivir.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MedicinaEntity::class, HorarioEntity::class, TomaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BuenVivirDb : RoomDatabase() {
    abstract fun dao(): BuenVivirDao

    companion object {
        @Volatile private var instancia: BuenVivirDb? = null

        /** Versión 2: cada medicina y horario recuerda su id en Firebase. No se pierde nada de lo guardado. */
        private val DE_1_A_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicinas ADD COLUMN nubeId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE horarios ADD COLUMN clave TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Versión 3: cada medicina guarda cuándo se agregó y por cuántos días se toma. No se pierde nada. */
        private val DE_2_A_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicinas ADD COLUMN creadaEn INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicinas ADD COLUMN inicio TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicinas ADD COLUMN dias INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Una sola base de datos para toda la app. */
        fun get(context: Context): BuenVivirDb = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                BuenVivirDb::class.java,
                "buenvivir.db"
            ).addMigrations(DE_1_A_2, DE_2_A_3).build().also { instancia = it }
        }
    }
}
