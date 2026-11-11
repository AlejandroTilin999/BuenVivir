package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.*
import com.utom.buenvivir.ui.theme.BuenVivirTheme

/**
 * Inicio cuando todavía no tiene medicinas (la app recién instalada). Estilo limpio.
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
    ConPoppins {
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // Encabezado: "Hola," y abajo el nombre, con Ajustes a la derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(FondoLavandaSuave)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hola,", fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = AcentoMorado)
                    Text(
                        nombre,
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = AzulNoche,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                BotonRedondo(Iconos.Engrane, "Ajustes", Color.White, AzulNoche, onAjustes)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 24.dp)
            ) {
                // Tarjeta con borde gris: Escuchar, título, texto y los dos botones
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFC5D0E3), RoundedCornerShape(24.dp))   // gris un poco más marcado
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        BotonRedondo(Iconos.Bocina, "Escuchar esta pantalla", ChipLavanda, AcentoMorado, onEscuchar)
                    }
                    Text(
                        androidx.compose.ui.text.buildAnnotatedString {
                            append("Aún no tienes\n")   // "medicinas" abajo, como en el canvas
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = AcentoMorado))
                            append("medicinas")
                            pop()
                        },
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = AzulNoche,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        textoSinMedicinas(acompanantes),
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = TextoSuave,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),   // botones un poco menos largos
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Si ya tiene médico o cuidador, ya no se le pide el código
                        if (acompanantes.isEmpty()) BotonOscuro("Ver mi código", alto = 64.dp, letra = 20.sp, onClick = onVerCodigo)
                        BotonBordeMas("Agregar una medicina", onAgregar)
                    }
                }
            }

            MenuInferior(seleccionado = 0, onInicio = {}, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
        }
    }
}

/** Botón redondo de 48 con solo un ícono (Ajustes, Escuchar). */
@Composable
private fun BotonRedondo(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String,
                         fondo: Color, tinte: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fondo)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = tinte, modifier = Modifier.size(24.dp))
    }
}

/** Botón blanco con borde fino y un "+" azul: "Agregar una medicina". */
@Composable
private fun BotonBordeMas(texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, BordeCampo, RoundedCornerShape(32.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Iconos.Mas, contentDescription = null, tint = AcentoMorado, modifier = Modifier.size(24.dp))
        Text(texto, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = AzulNoche)
    }
}

/**
 * Sin médico ni cuidador: se le pide dar su código.
 * Si ya tiene: "Alejandro aún no te agrega medicinas." (no tiene sentido pedirle el código otra vez).
 */
fun textoSinMedicinas(acompanantes: List<String>): String = when (acompanantes.size) {
    0 -> "Dale tu código a tu médico para\nque te las agregue."
    1 -> "${acompanantes[0]} aún no te agrega medicinas."
    else -> "${acompanantes.dropLast(1).joinToString(", ")} y ${acompanantes.last()} aún no te agregan medicinas."
}

@Preview(name = "Sin medicinas", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewSinMedicinas() = BuenVivirTheme { SinMedicinasScreen("Doña María") }
