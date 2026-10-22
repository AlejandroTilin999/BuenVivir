package com.utom.buenvivir.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MedicinaEntity::class, HorarioEntity::class, TomaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BuenVivirDb : RoomDatabase() {
    abstract fun dao(): BuenVivirDao

    companion object {
        @Volatile private var instancia: BuenVivirDb? = null

        /** Una sola base de datos para toda la app. */
        fun get(context: Context): BuenVivirDb = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                BuenVivirDb::class.java,
                "buenvivir.db"
            ).build().also { instancia = it }
        }
    }
}
