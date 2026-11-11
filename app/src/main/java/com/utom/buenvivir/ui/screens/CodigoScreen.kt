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

/** Pantalla 8: el código del paciente para que lo vinculen su médico o su familia. Estilo limpio. */
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

    ConPoppins {
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Bloque azul claro de arriba
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(FondoLavandaSuave)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(role = Role.Button, onClick = onRegresar)
                            .semantics { contentDescription = "Regresar" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Iconos.Atras, contentDescription = null, tint = AzulNoche, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        buildAnnotatedString {
                            append("Tu ")
                            withStyle(SpanStyle(color = AcentoMorado)) { append("código") }
                        },
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = AzulNoche,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .semantics { heading() }
                    )
                    Text(
                        "Dáselo a tu médico o a un familiar.",
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = TextoSuave,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp)) {
                    when (estado) {
                        EstadoCodigo.Cargando -> Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = AcentoMorado, modifier = Modifier.size(40.dp))
                            Text("Creando tu código…", fontSize = 18.sp, lineHeight = 28.sp, color = TextoSuave)
                        }

                        is EstadoCodigo.Listo -> {
                            // El código solo, grande y centrado, con una línea fina abajo
                            Text(
                                codigoEnGrupos(estado.codigo),
                                fontSize = 48.sp,
                                lineHeight = 56.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 12.sp,
                                color = AzulNoche,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 24.dp)
                                    // El lector de pantalla lo deletrea: "K, 7, M, 2, Q, 9"
                                    .semantics {
                                        contentDescription = "Tu código: " + estado.codigo.toList().joinToString(", ")
                                    }
                            )
                            HorizontalDivider(thickness = 1.dp, color = BordeSuave)
                            Row(
                                modifier = Modifier.padding(top = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Escuchar: solo el ícono
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, BordeCampo, CircleShape)
                                        .clickable(role = Role.Button, onClick = onEscuchar),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Iconos.Bocina, contentDescription = "Escuchar el código",
                                        tint = AcentoMorado, modifier = Modifier.size(24.dp))
                                }
                                BotonCodigo("Copiar", IconoCopiar, oscuro = false, Modifier.weight(1f)) {
                                    portapapeles.setText(AnnotatedString(estado.codigo))
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                }
                                BotonCodigo("Enviar", IconoEnviar, oscuro = true, Modifier.weight(1f)) {
                                    val texto = "Mi código de BuenVivir es ${codigoEnGrupos(estado.codigo)}. " +
                                        "Escríbelo en tu app para ver si tomo mis pastillas."
                                    val envio = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, texto)
                                    }
                                    context.startActivity(Intent.createChooser(envio, "Enviar mi código"))
                                }
                            }
                        }

                        EstadoCodigo.Error -> Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MensajeEnLinea("No se pudo mostrar tu código. Revisa que tengas internet.")
                            BotonOscuro("Intentar otra vez", onClick = onReintentar)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Iconos.Candado, contentDescription = null, tint = TextoSuave, modifier = Modifier.size(16.dp))
                Text("Solo dáselo a personas de confianza.", fontSize = 14.sp, lineHeight = 20.sp, color = TextoSuave)
            }

            MenuInferior(seleccionado = 2, onInicio = onIrInicio, onMedicinas = onIrMedicinas, onFamilia = onRegresar)
        }
    }
}

/** Botón de 56 en forma de píldora: "Copiar" con borde, "Enviar" azul marino. */
@Composable
private fun BotonCodigo(texto: String, icono: ImageVector, oscuro: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(forma)
            .then(if (oscuro) Modifier.background(AzulNoche) else Modifier.border(1.dp, BordeCampo, forma))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = if (oscuro) Color.White else AcentoMorado, modifier = Modifier.size(20.dp))
        Text(texto, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
            color = if (oscuro) Color.White else AzulNoche)
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
