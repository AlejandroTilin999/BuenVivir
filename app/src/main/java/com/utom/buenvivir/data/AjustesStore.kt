package com.utom.buenvivir.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ajustes")

/** Guarda los ajustes en el celular para que no se pierdan al cerrar la app. */
class AjustesStore(private val context: Context) {

    private object K {
        val sonido = booleanPreferencesKey("sonido")
        val vibracion = booleanPreferencesKey("vibracion")
        val tono = stringPreferencesKey("tono")
        val repetir = intPreferencesKey("repetir_minutos")
        val volumen = intPreferencesKey("volumen")
        val avisarReloj = booleanPreferencesKey("avisar_reloj")
        val vibrarReloj = booleanPreferencesKey("vibrar_reloj")
        val sonarReloj = booleanPreferencesKey("sonar_reloj")
        val registrarReloj = booleanPreferencesKey("registrar_reloj")
        val oscuro = booleanPreferencesKey("tema_oscuro")
        val vozLenta = booleanPreferencesKey("voz_lenta")
    }

    val ajustes: Flow<Ajustes> = context.dataStore.data.map { p ->
        val d = Ajustes()
        Ajustes(
            sonido = p[K.sonido] ?: d.sonido,
            vibracion = p[K.vibracion] ?: d.vibracion,
            tono = p[K.tono] ?: d.tono,
            repetirMinutos = p[K.repetir] ?: d.repetirMinutos,
            volumen = p[K.volumen] ?: d.volumen,
            avisarEnReloj = p[K.avisarReloj] ?: d.avisarEnReloj,
            vibrarEnReloj = p[K.vibrarReloj] ?: d.vibrarEnReloj,
            sonarEnReloj = p[K.sonarReloj] ?: d.sonarEnReloj,
            registrarDesdeReloj = p[K.registrarReloj] ?: d.registrarDesdeReloj,
            temaOscuro = p[K.oscuro] ?: d.temaOscuro,
            vozLenta = p[K.vozLenta] ?: d.vozLenta
        )
    }

    suspend fun guardar(a: Ajustes) {
        context.dataStore.edit { p ->
            p[K.sonido] = a.sonido
            p[K.vibracion] = a.vibracion
            p[K.tono] = a.tono
            p[K.repetir] = a.repetirMinutos
            p[K.volumen] = a.volumen
            p[K.avisarReloj] = a.avisarEnReloj
            p[K.vibrarReloj] = a.vibrarEnReloj
            p[K.sonarReloj] = a.sonarEnReloj
            p[K.registrarReloj] = a.registrarDesdeReloj
            p[K.oscuro] = a.temaOscuro
            p[K.vozLenta] = a.vozLenta
        }
    }
}
