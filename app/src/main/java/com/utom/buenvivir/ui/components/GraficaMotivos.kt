package com.utom.buenvivir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * Qué tipo de barrera hay detrás de cada motivo. Así el médico y el cuidador ven si el problema
 * es de memoria, de dinero, de acceso, de miedo o de comprensión, y no solo cuántas faltó.
 */
fun barreraDe(motivo: String): String = when (motivo.trim().lowercase()) {
    "se me olvidó" -> "Olvido"
    "no estaba en casa" -> "Rutina fuera de casa"
    "no tenía la medicina" -> "Acceso a la medicina"
    "no tenía dinero para comprarla" -> "Económica"
    "me da miedo" -> "Miedo o efectos"
    "no entendí cómo tomarla" -> "Comprensión"
    "no hubo respuesta" -> "Sin respuesta"
    else -> "Otra"
}

/** Cuenta los motivos de las tomas que no se tomó, del más común al menos común. */
fun contarMotivos(motivos: List<String?>): List<Pair<String, Int>> =
    motivos.map { it ?: "Otro motivo" }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

private val ColorBarra = Color(0xFF004EE0)
private val ColorBarraSuave = Color(0xFF8DB0F5)
private val FondoBarra = Color(0xFFEFF4FF)
private val Borde = Color(0xFFE1E7F2)
private val TextoFuerte = Color(0xFF0B1F4D)
private val TextoGrisMotivos = Color(0xFF5B6170)

/**
 * Gráfica de barras de los motivos por los que no se tomó la pastilla.
 * La barra más larga es el motivo más común; debajo de cada motivo dice su tipo de barrera.
 *
 * @param titulo "¿Por qué no se la tomó?" (médico y cuidador) o "¿Por qué no me la tomé?" (paciente)
 * @param periodo "Últimos 30 días"
 */
@Composable
fun GraficaMotivos(
    motivos: List<Pair<String, Int>>,
    titulo: String,
    periodo: String,
    modifier: Modifier = Modifier
) {
    val total = motivos.sumOf { it.second }
    val maximo = motivos.maxOfOrNull { it.second } ?: 0

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo,
            style = Letra.Subtitulo,
            color = TextoFuerte,
            modifier = Modifier.semantics { heading() }
        )
        Text(periodo, style = Letra.Etiqueta, color = TextoGrisMotivos)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Borde, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (total == 0) {
                Text(
                    "No hay pastillas sin tomar en este periodo.",
                    style = Letra.Texto, color = TextoGrisMotivos
                )
                return@Column
            }

            // Lo más importante primero: el motivo más común y su barrera
            val (principal, veces) = motivos.first()
            Text(
                buildAnnotatedString {
                    append("Lo más común: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextoFuerte)) { append(principal) }
                    append(" ($veces de $total). Barrera: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ColorBarra)) { append(barreraDe(principal)) }
                    append(".")
                },
                style = Letra.Texto, color = TextoGrisMotivos
            )

            motivos.forEachIndexed { i, (motivo, n) ->
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "$motivo: ${vecesTexto(n)} de $total. Barrera: ${barreraDe(motivo)}"
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            motivo,
                            style = Letra.TextoFuerte, color = TextoFuerte,
                            modifier = Modifier.weight(1f)
                        )
                        Text(vecesTexto(n), style = Letra.TextoFuerte.copy(fontWeight = FontWeight.Bold), color = TextoFuerte, softWrap = false)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FondoBarra)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (maximo == 0) 0f else n.toFloat() / maximo)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (i == 0) ColorBarra else ColorBarraSuave)
                        )
                    }
                    Text("Barrera: ${barreraDe(motivo)}", style = Letra.Etiqueta, color = TextoGrisMotivos)
                }
            }
        }
    }
}

private fun vecesTexto(n: Int) = if (n == 1) "1 vez" else "$n veces"

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun PreviewGraficaMotivos() {
    GraficaMotivos(
        motivos = listOf("No tenía dinero para comprarla" to 4, "Se me olvidó" to 2, "No estaba en casa" to 1),
        titulo = "¿Por qué no se la tomó?",
        periodo = "Últimos 30 días",
        modifier = Modifier.padding(16.dp)
    )
}
