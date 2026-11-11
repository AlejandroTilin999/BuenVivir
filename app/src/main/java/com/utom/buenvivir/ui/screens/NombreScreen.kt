package com.utom.buenvivir.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Preview
import com.utom.buenvivir.ui.components.CampoLimpio
import com.utom.buenvivir.ui.components.FormularioConFrasco
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.BuenVivirTheme

/**
 * El paciente escribe su nombre: la primera vez que abre la app, o desde Ajustes → Mi perfil.
 * Con ese nombre lo saluda la app y así lo ven su médico y quienes lo cuidan. Estilo limpio.
 */
@Composable
fun NombreScreen(
    inicial: String = "",
    editando: Boolean = false,
    onRegresar: () -> Unit = {},
    onListo: (String) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(inicial) }
    var revisar by remember { mutableStateOf(false) }
    val listo: () -> Unit = {
        revisar = true
        if (nombre.isNotBlank()) onListo(nombre.trim())
    }
    // La primera vez, el botón de atrás del celular regresa a Elegir rol
    BackHandler(enabled = !editando, onBack = onRegresar)

    FormularioConFrasco(
        etiquetaRegresar = if (editando) "Regresar a Ajustes" else "Cambiar rol",
        onRegresar = onRegresar,
        chipIcono = Iconos.Persona,
        chipTexto = "Paciente",
        tituloAntes = if (editando) "Mi " else "¿Cómo te ",
        tituloAcento = if (editando) "nombre" else "llamas?",
        subtitulo = "Así te saludará la app y te verán quienes te cuidan.",
        boton = if (editando) "Guardar" else "Continuar",
        onBoton = listo,
        mostrarCifrado = false
    ) { alTocar ->
        CampoLimpio(
            "Tu nombre", nombre, { nombre = it },
            ultimo = true,
            onListo = listo,
            ayuda = "Por ejemplo: Don José o Doña María",
            error = if (revisar && nombre.isBlank()) "Escribe tu nombre para continuar" else null,
            modifier = alTocar
        )
    }
}

@Preview(name = "¿Cómo te llamas?", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewNombre() = BuenVivirTheme { NombreScreen(onListo = {}) }
