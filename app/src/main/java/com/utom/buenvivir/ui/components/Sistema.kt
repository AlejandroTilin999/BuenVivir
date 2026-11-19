package com.utom.buenvivir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Sistema de diseño de BuenVivir: los mismos valores en todas las pantallas.
 *  - Grid de 4 columnas en celular: márgenes de 16 y separación de 16.
 *  - Espacios en múltiplos de 8.
 *  - Escala de letra fija (tamaño / alto de línea).
 *  - Botones de 56 y áreas táctiles de 48 o más.
 */

/** Espacios (regla de 8). */
object Espacio {
    val S = 8.dp      // entre cosas de un mismo grupo
    val M = 16.dp     // márgenes de la pantalla y relleno de tarjetas
    val L = 24.dp     // entre bloques
    val XL = 32.dp    // entre secciones
}

/** Tamaños que se repiten. */
object Medida {
    val Boton = 56.dp         // botón principal
    val Tactil = 48.dp        // mínimo para tocar (iconos, enlaces)
    val Chip = 48.dp
    val Fila = 80.dp          // tarjeta de lista
    val RadioTarjeta = 24.dp
    val RadioCaja = 16.dp
}

/**
 * Escala de letra pensada para adultos mayores: el texto normal es de 18 (no de 16),
 * nada baja de 16 y nada es delgado (mínimo Medium). Los altos de línea siguen la regla de 4/8.
 */
object Letra {
    val Nombre = TextStyle(fontFamily = Poppins, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    val Titulo = TextStyle(fontFamily = Poppins, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    val Subtitulo = TextStyle(fontFamily = Poppins, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
    val Grande = TextStyle(fontFamily = Poppins, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    val Texto = TextStyle(fontFamily = Poppins, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    val TextoFuerte = TextStyle(fontFamily = Poppins, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val Etiqueta = TextStyle(fontFamily = Poppins, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val EtiquetaFuerte = TextStyle(fontFamily = Poppins, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val Boton = TextStyle(fontFamily = Poppins, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
}

/** Colores del sistema (azul). */
object Tono {
    val Acento = Color(0xFF004EE0)        // azul: lo importante y los enlaces
    val Noche = Color(0xFF0B1F4D)         // texto principal y botón principal
    val Fondo = Color(0xFFEAF1FE)         // encabezados
    val Saludo = Color(0xFFC5D8FB)        // tarjeta azul de arriba (saludo y encabezados), un poco más fuerte
    val Chip = Color(0xFFEFF4FF)          // chips y cajas suaves
    val Borde = Color(0xFFE1E7F2)         // bordes de cajas
    val BordeFuerte = Color(0xFFC5D0E3)   // borde de la tarjeta principal
    val BordeCampo = Color(0xFFAEB9D0)    // campos y botones de contorno
    val Suave = Color(0xFF3A4150)         // texto secundario: gris oscuro, se lee bien (contraste alto)
    val Gris = Color(0xFFF4F5F7)          // fondo de "no la tomaste" y chips apagados
    val Blanco = Color(0xFFFFFFFF)
}

// ---------------------------------------------------------------------------------------------
// Piezas que se repiten en las pantallas del paciente (todas usan los valores de arriba)
// ---------------------------------------------------------------------------------------------

/**
 * Encabezado azul claro con esquinas redondeadas abajo: (Regresar) + título + Escuchar,
 * y debajo lo que cada pantalla necesite (la semana, una explicación...).
 */
@Composable
fun SisEncabezado(
    titulo: String,
    onEscuchar: (() -> Unit)? = null,
    regresar: Pair<String, () -> Unit>? = null,
    debajo: @Composable ColumnScope.() -> Unit = {}
) {
    // Tarjeta azul con círculos de adorno, igual que el saludo de Inicio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Espacio.M, end = Espacio.M, top = Espacio.S)
            .tarjetaSaludo()
            .padding(start = Espacio.M, end = Espacio.M, top = if (regresar != null) Espacio.S else Espacio.L, bottom = Espacio.L),
        verticalArrangement = Arrangement.spacedBy(Espacio.S)
    ) {
        regresar?.let { (texto, onClick) -> SisRegresar(texto, onClick) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, style = Letra.Titulo, color = Tono.Noche, modifier = Modifier.weight(1f).semantics { heading() })
            onEscuchar?.let { SisBotonIcono(Iconos.Bocina, "Escuchar esta pantalla", Tono.Blanco, Tono.Acento, it) }
        }
        debajo()
    }
}

/**
 * Fondo de la tarjeta azul de arriba: esquinas redondeadas, azul [Tono.Saludo]
 * y dos círculos blancos muy suaves en la esquina de abajo, de adorno.
 */
fun Modifier.tarjetaSaludo(): Modifier = this
    .clip(RoundedCornerShape(Medida.RadioTarjeta))
    .background(Tono.Saludo)
    .drawBehind {
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 88.dp.toPx(),
            center = Offset(size.width - 40.dp.toPx(), size.height - 24.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 64.dp.toPx(),
            center = Offset(size.width - 112.dp.toPx(), size.height + 24.dp.toPx())
        )
    }

/** "< Mis medicinas": regresar, de 48 de alto. */
@Composable
fun SisRegresar(texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(Medida.Tactil)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = Espacio.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.S)
    ) {
        Icon(Iconos.Atras, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(24.dp))
        Text(texto, style = Letra.TextoFuerte, color = Tono.Acento)
    }
}

/** Botón redondo de 48 con un ícono (Ajustes, Escuchar). */
@Composable
fun SisBotonIcono(icono: ImageVector, descripcion: String, fondo: Color, color: Color, onClick: () -> Unit) {
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

/** Sección: subtítulo de 20/28 y su contenido, separados 16. */
@Composable
fun SisSeccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.M)) {
        Text(titulo, style = Letra.Subtitulo, color = Tono.Noche, modifier = Modifier.semantics { heading() })
        contenido()
    }
}

/** Caja con borde fino y relleno de 16. */
@Composable
fun SisCaja(
    modifier: Modifier = Modifier,
    espacio: androidx.compose.ui.unit.Dp = Espacio.M,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Tono.Borde, RoundedCornerShape(Medida.RadioCaja))
            .padding(Espacio.M),
        verticalArrangement = Arrangement.spacedBy(espacio),
        content = contenido
    )
}

/** Línea divisoria dentro de una caja. */
@Composable
fun SisSeparador() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Tono.Borde))
}

