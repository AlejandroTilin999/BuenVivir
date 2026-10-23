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
            .background(Blanco)
    ) {
        Encabezado(equipo.size, onEscuchar)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (equipo.isEmpty()) {
                Text(
                    "Todavía no agregas a nadie. Toca el botón de abajo y dale tu código a tu médico o a un familiar.",
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = TextoGris,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
                        .padding(18.dp)
                )
            }
            if (medicos.isNotEmpty()) {
                Seccion(if (medicos.size == 1) "Tu médico" else "Tus médicos") {
                    medicos.forEach { TarjetaMedico(it) { porQuitar = it } }
                }
            }
            if (familia.isNotEmpty()) {
                Seccion("Quienes te cuidan") {
                    familia.forEach { TarjetaFamiliar(it) { porQuitar = it } }
                }
            }

            // Abre "Mi código" para que se vinculen su médico o un familiar
            Button(
                onClick = onAgregar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(60.dp)
                    .shadow(8.dp, RoundedCornerShape(30.dp), ambientColor = AzulBoton, spotColor = AzulBoton),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulBoton, contentColor = Blanco),
                elevation = null
            ) {
                Icon(Iconos.Mas, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Agregar a alguien", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        MenuInferior(seleccionado = 2, onInicio = onIrInicio, onMedicinas = onIrMedicinas, onFamilia = {})
    }

    porQuitar?.let { m ->
        AlertDialog(
            onDismissRequest = { porQuitar = null },
            title = { Text("¿Quitar a ${m.nombre}?", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text("Ya no podrá ver si tomaste tus pastillas.", fontSize = 19.sp, lineHeight = 26.sp)
            },
            confirmButton = {
                TextButton(onClick = { porQuitar = null; onQuitar(m) }) {
                    Text("Sí, quitar", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AzulBoton)
                }
            },
            dismissButton = {
                TextButton(onClick = { porQuitar = null }) {
                    Text("Cancelar", fontSize = 19.sp, color = TextoGris)
                }
            },
            containerColor = Blanco
        )
    }
}

// Mismo alto que el encabezado de Inicio y Mis medicinas, para que no "brinque".
@Composable
private fun Encabezado(total: Int, onEscuchar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Lavanda)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Mi familia",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                            append(if (total == 1) "1 persona" else "$total personas")
                        }
                        append(if (total == 1) " te acompaña" else " te acompañan")
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoLavanda
                )
            }
            BotonEscuchar("Escuchar esta pantalla", Blanco, onEscuchar)
        }

        Text(
            "Ellos ven si tomaste tus pastillas para poder ayudarte.",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextoGris,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Blanco)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextoOscuro,
            modifier = Modifier.semantics { heading() }
        )
        contenido()
    }
}

@Composable
private fun TarjetaFamiliar(m: MiembroEquipo, onQuitar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Lavanda),
            contentAlignment = Alignment.Center
        ) {
            Text(m.nombre.take(1).uppercase(), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = TextoChip)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(m.nombre, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
            if (m.detalle.isNotBlank()) Text(m.detalle, fontSize = 16.sp, color = TextoSecundario)
        }
        Quitar(m.nombre, TextoChip, onQuitar)
    }
}

@Composable
private fun TarjetaMedico(m: MiembroEquipo, onQuitar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AzulClaro)
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Blanco),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Estetoscopio, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(m.nombre, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextoAzulOscuro)
            if (m.detalle.isNotBlank()) Text(m.detalle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoAzul)
        }
        Quitar(m.nombre, TextoAzulOscuro, onQuitar)
    }
}

/** Enlace "Quitar" con área grande para tocar. */
@Composable
private fun Quitar(nombre: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        "Quitar",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Quitar a $nombre", onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp)
    )
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
