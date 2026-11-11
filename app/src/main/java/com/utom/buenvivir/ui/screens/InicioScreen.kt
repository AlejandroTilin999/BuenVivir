package com.utom.buenvivir.ui.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.EnlaceSecundario
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
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
            .background(Blanco)
    ) {
        Encabezado(nombre = nombre, tomadas = tomadas, total = total, onAjustes = onAjustes)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TarjetaToma(toma, estado, horaTomada, atrasada, manana, registrarDesde, onEscuchar, onTomar, onNoTomada)
            // Las que siguen se ven siempre, aunque la de arriba siga pendiente
            if (siguientes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Después",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro,
                        modifier = Modifier.semantics { heading() }
                    )
                    siguientes.forEach { TarjetaSiguiente(it) }
                }
            }
        }

        MenuInferior(seleccionado = 0, onInicio = {}, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
    }
}

// ---------- Encabezado lavanda con saludo y progreso ----------

@Composable
private fun Encabezado(nombre: String, tomadas: Int, total: Int, onAjustes: () -> Unit) {
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(saludo(), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
                Text(
                    nombre,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Blanco)
                    .clickable(role = Role.Button, onClick = onAjustes),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Engrane, contentDescription = "Ajustes", tint = AzulBoton, modifier = Modifier.size(26.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Blanco)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (total == 0) "Hoy ya no te toca ninguna pastilla"
                else "Hoy: $tomadas de $total pastillas tomadas",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoGris
            )
            if (total > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(total) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (i < tomadas) Violeta else Lavanda)
                    )
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
    // La tarjeta se tiñe según lo que pasó: morado si la tomó, gris si no.
    val (fondo, borde) = when (estado) {
        EstadoToma.Pendiente -> Blanco to BordeTarjeta
        EstadoToma.Tomada -> Blanco to BordeTarjeta
        is EstadoToma.Omitida -> FondoOmitida to BordeOmitida
    }
    // El chip de la hora se apaga cuando la pastilla ya pasó, para que no parezca pendiente.
    val (fondoChip, colorChip, iconoChip) = when (estado) {
        EstadoToma.Pendiente -> Triple(LavandaChip, TextoChip, Iconos.Reloj)
        EstadoToma.Tomada -> Triple(ChipApagado, TextoApagado, Iconos.Palomita)
        is EstadoToma.Omitida -> Triple(ChipApagado, TextoApagado, Iconos.Menos)
    }
    val etiquetaHora = when {
        manana -> "Mañana · ${toma.hora}"
        estado != EstadoToma.Pendiente -> "Hoy · ${toma.hora}"
        atrasada -> "Pendiente · ${toma.hora}"
        else -> "Ahora · ${toma.hora}"
    }
    val forma = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borde, forma)
            .clip(forma)
            .background(fondo)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hora + Escuchar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(fondoChip)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(iconoChip, contentDescription = null, tint = colorChip, modifier = Modifier.size(20.dp))
                Text(etiquetaHora, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colorChip)
            }
            BotonEscuchar("Escuchar las indicaciones de esta pastilla", LavandaChip, onEscuchar)
        }

        // Nombre y para qué sirve
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                toma.medicina,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoNormal,
                letterSpacing = (-0.5).sp
            )
            Text(toma.para, fontSize = 17.sp, color = TextoSecundario)
        }

        when (estado) {
            EstadoToma.Pendiente -> {
                Text(
                    buildAnnotatedString {
                        append("Toma ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(toma.cantidad) }
                        append(" ${toma.indicacion}.")
                    },
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                    color = TextoNormal
                )
                // Solo se puede marcar desde 5 minutos antes de su hora
                if (registrarDesde != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(LavandaChip)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Iconos.Reloj, contentDescription = null, tint = TextoChip, modifier = Modifier.size(24.dp))
                        Text(
                            if (manana) "Podrás marcarla mañana desde las $registrarDesde"
                            else "Podrás marcarla desde las $registrarDesde",
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoChip
                        )
                    }
                } else Column {
                    Button(
                        onClick = onTomar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(64.dp)
                            // Sombra suave para que se note que es un botón
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(32.dp),
                                ambientColor = AzulBoton,
                                spotColor = AzulBoton
                            ),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulBoton, contentColor = Blanco),
                        elevation = null
                    ) {
                        Icon(Iconos.Pastilla, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Tomar pastilla", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    EnlaceSecundario("No me la tomé", onNoTomada)
                }
            }

            EstadoToma.Tomada -> Column {
                Aviso(
                    fondo = AvisoTomada,
                    borde = BordeAvisoTomada,
                    fondoIcono = Violeta,
                    icono = Iconos.Palomita,
                    colorIcono = Blanco,
                    titulo = "Ya te la tomaste",
                    colorTitulo = TextoMorado,
                    detalle = horaTomada,
                    colorDetalle = TextoMoradoMedio
                )
                EnlaceSecundario("Me equivoqué, no me la tomé", onNoTomada)
            }

            is EstadoToma.Omitida -> Column {
                Aviso(
                    fondo = GrisAviso,
                    fondoIcono = Blanco,
                    icono = Iconos.Menos,
                    colorIcono = GrisAviso,
                    titulo = "No la tomaste",
                    colorTitulo = Blanco,
                    detalle = "Motivo: ${estado.motivo}",
                    colorDetalle = TextoGrisAviso
                )
                EnlaceSecundario("Ya me la tomé", onTomar)
            }
        }
    }
}

/** Recuadro de aviso (no es botón): círculo con ícono, título y un detalle. */
@Composable
private fun Aviso(
    fondo: Color,
    borde: Color? = null,
    fondoIcono: Color,
    icono: ImageVector,
    colorIcono: Color,
    titulo: String,
    colorTitulo: Color,
    detalle: String?,
    colorDetalle: Color
) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (borde != null) Modifier.border(2.dp, borde, forma) else Modifier)
            .clip(forma)
            .background(fondo)
            .padding(if (borde != null) 12.dp else 14.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(fondoIcono),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colorTitulo)
            detalle?.let {
                Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorDetalle)
            }
        }
    }
}

// ---------- Siguiente pastilla ----------

@Composable
private fun TarjetaSiguiente(siguiente: ProximaToma) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Sombra corta, se nota sobre todo abajo
            .shadow(elevation = 3.dp, shape = forma, ambientColor = TextoOscuro, spotColor = TextoOscuro)
            .border(2.dp, BordeTarjeta, forma)
            .clip(forma)
            .background(Blanco)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Lavanda),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Pastilla, contentDescription = null, tint = TextoChip, modifier = Modifier.size(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(siguiente.medicina, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
            Text(siguiente.cuando, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario)
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
