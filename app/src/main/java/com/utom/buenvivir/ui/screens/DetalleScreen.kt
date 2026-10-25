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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.CambioDosis
import com.utom.buenvivir.data.Medicina
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.data.TomaPasada
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.theme.*

/** Pantalla 5: cómo tomar una medicina, su video y las últimas tomas. */
@Composable
fun DetalleScreen(
    medicina: Medicina,
    historial: List<TomaPasada>,
    onRegresar: () -> Unit = {},
    onEscuchar: () -> Unit = {},
    onVerVideo: () -> Unit = {},
    onIrInicio: () -> Unit = {},
    onIrFamilia: () -> Unit = {},
    quienLaPuso: String? = null,              // "Te la puso tu médico: Dra. Ana", "La agregaste tú"
    cambios: List<CambioDosis> = emptyList(), // cambios de dosis, para que sepa qué cambió y quién
    onCambiar: (() -> Unit)? = null,          // solo si la agregó él mismo
    onQuitar: (() -> Unit)? = null
) {
    var confirmarQuitar by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Regresar(onRegresar)
            Encabezado(medicina)
            if (!quienLaPuso.isNullOrBlank()) {
                Text(quienLaPuso, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
            }
            ComoTomarla(medicina, onEscuchar)
            Video(medicina.video, onVerVideo)
            UltimasTomas(historial)
            if (cambios.isNotEmpty()) CambiosDeDosis(cambios)
            if (onCambiar != null || onQuitar != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    onCambiar?.let { BotonBorde("Cambiar", AzulBoton, Modifier.weight(1f), it) }
                    onQuitar?.let { BotonBorde("Quitar", GrisAviso, Modifier.weight(1f)) { confirmarQuitar = true } }
                }
            }
        }

        MenuInferior(seleccionado = 1, onInicio = onIrInicio, onMedicinas = onRegresar, onFamilia = onIrFamilia)
    }

    if (confirmarQuitar && onQuitar != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar ${medicina.nombre}?", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Dejará de sonar su alarma.", fontSize = 18.sp) },
            confirmButton = {
                TextButton(onClick = { confirmarQuitar = false; onQuitar() }) {
                    Text("Sí, quitar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GrisAviso)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarQuitar = false }) {
                    Text("No", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun CambiosDeDosis(cambios: List<CambioDosis>) {
    val formato = java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM", java.util.Locale.forLanguageTag("es-MX"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Titulo("Cambios en tu dosis")
        cambios.sortedByDescending { it.fecha }.take(5).forEach { c ->
            val fecha = java.time.Instant.ofEpochMilli(c.fecha).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formato)
            val quien = when (c.rol) {
                "medico" -> "Tu médico"
                "familiar" -> "Tu cuidador"
                "paciente" -> "Tú"
                else -> "Alguien"
            } + (if (c.quien.isNotBlank() && c.rol != "paciente") " ${c.quien}" else "")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Lavanda, RoundedCornerShape(18.dp))
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("$fecha · $quien", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoLavanda)
                Text("Antes: ${c.antes}", fontSize = 17.sp, lineHeight = 23.sp, color = TextoSecundario)
                Text("Ahora: ${c.despues}", fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, color = TextoNormal)
            }
        }
    }
}

@Composable
private fun BotonBorde(texto: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .border(2.dp, color, RoundedCornerShape(29.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun Regresar(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Iconos.Atras, contentDescription = null, tint = TextoChip, modifier = Modifier.size(24.dp))
        Text("Mis medicinas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoChip)
    }
}

@Composable
private fun Encabezado(medicina: Medicina) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Lavanda),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Pastilla, contentDescription = null, tint = TextoChip, modifier = Modifier.size(36.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                medicina.nombre,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text(medicina.para, fontSize = 17.sp, color = TextoSecundario)
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextoNormal,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun ComoTomarla(medicina: Medicina, onEscuchar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Lavanda, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Titulo("Cómo tomarla")
            BotonEscuchar("Escuchar cómo tomarla", LavandaChip, onEscuchar)
        }
        Text(
            buildAnnotatedString {
                append("Toma ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(medicina.cantidad) }
                append(" ${medicina.indicacion}.")
            },
            fontSize = 19.sp,
            lineHeight = 27.sp,
            color = TextoNormal
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            medicina.horarios.forEach { hora ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Lavanda),
                    contentAlignment = Alignment.Center
                ) {
                    Text(hora, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoChip)
                }
            }
        }
    }
}

@Composable
private fun Video(titulo: String, onVerVideo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Titulo("Aprende sobre tu pastilla")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(TextoChip)
                .clickable(role = Role.Button, onClick = onVerVideo)
                .semantics { contentDescription = "Reproducir video: $titulo" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Blanco),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Play, contentDescription = null, tint = TextoChip, modifier = Modifier.size(28.dp))
            }
        }
        Text(titulo, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp, color = TextoNormal)
    }
}

@Composable
private fun UltimasTomas(historial: List<TomaPasada>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Titulo("Tus últimas tomas")
        historial.forEach { FilaToma(it) }
    }
}

@Composable
private fun FilaToma(toma: TomaPasada) {
    val falto = toma.motivo != null
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(if (falto) GrisFondo else Blanco)
            .border(2.dp, if (falto) GrisBorde else Lavanda, forma)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Día y hora en un solo renglón: "Hoy · 8:00 p. m."
            Text("${toma.dia} · ${toma.hora}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoNormal)
            if (falto) {
                Text(toma.motivo ?: "", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoGrisMedio)
            } else {
                toma.horaReal?.let {
                    Text("A las $it", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoMoradoMedio)
                }
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (falto) GrisAviso else Violeta)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!falto) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Blanco, modifier = Modifier.size(15.dp))
            }
            Text(
                if (falto) "No la tomó" else "Tomada",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Blanco,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Preview(name = "Detalle", widthDp = 360, heightDp = 1370, showBackground = true)
@Composable
private fun PreviewDetalle() = BuenVivirTheme {
    DetalleScreen(Repositorio.medicinas.first(), Repositorio.historial)
}
