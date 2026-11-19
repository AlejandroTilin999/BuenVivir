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
import com.utom.buenvivir.ui.components.*
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
            .background(Tono.Blanco)
    ) {
        SisEncabezado(medicina.nombre, onEscuchar = onEscuchar, regresar = "Mis medicinas" to onRegresar) {
            Text(medicina.para, style = Letra.Texto, color = Tono.Suave)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.XL)
        ) {
            SisSeccion("Cómo tomarla") { ComoTomarla(medicina, quienLaPuso) }
            SisSeccion("Aprende sobre tu pastilla") { Video(medicina.video, onVerVideo) }
            if (historial.isNotEmpty()) SisSeccion("Tus últimas tomas") { UltimasTomas(historial) }
            if (cambios.isNotEmpty()) SisSeccion("Cambios en tu dosis") { CambiosDeDosis(cambios) }
            if (onCambiar != null || onQuitar != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.S)) {
                    onCambiar?.let { SisBoton("Cambiar", it, Modifier.weight(1f), contorno = true) }
                    onQuitar?.let { SisBoton("Quitar", { confirmarQuitar = true }, Modifier.weight(1f), contorno = true) }
                }
            }
        }

        MenuInferior(seleccionado = 1, onInicio = onIrInicio, onMedicinas = onRegresar, onFamilia = onIrFamilia)
    }

    if (confirmarQuitar && onQuitar != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar ${medicina.nombre}?", style = Letra.Subtitulo) },
            text = { Text("Dejará de sonar su alarma.", style = Letra.Texto) },
            confirmButton = {
                TextButton(onClick = { confirmarQuitar = false; onQuitar() }) {
                    Text("Sí, quitar", style = Letra.TextoFuerte, color = Tono.Acento)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarQuitar = false }) {
                    Text("No", style = Letra.TextoFuerte, color = Tono.Suave)
                }
            },
            containerColor = Tono.Blanco
        )
    }
}

@Composable
private fun ComoTomarla(medicina: Medicina, quienLaPuso: String?) {
    SisCaja {
        Text(
            buildAnnotatedString {
                append("Toma ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(medicina.cantidad) }
                append(" ${medicina.indicacion}.")
            },
            style = Letra.Grande,
            color = Tono.Noche
        )
        // Horas en chips de 40
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.S)) {
            medicina.horarios.forEach { hora ->
                Row(
                    modifier = Modifier
                        .height(Medida.Chip)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Tono.Chip)
                        .padding(horizontal = Espacio.M),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.S)
                ) {
                    Icon(Iconos.Reloj, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(20.dp))
                    Text(hora, style = Letra.EtiquetaFuerte, color = Tono.Acento)
                }
            }
        }
        if (!quienLaPuso.isNullOrBlank()) {
            Text(quienLaPuso, style = Letra.Etiqueta, color = Tono.Suave)
        }
    }
}

@Composable
private fun Video(titulo: String, onVerVideo: () -> Unit) {
    val forma = RoundedCornerShape(Medida.RadioCaja)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .border(1.dp, Tono.Borde, forma)
            .clickable(role = Role.Button, onClick = onVerVideo)
            .semantics(mergeDescendants = true) { contentDescription = "Reproducir video: $titulo" }
            .padding(Espacio.M),
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Medida.Tactil)
                .clip(CircleShape)
                .background(Tono.Noche),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Play, contentDescription = null, tint = Tono.Blanco, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = Letra.TextoFuerte, color = Tono.Noche)
            Text("Video corto", style = Letra.Etiqueta, color = Tono.Suave)
        }
    }
}

@Composable
private fun UltimasTomas(historial: List<TomaPasada>) {
    SisCaja(espacio = Espacio.S) {
        historial.forEachIndexed { i, toma ->
            if (i > 0) SisSeparador()
            val falto = toma.motivo != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Medida.Tactil)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(Espacio.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Día y hora en un solo renglón: "Hoy · 8:00 p. m."
                    Text("${toma.dia} · ${toma.hora}", style = Letra.TextoFuerte, color = Tono.Noche)
                    val detalle = if (falto) toma.motivo else toma.horaReal?.let { "Tomada a las $it" }
                    detalle?.let { Text(it, style = Letra.Etiqueta, color = Tono.Suave) }
                }
                SisEstado(if (falto) "No la tomó" else "Tomada", !falto)
            }
        }
    }
}

@Composable
private fun CambiosDeDosis(cambios: List<CambioDosis>) {
    val formato = java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM", java.util.Locale.forLanguageTag("es-MX"))
    SisCaja(espacio = Espacio.S) {
        cambios.sortedByDescending { it.fecha }.take(5).forEachIndexed { i, c ->
            if (i > 0) SisSeparador()
            val fecha = java.time.Instant.ofEpochMilli(c.fecha).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formato)
            val quien = when (c.rol) {
                "medico" -> "Tu médico"
                "familiar" -> "Tu cuidador"
                "paciente" -> "Tú"
                else -> "Alguien"
            } + (if (c.quien.isNotBlank() && c.rol != "paciente") " ${c.quien}" else "")
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text("$fecha · $quien", style = Letra.EtiquetaFuerte, color = Tono.Acento)
                Text("Antes: ${c.antes}", style = Letra.Texto, color = Tono.Suave)
                Text("Ahora: ${c.despues}", style = Letra.TextoFuerte, color = Tono.Noche)
            }
        }
    }
}

@Preview(name = "Detalle", widthDp = 360, heightDp = 1370, showBackground = true)
@Composable
private fun PreviewDetalle() = BuenVivirTheme {
    DetalleScreen(Repositorio.medicinas.first(), Repositorio.historial)
}
