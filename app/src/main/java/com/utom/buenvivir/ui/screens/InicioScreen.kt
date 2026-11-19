package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.ProximaToma
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.data.Toma
import com.utom.buenvivir.ui.components.Espacio
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.Letra
import com.utom.buenvivir.ui.components.Medida
import com.utom.buenvivir.ui.components.Tono
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.components.SisBotonIcono
import com.utom.buenvivir.ui.theme.*
import java.time.LocalTime

/** En qué estado está la pastilla que toca ahora. */
sealed interface EstadoToma {
    data object Pendiente : EstadoToma
    data object Tomada : EstadoToma
    data class Omitida(val motivo: String) : EstadoToma
}

@Composable
fun InicioScreen(
    nombre: String,
    tomadas: Int,
    total: Int,
    toma: Toma,
    estado: EstadoToma,
    siguiente: ProximaToma,
    horaTomada: String? = null,
    siguientes: List<ProximaToma> = listOf(siguiente),   // todas las que vienen, en orden de hora
    atrasada: Boolean = false,                           // ya pasó su hora y no la ha registrado
    manana: Boolean = false,                             // hoy ya no le toca nada: la de arriba es de mañana
    registrarDesde: String? = null,                      // si no es null, todavía no puede marcarla (falta mucho)
    onEscuchar: () -> Unit = {},
    onTomar: () -> Unit = {},
    onNoTomada: () -> Unit = {},
    onIrMedicinas: () -> Unit = {},
    onIrFamilia: () -> Unit = {},
    onAjustes: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tono.Blanco)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.XL)
        ) {
            // Mismo saludo que la pantalla sin medicinas: tarjeta azul claro con círculos de adorno
            Encabezado(nombre = nombre, tomadas = tomadas, total = total, onAjustes = onAjustes)
            TarjetaToma(toma, estado, horaTomada, atrasada, manana, registrarDesde, onEscuchar, onTomar, onNoTomada)
            // Las que siguen se ven siempre, aunque la de arriba siga pendiente
            if (siguientes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.M)) {
                    Text("Después", style = Letra.Subtitulo, color = Tono.Noche, modifier = Modifier.semantics { heading() })
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                        siguientes.forEach { TarjetaSiguiente(it) }
                    }
                }
            }
        }

        MenuInferior(seleccionado = 0, onInicio = {}, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
    }
}

// ---------- Encabezado con saludo y progreso ----------

/** Azul claro del saludo (el mismo de la pantalla sin medicinas). */
private val AzulSaludo = Tono.Saludo

@Composable
private fun Encabezado(nombre: String, tomadas: Int, total: Int, onAjustes: () -> Unit) {
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
                    Text(saludo(), style = Letra.Grande, color = Tono.Acento)
                    Text(nombre, style = Letra.Nombre, color = Tono.Noche, modifier = Modifier.semantics { heading() })
                }
                SisBotonIcono(Iconos.Engrane, "Ajustes", Tono.Blanco, Tono.Noche, onAjustes)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Tono.Blanco)
                    .padding(Espacio.M),
                verticalArrangement = Arrangement.spacedBy(Espacio.S)
            ) {
                Text(
                    if (total == 0) "Hoy ya no te toca ninguna pastilla"
                    else "Hoy: $tomadas de $total pastillas tomadas",
                    style = Letra.EtiquetaFuerte,
                    color = Tono.Acento
                )
                if (total > 0) Row(horizontalArrangement = Arrangement.spacedBy(Espacio.S)) {
                    repeat(total) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(Espacio.S)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i < tomadas) Tono.Acento else Tono.Saludo)
                        )
                    }
                }
            }
        }
    }
}

private fun saludo(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Buenos días,"
    in 12..18 -> "Buenas tardes,"
    else -> "Buenas noches,"
}

/** Botón redondo de 48 con un ícono (Ajustes, Escuchar). */
@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, fondo: Color, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Medida.Tactil)
            .clip(CircleShape)
            .background(fondo)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = color, modifier = Modifier.size(24.dp))
    }
}

// ---------- Tarjeta de la pastilla (cambia según el estado) ----------

