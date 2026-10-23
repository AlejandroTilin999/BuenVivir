package com.utom.buenvivir.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Archivo aparte de los ajustes: aquí solo se guarda qué rol tiene este celular
private val Context.sesionStore by preferencesDataStore(name = "sesion")

/** Recuerda si este celular es de un paciente o de un médico, para abrir directo en sus pantallas. */
class SesionStore(private val context: Context) {

    private val clave = stringPreferencesKey("rol")

    /** El rol guardado, o null si todavía no se elige. */
    val rol: Flow<String?> = context.sesionStore.data.map { it[clave] }

    suspend fun guardarRol(rol: String?) {
        context.sesionStore.edit { if (rol == null) it.remove(clave) else it[clave] = rol }
    }

    companion object {
        const val PACIENTE = "paciente"
        const val MEDICO = "medico"
    }
}
