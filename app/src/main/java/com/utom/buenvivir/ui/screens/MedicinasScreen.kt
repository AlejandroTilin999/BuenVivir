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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.utom.buenvivir.data.DiaSemana
import com.utom.buenvivir.ui.components.GraficaMotivos
import com.utom.buenvivir.data.EstadoDia
import com.utom.buenvivir.data.Medicina
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.*
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.theme.*

/** Una toma de hoy, con el estado que tenga en este momento. */
data class TomaDelDia(
    val hora: String,
    val medicina: String,
    val estado: EstadoToma,
    val horaReal: String? = null    // a qué hora se la tomó de verdad
)

@Composable
fun MedicinasScreen(
    semana: List<DiaSemana>,
    tomadasSemana: Int,
    totalSemana: Int,
    tomasHoy: List<TomaDelDia>,
    medicinas: List<Medicina>,
    motivos: List<Pair<String, Int>> = emptyList(),   // por qué no se la tomó, últimos 30 días
    onEscuchar: () -> Unit = {},
    onAbrirMedicina: (Medicina) -> Unit = {},
    onAgregar: () -> Unit = {},
    onIrInicio: () -> Unit = {},
    onIrFamilia: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tono.Blanco)
    ) {
        SisEncabezado("Mis medicinas", onEscuchar = onEscuchar) {
            Semana(semana, tomadasSemana, totalSemana)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.XL)
        ) {
            if (tomasHoy.isNotEmpty()) SisSeccion("Hoy") { TarjetaHoy(tomasHoy) }
            SisSeccion("Tus medicinas") {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                    if (medicinas.isEmpty()) {
                        Text("Todavía no tienes medicinas.", style = Letra.Texto, color = Tono.Suave)
                    }
                    medicinas.forEach { TarjetaMedicina(it) { onAbrirMedicina(it) } }
                    SisBoton("Agregar una medicina", onAgregar, icono = Iconos.Mas, contorno = true)
                }
            }
            // Solo si ha faltado alguna: así ve qué le está costando más trabajo
            if (motivos.isNotEmpty()) {
                GraficaMotivos(motivos, "¿Por qué no me la tomé?", "Últimos 30 días")
            }
        }

        MenuInferior(seleccionado = 1, onInicio = onIrInicio, onMedicinas = {}, onFamilia = onIrFamilia)
    }
}

// ---------- La semana (dentro del encabezado) ----------

@Composable
private fun Semana(semana: List<DiaSemana>, tomadas: Int, total: Int) {
    Column(
        modifier = Modifier
            .padding(top = Espacio.S)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Medida.RadioCaja))
            .background(Tono.Blanco)
            .padding(Espacio.M),
        verticalArrangement = Arrangement.spacedBy(Espacio.M)
    ) {
        Text(
            buildAnnotatedString {
                append("Esta semana: ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$tomadas de $total") }
                append(" tomadas")
            },
            style = Letra.TextoFuerte,
            color = Tono.Noche
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            semana.forEach { DiaCalendario(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DiaCalendario(dia: DiaSemana, modifier: Modifier) {
    val esHoy = dia.estado == EstadoDia.Hoy
    val descripcion = dia.nombre + ": " + when (dia.estado) {
        EstadoDia.Completo -> "tomó todas"
        EstadoDia.Falto -> "le faltó una"
        EstadoDia.Hoy -> "en curso"
        EstadoDia.Futuro -> "todavía no llega"
    }
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.S)
    ) {
        Text(dia.letra, style = Letra.EtiquetaFuerte, color = if (esHoy) Tono.Acento else Tono.Noche)
        val circulo = Modifier
            .size(32.dp)
            .clip(CircleShape)
        when (dia.estado) {
            EstadoDia.Completo -> Box(circulo.background(Tono.Acento), contentAlignment = Alignment.Center) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Tono.Blanco, modifier = Modifier.size(16.dp))
            }
            EstadoDia.Falto -> Box(circulo.background(Tono.Suave), contentAlignment = Alignment.Center) {
                Icon(Iconos.Menos, contentDescription = null, tint = Tono.Blanco, modifier = Modifier.size(16.dp))
            }
            EstadoDia.Hoy -> Box(circulo.background(Tono.Blanco).border(2.dp, Tono.Acento, CircleShape))
            EstadoDia.Futuro -> Box(circulo.background(Tono.Blanco).border(1.dp, Tono.Borde, CircleShape))
        }
    }
}

// ---------- Hoy ----------

@Composable
private fun TarjetaHoy(tomas: List<TomaDelDia>) {
    SisCaja(espacio = Espacio.S) {
        tomas.forEachIndexed { i, toma ->
            if (i > 0) SisSeparador()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Medida.Tactil)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(Espacio.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(toma.medicina, style = Letra.TextoFuerte, color = Tono.Noche)
                    Text(
                        toma.horaReal?.let { "${toma.hora} · tomada a las $it" } ?: toma.hora,
                        style = Letra.Etiqueta,
                        color = Tono.Suave
                    )
                }
                when (toma.estado) {
                    EstadoToma.Tomada -> SisEstado("Tomada", true)
                    EstadoToma.Pendiente -> SisEstado("Pendiente", null)
                    is EstadoToma.Omitida -> SisEstado("No la tomó", false)
                }
            }
        }
    }
}

// ---------- Tus medicinas ----------

@Composable
private fun TarjetaMedicina(medicina: Medicina, onClick: () -> Unit) {
    val forma = RoundedCornerShape(Medida.RadioCaja)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Medida.Fila)
            .clip(forma)
            .border(1.dp, Tono.Borde, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Espacio.M),
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SisIconoLista(Iconos.Pastilla)
        Column(modifier = Modifier.weight(1f)) {
            Text(medicina.nombre, style = Letra.TextoFuerte, color = Tono.Noche)
            Text(medicina.frecuencia, style = Letra.Etiqueta, color = Tono.Suave)
        }
        Icon(Iconos.Flecha, contentDescription = null, tint = Tono.Suave, modifier = Modifier.size(20.dp))
    }
}

// ---------- Vista previa ----------

@Preview(name = "Mis medicinas", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewMedicinas() = BuenVivirTheme {
    MedicinasScreen(
        semana = Repositorio.semana,
        tomadasSemana = 11,
        totalSemana = 14,
        tomasHoy = listOf(
            TomaDelDia("8:00 a. m.", "Metformina", EstadoToma.Tomada),
            TomaDelDia("8:00 p. m.", "Metformina", EstadoToma.Pendiente)
        ),
        medicinas = Repositorio.medicinas
    )
}
