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
    private val claveNombre = stringPreferencesKey("nombre")

    /** El nombre del paciente ("Don José"), o null si todavía no lo escribe. */
    val nombre: Flow<String?> = context.sesionStore.data.map { it[claveNombre] }

    suspend fun guardarNombre(nombre: String) {
        context.sesionStore.edit { it[claveNombre] = nombre }
    }

    /** El rol guardado, o null si todavía no se elige. */
    val rol: Flow<String?> = context.sesionStore.data.map { it[clave] }

    /** Este celular deja de ser del paciente: se olvidan su rol y su nombre. */
    suspend fun olvidar() {
        context.sesionStore.edit { it.remove(clave); it.remove(claveNombre) }
    }

    suspend fun guardarRol(rol: String?) {
        context.sesionStore.edit { if (rol == null) it.remove(clave) else it[clave] = rol }
    }

    companion object {
        const val PACIENTE = "paciente"
        const val MEDICO = "medico"
        const val FAMILIAR = "familiar"
    }
}
