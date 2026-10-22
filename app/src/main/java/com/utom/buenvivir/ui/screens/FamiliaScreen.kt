package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Contacto
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.theme.*

/** Pantalla 6: quién acompaña al paciente (familia y médico). */
@Composable
fun FamiliaScreen(
    familiares: List<Contacto>,
    medico: Contacto?,
    onEscuchar: () -> Unit = {},
    onAgregar: () -> Unit = {},
    onIrInicio: () -> Unit = {},
    onIrMedicinas: () -> Unit = {}
) {
    val total = familiares.size + if (medico != null) 1 else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        Encabezado(total, onEscuchar)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (familiares.isNotEmpty()) {
                Seccion("Familia") {
                    familiares.forEach { TarjetaFamiliar(it) }
                }
            }
            if (medico != null) {
                Seccion("Tu médico") { TarjetaMedico(medico) }
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
private fun TarjetaFamiliar(contacto: Contacto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
            Text(contacto.nombre.take(1).uppercase(), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = TextoChip)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(contacto.nombre, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
            Text(contacto.relacion, fontSize = 16.sp, color = TextoSecundario)
        }
    }
}

@Composable
private fun TarjetaMedico(contacto: Contacto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AzulClaro)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(contacto.nombre, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextoAzulOscuro)
            Text(contacto.relacion, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoAzul)
        }
    }
}

@Preview(name = "Mi familia", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewFamilia() = BuenVivirTheme {
    FamiliaScreen(Repositorio.familiares, Repositorio.medico)
}
