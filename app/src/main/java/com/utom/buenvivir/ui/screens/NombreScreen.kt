package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.medico.Aviso
import com.utom.buenvivir.ui.medico.AvisoMedico
import com.utom.buenvivir.ui.medico.BotonPrincipal
import com.utom.buenvivir.ui.medico.Campo
import com.utom.buenvivir.ui.medico.Regresar
import com.utom.buenvivir.ui.theme.*

/**
 * El paciente escribe su nombre: la primera vez que abre la app, o desde Ajustes → Mi perfil.
 * Con ese nombre lo saluda la app y así lo ven su médico y quienes lo cuidan.
 */
@Composable
fun NombreScreen(
    inicial: String = "",
    editando: Boolean = false,
    onRegresar: () -> Unit = {},
    onListo: (String) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(inicial) }
    var error by remember { mutableStateOf(false) }
    val listo = {
        if (nombre.isBlank()) error = true else onListo(nombre.trim())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = if (editando) 6.dp else 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (editando) Regresar("Ajustes", onRegresar)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (editando) "Mi nombre" else "¿Cómo te llamas?",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Así te saludará la app, y así te verán tu médico y quienes te cuidan.",
                fontSize = 19.sp,
                lineHeight = 26.sp,
                color = TextoSecundario
            )
        }
        Campo(
            etiqueta = "Tu nombre",
            valor = nombre,
            onCambio = { nombre = it; error = false },
            ayuda = "Por ejemplo: Don José o Doña María",
            ultimo = true,
            onListo = listo
        )
        if (error) Aviso(AvisoMedico("Escribe tu nombre para continuar.", esError = true))
        Spacer(Modifier.height(8.dp))
        BotonPrincipal(if (editando) "Guardar" else "Continuar", onClick = listo)
    }
}

@Preview(name = "¿Cómo te llamas?", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewNombre() = BuenVivirTheme { NombreScreen(onListo = {}) }
