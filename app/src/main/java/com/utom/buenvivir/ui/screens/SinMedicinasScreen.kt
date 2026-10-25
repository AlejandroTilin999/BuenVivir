package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.theme.*

/**
 * Inicio cuando todavía no tiene medicinas (la app recién instalada).
 * Sus medicinas se las agrega su médico o su familia: aquí se le explica cómo.
 */
@Composable
fun SinMedicinasScreen(
    nombre: String,
    acompanantes: List<String> = emptyList(),   // nombres de su médico o cuidador, si ya tiene
    onEscuchar: () -> Unit = {},
    onVerCodigo: () -> Unit = {},
    onAgregar: () -> Unit = {},
    onAjustes: () -> Unit = {},
    onIrMedicinas: () -> Unit = {},
    onIrFamilia: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().background(Blanco)) {
        // Encabezado igual al de Inicio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Lavanda)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Hola,", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
                Text(
                    nombre,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Blanco)
                    .clickable(role = Role.Button, onClick = onAjustes),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Engrane, contentDescription = "Ajustes", tint = AzulBoton, modifier = Modifier.size(26.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, BordeTarjeta, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BotonEscuchar("Escuchar esta pantalla", LavandaChip, onEscuchar)
                }
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Lavanda),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Iconos.Pastilla, contentDescription = null, tint = TextoChip, modifier = Modifier.size(36.dp))
                }
                Text(
                    "Todavía no tienes medicinas",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoNormal,
                    textAlign = TextAlign.Center
                )
                Text(
                    textoSinMedicinas(acompanantes),
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )
                if (acompanantes.isEmpty()) Button(
                    onClick = onVerCodigo,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulBoton, contentColor = Blanco)
                ) {
                    Text("Ver mi código", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(2.dp, AzulBoton, RoundedCornerShape(32.dp))
                        .clickable(role = Role.Button, onClick = onAgregar),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Agregar una medicina", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AzulBoton)
                }
            }
        }

        MenuInferior(seleccionado = 0, onInicio = {}, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
    }
}

/**
 * Sin médico ni cuidador: se le pide dar su código.
 * Si ya tiene: "Alejandro todavía no te agrega medicinas…" (no tiene sentido pedirle el código otra vez).
 */
fun textoSinMedicinas(acompanantes: List<String>): String = when (acompanantes.size) {
    0 -> "Dale tu código a tu médico para que te agregue tus medicinas."
    1 -> "${acompanantes[0]} aún no te agrega medicinas."
    else -> "${acompanantes.dropLast(1).joinToString(", ")} y ${acompanantes.last()} aún no te agregan medicinas."
}

@Preview(name = "Sin medicinas", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewSinMedicinas() = BuenVivirTheme { SinMedicinasScreen("Don José") }
