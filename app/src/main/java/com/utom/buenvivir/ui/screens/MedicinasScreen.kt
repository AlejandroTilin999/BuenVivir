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
import com.utom.buenvivir.data.EstadoDia
import com.utom.buenvivir.data.Medicina
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.BotonEscuchar
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
    onEscuchar: () -> Unit = {},
    onAbrirMedicina: (Medicina) -> Unit = {},
    onIrInicio: () -> Unit = {},
    onIrFamilia: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        Encabezado(semana, tomadasSemana, totalSemana, onEscuchar)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Seccion("Hoy") { TarjetaHoy(tomasHoy) }
            Seccion("Tus medicinas") {
                medicinas.forEach { TarjetaMedicina(it) { onAbrirMedicina(it) } }
            }
        }

        MenuInferior(seleccionado = 1, onInicio = onIrInicio, onMedicinas = {}, onFamilia = onIrFamilia)
    }
}

// ---------- Encabezado con la semana ----------
// Mide lo mismo que el de Inicio: arriba 16, título + línea de 19 sp, espacio 18,
// tarjeta de 80 y abajo 24. Así no "brinca" al cambiar de pestaña.

@Composable
private fun Encabezado(semana: List<DiaSemana>, tomadas: Int, total: Int, onEscuchar: () -> Unit) {
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
                    "Mis medicinas",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    buildAnnotatedString {
                        append("Esta semana: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$tomadas de $total") }
                        append(" tomadas")
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoLavanda
                )
            }
            BotonEscuchar("Escuchar esta pantalla", Blanco, onEscuchar)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Blanco)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            dia.letra,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = if (esHoy) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (esHoy) AzulBoton else TextoSecundario
        )
        val circulo = Modifier
            .size(34.dp)
            .clip(CircleShape)
        when (dia.estado) {
            EstadoDia.Completo -> Box(circulo.background(AzulBoton), contentAlignment = Alignment.Center) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Blanco, modifier = Modifier.size(18.dp))
            }
            EstadoDia.Falto -> Box(circulo.background(Lavanda), contentAlignment = Alignment.Center) {
                Icon(Iconos.Menos, contentDescription = null, tint = TextoChip, modifier = Modifier.size(18.dp))
            }
            EstadoDia.Hoy -> Box(circulo.background(Blanco).border(3.dp, AzulBoton, CircleShape))
            EstadoDia.Futuro -> Box(circulo.background(Blanco).border(2.dp, BordeTarjeta, CircleShape))
        }
    }
}

// ---------- Secciones ----------

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
private fun TarjetaHoy(tomas: List<TomaDelDia>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
    ) {
        tomas.forEachIndexed { i, toma ->
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(LavandaSuave)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(toma.hora, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextoNormal)
                    Text(toma.medicina, fontSize = 16.sp, color = TextoSecundario)
                    toma.horaReal?.let {
                        Text("Tomada a las $it", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextoMoradoMedio)
                    }
                }
                ChipEstado(toma.estado)
            }
        }
    }
}

@Composable
private fun ChipEstado(estado: EstadoToma) {
    val forma = RoundedCornerShape(12.dp)
    val (texto, icono, fondo, color) = when (estado) {
        EstadoToma.Tomada -> Estilo("Tomada", Iconos.Palomita, Violeta, Blanco)
        EstadoToma.Pendiente -> Estilo("Pendiente", Iconos.Reloj, LavandaChip, TextoChip)
        is EstadoToma.Omitida -> Estilo("No la tomó", Iconos.Menos, GrisAviso, Blanco)
    }
    Row(
        modifier = Modifier
            .clip(forma)
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private data class Estilo(val texto: String, val icono: ImageVector, val fondo: Color, val color: Color)

@Composable
private fun TarjetaMedicina(medicina: Medicina, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AzulClaro)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Blanco),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Pastilla, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(30.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(medicina.nombre, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TextoAzulOscuro)
            Text(medicina.frecuencia, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoAzul)
        }
        Icon(Iconos.Flecha, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(26.dp))
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