@Composable
private fun TarjetaToma(
    toma: Toma,
    estado: EstadoToma,
    horaTomada: String?,
    atrasada: Boolean,
    manana: Boolean,
    registrarDesde: String?,
    onEscuchar: () -> Unit,
    onTomar: () -> Unit,
    onNoTomada: () -> Unit
) {
    // El chip de la hora se apaga cuando la pastilla ya pasó, para que no parezca pendiente
    val pendiente = estado == EstadoToma.Pendiente
    val (fondoChip, colorChip) = if (pendiente) Tono.Chip to Tono.Acento else Tono.Gris to Tono.Suave
    val iconoChip = when (estado) {
        EstadoToma.Pendiente -> Iconos.Reloj
        EstadoToma.Tomada -> Iconos.Palomita
        is EstadoToma.Omitida -> Iconos.Menos
    }
    val etiquetaHora = when {
        manana -> "Mañana · ${toma.hora}"
        !pendiente -> "Hoy · ${toma.hora}"
        atrasada -> "Pendiente · ${toma.hora}"
        else -> "Ahora · ${toma.hora}"
    }
    val forma = RoundedCornerShape(Medida.RadioTarjeta)
    // Tomada = verde, no tomada = naranja; pendiente se queda blanca
    val (fondoTarjeta, bordeTarjeta) = when (estado) {
        EstadoToma.Pendiente -> Tono.Blanco to Tono.BordeFuerte
        EstadoToma.Tomada -> VerdeFondo to VerdeBorde
        is EstadoToma.Omitida -> NaranjaFondo to NaranjaBorde
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (pendiente) 1.dp else 2.dp, bordeTarjeta, forma)
            .clip(forma)
            .background(fondoTarjeta)
            .padding(Espacio.M),
        verticalArrangement = Arrangement.spacedBy(Espacio.M)
    ) {
        // Hora + Escuchar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .height(Medida.Chip)
                    .clip(RoundedCornerShape(20.dp))
                    .background(fondoChip)
                    .padding(horizontal = Espacio.M),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.S)
            ) {
                Icon(iconoChip, contentDescription = null, tint = colorChip, modifier = Modifier.size(20.dp))
                Text(etiquetaHora, style = Letra.Etiqueta.copy(fontWeight = FontWeight.SemiBold), color = colorChip)
            }
            BotonRedondo(Iconos.Bocina, "Escuchar las indicaciones de esta pastilla", Tono.Chip, Tono.Acento, onEscuchar)
        }

        // Nombre y para qué sirve
        Column {
            Text(toma.medicina, style = Letra.Titulo, color = Tono.Noche)
            Text(toma.para, style = Letra.Texto, color = Tono.Suave)
        }

        when (estado) {
            EstadoToma.Pendiente -> {
                Text(
                    buildAnnotatedString {
                        append("Toma ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(toma.cantidad) }
                        append(" ${toma.indicacion}.")
                    },
                    style = Letra.Grande,
                    color = Tono.Noche
                )
                // Solo se puede marcar desde 5 minutos antes de su hora
                if (registrarDesde != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Medida.RadioCaja))
                            .background(Tono.Chip)
                            .padding(Espacio.M),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.M)
                    ) {
                        Icon(Iconos.Reloj, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(24.dp))
                        Text(
                            if (manana) "Podrás marcarla mañana desde las $registrarDesde"
                            else "Podrás marcarla desde las $registrarDesde",
                            style = Letra.Texto.copy(fontWeight = FontWeight.SemiBold),
                            color = Tono.Acento
                        )
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                    Button(
                        onClick = onTomar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Medida.Boton),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Tono.Noche, contentColor = Tono.Blanco),
                        elevation = null
                    ) {
                        Icon(Iconos.Pastilla, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(Espacio.S))
                        Text("Tomar pastilla", style = Letra.Boton)
                    }
                    Enlace("No me la tomé", onNoTomada)
                }
            }

            EstadoToma.Tomada -> EstadoGrande(
                tomada = true,
                titulo = "¡Ya te la tomaste!",
                color = Verde,
                etiqueta = horaTomada?.let { "Registrada a las $it" },
                enlace = "Me equivoqué, no me la tomé",
                onEnlace = onNoTomada
            )

            is EstadoToma.Omitida -> EstadoGrande(
                tomada = false,
                titulo = "No te la tomaste",
                color = Naranja,
                etiqueta = "Motivo: ${estado.motivo}",
                enlace = "Ya me la tomé",
                onEnlace = onTomar
            )
        }
    }
}

// ---------- Estado grande: ícono, color y texto (no depende solo del color) ----------

private val Verde = Color(0xFF1B7F46)
private val VerdeFondo = Color(0xFFE8F6EE)
private val VerdeBorde = Color(0xFF9ED5B4)
private val Naranja = Color(0xFFB45309)       // naranja y no rojo: avisa sin asustar ni regañar
private val NaranjaFondo = Color(0xFFFFF4E5)
private val NaranjaBorde = Color(0xFFF5C58E)

@Composable
private fun EstadoGrande(
    tomada: Boolean,
    titulo: String,
    color: Color,
    etiqueta: String?,
    enlace: String,
    onEnlace: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.S)
    ) {
        IconoEstado(tomada, color)
        Text(titulo, style = Letra.Titulo, color = color, textAlign = TextAlign.Center)
        etiqueta?.let {
            Text(
                it,
                style = Letra.EtiquetaFuerte,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Tono.Blanco)
                    .padding(horizontal = Espacio.M, vertical = 6.dp)
            )
        }
        Enlace(enlace, onEnlace)
    }
}

