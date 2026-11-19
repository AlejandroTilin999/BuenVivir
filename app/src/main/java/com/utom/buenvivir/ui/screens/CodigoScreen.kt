package com.utom.buenvivir.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.*
import com.utom.buenvivir.ui.theme.BuenVivirTheme

/** Qué se ve en "Mi código": todavía cargando, listo, o no se pudo (sin internet). */
sealed interface EstadoCodigo {
    data object Cargando : EstadoCodigo
    data class Listo(val codigo: String) : EstadoCodigo
    data object Error : EstadoCodigo
}

/** "K7M2Q9" -> "K7M 2Q9", más fácil de leer y de dictar. */
fun codigoEnGrupos(codigo: String) = codigo.chunked(3).joinToString(" ")

/** Íconos de copiar y enviar (mismo trazo que los demás). */
private fun trazo(vararg caminos: String): ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).apply {
    caminos.forEach {
        addPath(addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }
}.build()

private val IconoCopiar = trazo(
    "M11 9h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2z",
    "M5 15H4a2 2 0 0 1 -2 -2V4a2 2 0 0 1 2 -2h9a2 2 0 0 1 2 2v1"
)
private val IconoEnviar = trazo(
    "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
    "M15 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M8.6 13.5l6.8 4", "M15.4 6.5l-6.8 4"
)

/** Pantalla 8: el código del paciente para que lo vinculen su médico o su familia. */
@Composable
fun CodigoScreen(
    estado: EstadoCodigo,
    onRegresar: () -> Unit = {},
    onEscuchar: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onIrInicio: () -> Unit = {},
    onIrMedicinas: () -> Unit = {}
) {
    val context = LocalContext.current
    val portapapeles = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().background(Tono.Blanco)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = Espacio.M, end = Espacio.M, top = Espacio.S, bottom = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.M)
        ) {
            SisRegresar("Mi familia", onRegresar)

            SisTarjetaAzul {
                Text(
                    buildAnnotatedString {
                        append("Tu ")
                        withStyle(SpanStyle(color = Tono.Acento)) { append("código") }
                    },
                    style = Letra.Titulo,
                    color = Tono.Noche,
                    modifier = Modifier.semantics { heading() }
                )
                Text("Dáselo a tu médico o a un familiar.", style = Letra.Texto, color = Tono.Noche)
            }

            // Cuadro blanco con borde gris; Escuchar flota en la esquina
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Tono.BordeFuerte, RoundedCornerShape(Medida.RadioTarjeta))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacio.M, vertical = Espacio.L),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Espacio.M)
                ) {
                    Text(
                        "Tu código es",
                        style = Letra.Subtitulo,
                        color = Tono.Noche,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Medida.Tactil)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                    when (estado) {
                        EstadoCodigo.Cargando -> {
                            CircularProgressIndicator(color = Tono.Acento, modifier = Modifier.size(40.dp))
                            Text("Creando tu código…", style = Letra.Texto, color = Tono.Suave)
                        }

                        is EstadoCodigo.Listo -> {
                            CodigoEnCuadros(estado.codigo)
                            Text(
                                "Así sabrán si tomas tus pastillas.",
                                style = Letra.Texto,
                                color = Tono.Suave,
                                textAlign = TextAlign.Center
                            )
                            Column(
                                modifier = Modifier.padding(top = Espacio.S),
                                verticalArrangement = Arrangement.spacedBy(Espacio.S)
                            ) {
                                SisBoton("Enviar mi código", {
                                    val texto = "Mi código de BuenVivir es ${codigoEnGrupos(estado.codigo)}. " +
                                        "Escríbelo en tu app para ver si tomo mis pastillas."
                                    val envio = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, texto)
                                    }
                                    context.startActivity(Intent.createChooser(envio, "Enviar mi código"))
                                }, icono = IconoEnviar)
                                SisBoton("Copiar", {
                                    portapapeles.setText(AnnotatedString(estado.codigo))
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                }, icono = IconoCopiar, contorno = true)
                            }
                        }

                        EstadoCodigo.Error -> {
                            Text(
                                "No se pudo mostrar tu código. Revisa que tengas internet.",
                                style = Letra.TextoFuerte,
                                color = RojoError,
                                textAlign = TextAlign.Center
                            )
                            SisBoton("Intentar otra vez", onReintentar)
                        }
                    }
                }
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(Espacio.M)) {
                    SisBotonIcono(Iconos.Bocina, "Escuchar el código", Tono.Chip, Tono.Acento, onEscuchar)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Espacio.S),
                horizontalArrangement = Arrangement.spacedBy(Espacio.S, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Iconos.Candado, contentDescription = null, tint = Tono.Suave, modifier = Modifier.size(20.dp))
                Text("Solo dáselo a personas de confianza.", style = Letra.Etiqueta, color = Tono.Suave)
            }
        }

        MenuInferior(seleccionado = 2, onInicio = onIrInicio, onMedicinas = onIrMedicinas, onFamilia = onRegresar)
    }
}

/** El código en cuadritos blancos con borde gris oscuro: 3 juntos, un espacio más grande, y 3 juntos. */
@Composable
private fun CodigoEnCuadros(codigo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // El lector de pantalla lo deletrea: "K, 7, M, 2, Q, 9"
            .semantics(mergeDescendants = true) { contentDescription = "Tu código: " + codigo.toList().joinToString(", ") },
        horizontalArrangement = Arrangement.spacedBy(Espacio.M, Alignment.CenterHorizontally)
    ) {
        codigo.chunked(3).forEach { grupo ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                grupo.forEach { letra ->
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(64.dp)
                            .border(2.dp, Tono.Suave, RoundedCornerShape(12.dp))
                            .background(Tono.Blanco, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letra.toString(), style = Letra.Titulo.copy(fontSize = 36.sp, lineHeight = 44.sp), color = Tono.Noche)
                    }
                }
            }
        }
    }
}

@Preview(name = "Mi código", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewCodigo() = BuenVivirTheme {
    CodigoScreen(EstadoCodigo.Listo("K7M2Q9"))
}

@Preview(name = "Mi código · sin internet", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewCodigoError() = BuenVivirTheme {
    CodigoScreen(EstadoCodigo.Error)
}