/** Botón principal (56, azul oscuro) o de contorno. */
@Composable
fun SisBoton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    contorno: Boolean = false,
    alto: androidx.compose.ui.unit.Dp = Medida.Boton
) {
    val forma = RoundedCornerShape(alto / 2)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(forma)
            .then(if (contorno) Modifier.border(1.dp, Tono.BordeCampo, forma).background(Tono.Blanco) else Modifier.background(Tono.Noche))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Espacio.M),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (contorno) Tono.Noche else Tono.Blanco
        icono?.let {
            Icon(it, contentDescription = null, tint = if (contorno) Tono.Acento else color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(Espacio.S))
        }
        Text(texto, style = Letra.Boton, color = color)
    }
}

/** Enlace azul de 48 de alto. */
@Composable
fun SisEnlace(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Tono.Acento) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Medida.Tactil)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = Letra.TextoFuerte, color = color)
    }
}

/** Ícono en un cuadrito de 40 (listas de medicinas, personas). */
@Composable
fun SisIconoLista(icono: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Tono.Chip),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = Tono.Acento, modifier = Modifier.size(24.dp))
    }
}

/** Chip de estado ("Tomada", "Pendiente", "No la tomó"). */
@Composable
fun SisEstado(texto: String, tomada: Boolean?) {
    val color = when (tomada) {
        true -> Tono.Acento
        false -> Tono.Suave
        null -> Tono.Suave
    }
    Text(texto, style = Letra.EtiquetaFuerte, color = color)
}

/** Azul claro de las tarjetas de arriba (un poco más fuerte que el de los encabezados). */
val AzulTarjeta = Color(0xFFD3E3FD)

/** Tarjeta azul claro con dos círculos blancos suaves de adorno en la esquina. */
@Composable
fun SisTarjetaAzul(contenido: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Medida.RadioTarjeta))
            .background(AzulTarjeta)
    ) {
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
            verticalArrangement = Arrangement.spacedBy(Espacio.S),
            content = contenido
        )
    }
}
