package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.theme.*

/** Qué se ve en "Mi código": todavía cargando, listo, o no se pudo (sin internet). */
sealed interface EstadoCodigo {
    data object Cargando : EstadoCodigo
    data class Listo(val codigo: String) : EstadoCodigo
    data object Error : EstadoCodigo
}

/** "K7M2Q9" -> "K7M 2Q9", más fácil de leer y de dictar. */
fun codigoEnGrupos(codigo: String) = codigo.chunked(3).joinToString(" ")

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onRegresar)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Iconos.Atras, contentDescription = null, tint = TextoChip, modifier = Modifier.size(24.dp))
                Text("Mi familia", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoChip)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Tu código",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "Dáselo a tu médico o a un familiar para que pueda ver si tomaste tus pastillas.",
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    color = TextoGris
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Lavanda)
                    .padding(horizontal = 18.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (estado) {
                    EstadoCodigo.Cargando -> {
                        CircularProgressIndicator(color = TextoChip, modifier = Modifier.size(44.dp))
                        Text("Creando tu código…", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
                    }
                    is EstadoCodigo.Listo -> {
                        Text(
                            codigoEnGrupos(estado.codigo),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextoOscuro,
                            letterSpacing = 8.sp,
                            // El lector de pantalla lo deletrea: "K, 7, M, 2, Q, 9"
                            modifier = Modifier.semantics {
                                contentDescription = "Tu código: " + estado.codigo.toList().joinToString(", ")
                            }
                        )
                        BotonClaro("Escuchar el código", Iconos.Bocina, onEscuchar)
                    }
                    EstadoCodigo.Error -> {
                        Text(
                            "No se pudo mostrar tu código. Revisa que tengas internet.",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp,
                            color = TextoOscuro,
                            textAlign = TextAlign.Center
                        )
                        BotonClaro("Intentar otra vez", Iconos.Flecha, onReintentar)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Iconos.Candado, contentDescription = null, tint = TextoGrisMedio, modifier = Modifier.size(22.dp))
                Text("Solo dáselo a personas de confianza.", fontSize = 17.sp, lineHeight = 24.sp, color = TextoGrisMedio)
            }
        }

        MenuInferior(seleccionado = 2, onInicio = onIrInicio, onMedicinas = onIrMedicinas, onFamilia = onRegresar)
    }
}

/** Botón blanco en forma de píldora, sobre el fondo lavanda. */
@Composable
private fun BotonClaro(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blanco, contentColor = AzulBoton),
        elevation = null
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
