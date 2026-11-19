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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.*
import com.utom.buenvivir.ui.theme.BuenVivirTheme

/**
 * Inicio cuando todavía no tiene medicinas (la app recién instalada).
 * Arriba una tarjeta azul claro con el saludo; abajo un cuadro blanco con borde gris que explica qué hacer.
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
    Column(modifier = Modifier.fillMaxSize().background(Tono.Blanco)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.M)
        ) {
            TarjetaSaludo(nombre, onAjustes)
            CuadroSinMedicinas(acompanantes, onEscuchar, onAgregar, onVerCodigo)
        }
        MenuInferior(seleccionado = 0, onInicio = {}, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
    }
}

/** Azul claro del saludo (un poco más fuerte que el de los encabezados). */
private val AzulSaludo = Tono.Saludo

/** Tarjeta de arriba: "Buenas noches," + nombre, Ajustes y "Hoy no tienes pastillas". */
@Composable
private fun TarjetaSaludo(nombre: String, onAjustes: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Medida.RadioTarjeta))
            .background(AzulSaludo)
    ) {
        // Dos círculos blancos muy suaves en la esquina, de adorno
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 48.dp, y = 64.dp)
                .size(176.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-48).dp, y = 88.dp)
                .size(128.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.M)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Espacio.M),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(saludoDelDia(), style = Letra.Grande, color = Tono.Acento)
                    Text(nombre, style = Letra.Nombre, color = Tono.Noche, modifier = Modifier.semantics { heading() })
                }
                SisBotonIcono(Iconos.Engrane, "Ajustes", Tono.Blanco, Tono.Noche, onAjustes)
            }
            Row(
                modifier = Modifier
                    .height(Espacio.XL + Espacio.S)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Tono.Blanco)
                    .padding(horizontal = Espacio.M),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.S)
            ) {
                Icon(Iconos.Reloj, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(20.dp))
                Text("Hoy no tienes pastillas", style = Letra.EtiquetaFuerte, color = Tono.Acento)
            }
        }
    }
}

/** Cuadro blanco con borde gris: Escuchar en la esquina, ícono, título, texto y botones. */
@Composable
private fun CuadroSinMedicinas(
    acompanantes: List<String>,
    onEscuchar: () -> Unit,
    onAgregar: () -> Unit,
    onVerCodigo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Tono.BordeFuerte, RoundedCornerShape(Medida.RadioTarjeta))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espacio.L),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.M)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Tono.Chip),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Pastilla, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(40.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                Text(
                    buildAnnotatedString {
                        append("Aún no tienes ")
                        withStyle(SpanStyle(color = Tono.Acento)) { append("medicinas") }
                    },
                    style = Letra.Titulo,
                    color = Tono.Noche,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    textoSinMedicinas(acompanantes),
                    style = Letra.Texto,
                    color = Tono.Suave,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.padding(top = Espacio.S),
                verticalArrangement = Arrangement.spacedBy(Espacio.S)
            ) {
                SisBoton("Agregar una medicina", onAgregar, icono = Iconos.Mas)
                // Si ya tiene médico o cuidador, ya no se le pide el código
                if (acompanantes.isEmpty()) SisEnlace("Ver mi código", onVerCodigo)
            }
        }
        // Escuchar flota en la esquina para que lo demás quede más arriba
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(Espacio.M)) {
            SisBotonIcono(Iconos.Bocina, "Escuchar esta pantalla", Tono.Chip, Tono.Acento, onEscuchar)
        }
    }
}

private fun saludoDelDia(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Buenos días,"
    in 12..18 -> "Buenas tardes,"
    else -> "Buenas noches,"
}

/**
 * Sin médico ni cuidador: se le pide dar su código.
 * Si ya tiene: "Alejandro aún no te agrega medicinas." (no tiene sentido pedirle el código otra vez).
 */
fun textoSinMedicinas(acompanantes: List<String>): String = when (acompanantes.size) {
    0 -> "Pídele a tu médico que te las agregue."
    1 -> "${acompanantes[0]} aún no te agrega medicinas."
    else -> "${acompanantes.dropLast(1).joinToString(", ")} y ${acompanantes.last()} aún no te agregan medicinas."
}

@Preview(name = "Sin medicinas", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewSinMedicinas() = BuenVivirTheme { SinMedicinasScreen("Doña María") }
