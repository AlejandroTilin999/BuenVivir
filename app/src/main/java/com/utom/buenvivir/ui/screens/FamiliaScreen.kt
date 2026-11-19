package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.MiembroEquipo
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.components.*
import com.utom.buenvivir.ui.theme.*

/** Pantalla 6: quién acompaña al paciente (su médico y su familia), en vivo desde Firebase. */
@Composable
fun FamiliaScreen(
    equipo: List<MiembroEquipo>,
    onEscuchar: () -> Unit = {},
    onAgregar: () -> Unit = {},
    onQuitar: (MiembroEquipo) -> Unit = {},
    onIrInicio: () -> Unit = {},
    onIrMedicinas: () -> Unit = {}
) {
    var porQuitar by remember { mutableStateOf<MiembroEquipo?>(null) }
    val medicos = equipo.filter { it.esMedico }
    val familia = equipo.filterNot { it.esMedico }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tono.Blanco)
    ) {
        SisEncabezado("Mi familia", onEscuchar = onEscuchar) {
            val total = equipo.size
            Text(
                if (total == 0) "Ellos ven si tomaste tus pastillas para poder ayudarte."
                else "${if (total == 1) "1 persona te acompaña" else "$total personas te acompañan"}. " +
                    "Ven si tomaste tus pastillas para poder ayudarte.",
                style = Letra.Texto,
                color = Tono.Suave
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.XL)
        ) {
            if (equipo.isEmpty()) {
                SisCaja {
                    Text(
                        "Todavía no agregas a nadie. Toca el botón de abajo y dale tu código a tu médico o a un familiar.",
                        style = Letra.Texto,
                        color = Tono.Suave
                    )
                }
            }
            if (medicos.isNotEmpty()) {
                SisSeccion(if (medicos.size == 1) "Tu médico" else "Tus médicos") {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                        medicos.forEach { Persona(it, esMedico = true) { porQuitar = it } }
                    }
                }
            }
            if (familia.isNotEmpty()) {
                SisSeccion("Quienes te cuidan") {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                        familia.forEach { Persona(it, esMedico = false) { porQuitar = it } }
                    }
                }
            }

            // Abre "Mi código" para que se vinculen su médico o un familiar
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                SisBoton("Agregar a alguien", onAgregar, icono = Iconos.Mas)
                Text(
                    "Te mostraremos tu código para que se lo des.",
                    style = Letra.Etiqueta,
                    color = Tono.Suave,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        MenuInferior(seleccionado = 2, onInicio = onIrInicio, onMedicinas = onIrMedicinas, onFamilia = {})
    }

    porQuitar?.let { m ->
        AlertDialog(
            onDismissRequest = { porQuitar = null },
            title = { Text("¿Quitar a ${m.nombre}?", style = Letra.Subtitulo) },
            text = { Text("Ya no podrá ver si tomaste tus pastillas.", style = Letra.Texto) },
            confirmButton = {
                TextButton(onClick = { porQuitar = null; onQuitar(m) }) {
                    Text("Sí, quitar", style = Letra.TextoFuerte, color = Tono.Acento)
                }
            },
            dismissButton = {
                TextButton(onClick = { porQuitar = null }) {
                    Text("Cancelar", style = Letra.TextoFuerte, color = Tono.Suave)
                }
            },
            containerColor = Tono.Blanco
        )
    }
}

/** Una persona del equipo: inicial (o estetoscopio), nombre, detalle y "Quitar". */
@Composable
private fun Persona(m: MiembroEquipo, esMedico: Boolean, onQuitar: () -> Unit) {
    val forma = RoundedCornerShape(Medida.RadioCaja)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Medida.Fila)
            .border(1.dp, Tono.Borde, forma)
            .padding(start = Espacio.M, top = Espacio.S, bottom = Espacio.S),
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Medida.Tactil)
                .clip(CircleShape)
                .background(Tono.Chip),
            contentAlignment = Alignment.Center
        ) {
            if (esMedico) {
                Icon(Iconos.Estetoscopio, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(24.dp))
            } else {
                Text(m.nombre.take(1).uppercase(), style = Letra.Subtitulo, color = Tono.Acento)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(m.nombre, style = Letra.TextoFuerte, color = Tono.Noche)
            if (m.detalle.isNotBlank()) Text(m.detalle, style = Letra.Etiqueta, color = Tono.Suave)
        }
        Box(
            modifier = Modifier
                .heightIn(min = Medida.Tactil)
                .clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "Quitar a ${m.nombre}", onClick = onQuitar)
                .padding(horizontal = Espacio.M),
            contentAlignment = Alignment.Center
        ) {
            Text("Quitar", style = Letra.EtiquetaFuerte, color = Tono.Acento)
        }
    }
}

@Preview(name = "Mi familia", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewFamilia() = BuenVivirTheme {
    FamiliaScreen(
        listOf(
            MiembroEquipo("1", "Laura Méndez", "medico", "Centro de Salud"),
            MiembroEquipo("2", "María", "familiar", "")
        )
    )
}

@Preview(name = "Mi familia · vacía", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewFamiliaVacia() = BuenVivirTheme { FamiliaScreen(emptyList()) }