/** Tomada: persona con palomita. No tomada: pastilla con tache. En un círculo blanco de 96. */
@Composable
private fun IconoEstado(tomada: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(96.dp).clip(CircleShape).background(Tono.Blanco),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(56.dp)) {
            val u = size.width / 56f
            val trazo = Stroke(width = 3.5f * u, cap = StrokeCap.Round)
            if (tomada) {
                drawCircle(color, radius = 8f * u, center = Offset(24f * u, 17f * u), style = trazo)
                val hombros = Path().apply {
                    moveTo(8f * u, 46f * u)
                    cubicTo(8f * u, 36.6f * u, 15.2f * u, 30f * u, 24f * u, 30f * u)
                    cubicTo(32.8f * u, 30f * u, 40f * u, 36.6f * u, 40f * u, 46f * u)
                }
                drawPath(hombros, color, style = trazo)
            } else {
                rotate(-35f, pivot = Offset(24f * u, 26f * u)) {
                    drawRoundRect(
                        color, topLeft = Offset(8f * u, 18f * u), size = Size(32f * u, 16f * u),
                        cornerRadius = CornerRadius(8f * u), style = trazo
                    )
                    drawLine(color, Offset(24f * u, 18f * u), Offset(24f * u, 34f * u), strokeWidth = 3.5f * u)
                }
            }
            // Insignia: palomita o tache en blanco sobre un círculo del color
            drawCircle(color, radius = 11f * u, center = Offset(42f * u, 40f * u))
            if (tomada) {
                val palomita = Path().apply {
                    moveTo(37f * u, 40.5f * u); lineTo(40.4f * u, 43.9f * u); lineTo(46.6f * u, 37.3f * u)
                }
                drawPath(palomita, Color.White, style = Stroke(width = 3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawLine(Color.White, Offset(38f * u, 36f * u), Offset(46f * u, 44f * u), strokeWidth = 3f * u, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(46f * u, 36f * u), Offset(38f * u, 44f * u), strokeWidth = 3f * u, cap = StrokeCap.Round)
            }
        }
    }
}

/** Enlace de 48 de alto, en azul ("No me la tomé", "Ya me la tomé"). */
@Composable
private fun Enlace(texto: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Medida.Tactil)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = Letra.Texto.copy(fontWeight = FontWeight.SemiBold), color = Tono.Acento)
    }
}

/** Recuadro de aviso (no es botón): círculo con ícono, título y un detalle. */
@Composable
private fun Aviso(
    fondo: Color,
    fondoIcono: Color,
    icono: ImageVector,
    colorIcono: Color,
    titulo: String,
    detalle: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Medida.RadioCaja))
            .background(fondo)
            .padding(Espacio.M)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Medida.Tactil)
                .clip(CircleShape)
                .background(fondoIcono),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(24.dp))
        }
        Column {
            Text(titulo, style = Letra.Grande.copy(fontWeight = FontWeight.Bold), color = Tono.Noche)
            detalle?.let { Text(it, style = Letra.Texto, color = Tono.Noche) }
        }
    }
}

// ---------- Siguiente pastilla ----------

@Composable
private fun TarjetaSiguiente(siguiente: ProximaToma) {
    val forma = RoundedCornerShape(Medida.RadioCaja)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .border(1.dp, Tono.Borde, forma)
            .clip(forma)
            .background(Tono.Blanco)
            .padding(Espacio.M)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Tono.Chip),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Pastilla, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(24.dp))
        }
        Column {
            Text(siguiente.medicina, style = Letra.Texto.copy(fontWeight = FontWeight.SemiBold), color = Tono.Noche)
            Text(siguiente.cuando, style = Letra.Etiqueta, color = Tono.Suave)
        }
    }
}

// ---------- Vistas previas ----------

@Preview(name = "Pendiente", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewPendiente() = BuenVivirTheme {
    InicioScreen(Repositorio.nombrePaciente, 1, 2, Repositorio.tomaActual, EstadoToma.Pendiente, Repositorio.siguiente)
}

@Preview(name = "Tomada", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewTomada() = BuenVivirTheme {
    InicioScreen(Repositorio.nombrePaciente, 2, 2, Repositorio.tomaActual, EstadoToma.Tomada, Repositorio.siguiente, horaTomada = "8:05 p. m.")
}

@Preview(name = "Omitida", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewOmitida() = BuenVivirTheme {
    InicioScreen(Repositorio.nombrePaciente, 1, 2, Repositorio.tomaActual, EstadoToma.Omitida("No tenía la medicina"), Repositorio.siguiente)
}
