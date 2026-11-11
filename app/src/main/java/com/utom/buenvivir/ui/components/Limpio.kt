package com.utom.buenvivir.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.R
import kotlinx.coroutines.delay

/*
 * Estilo limpio del rediseño: letra Poppins, fondo blanco, acento morado
 * y botones azul noche. Medidas en múltiplos de 8 (márgenes 16, espacios 8/16/24).
 */

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

// Paleta azul (los nombres se quedaron de la versión morada, para no cambiar todas las pantallas)
val AcentoMorado = Color(0xFF004EE0)      // azul del logo: una palabra del título y la opción elegida
val AzulNoche = Color(0xFF0B1F4D)         // azul marino: textos fuertes y botón principal
val FondoLavandaSuave = Color(0xFFEAF1FE) // azul muy claro: bloque de arriba con la imagen
val ChipLavanda = Color(0xFFEFF4FF)       // círculos de los íconos y etiquetas
val SeleccionLavanda = Color(0xFFF3F7FF)  // fondo de la opción elegida
val BordeSuave = Color(0xFFE1E7F2)
val BordeCampo = Color(0xFFAEB9D0)        // borde de los campos de texto (fondo blanco)
val TextoSuave = Color(0xFF5B6170)
val RojoError = Color(0xFFB3261E)         // errores: borde del campo y mensaje debajo

/** Íconos de trazo para los mensajes: círculo con "!" y círculo con palomita. */
private fun trazo(vararg caminos: String): ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).apply {
    caminos.forEach {
        addPath(
            pathData = addPathNodes(it),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
}.build()

val IconoAlerta = trazo("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M12 7.5v5.5", "M12 16.5v0.01")
val IconoListo = trazo("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M8 12.5l3 3l5 -6")

/** Pone la letra Poppins a todo lo que va adentro (el color lo pone cada texto). */
@Composable
fun ConPoppins(contenido: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Poppins),
        content = contenido
    )
}

/** Título de 28 con una palabra en morado: "Antes de **empezar**". */
@Composable
fun TituloConAcento(antes: String, acento: String, despues: String = "", centrado: Boolean = false) {
    Text(
        buildAnnotatedString {
            append(antes)
            withStyle(SpanStyle(color = AcentoMorado)) { append(acento) }
            append(despues)
        },
        fontFamily = Poppins,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = AzulNoche,
        textAlign = if (centrado) TextAlign.Center else TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
    )
}

/** Botón principal: azul noche, forma de píldora, 56 de alto. */
@Composable
fun BotonOscuro(
    texto: String,
    ocupado: Boolean = false,
    habilitado: Boolean = true,
    alto: androidx.compose.ui.unit.Dp = 56.dp,
    letra: androidx.compose.ui.unit.TextUnit = 18.sp,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !ocupado,
        modifier = Modifier
            .fillMaxWidth()
            .height(alto),
        shape = RoundedCornerShape(alto / 2),
        colors = ButtonDefaults.buttonColors(
            containerColor = AzulNoche,
            contentColor = Color.White,
            disabledContainerColor = if (ocupado) AzulNoche else BordeSuave,
            disabledContentColor = if (ocupado) Color.White else TextoSuave
        ),
        elevation = null
    ) {
        if (ocupado) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
        } else {
            // Color del botón (blanco, o gris si está apagado)
            Text(texto, fontFamily = Poppins, fontSize = letra, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                color = LocalContentColor.current)
        }
    }
}

/** Botón con borde y un ícono morado, como "Escuchar el aviso". */
@Composable
fun BotonContorno(texto: String, icono: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BordeSuave, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icono, contentDescription = null, tint = AcentoMorado, modifier = Modifier.size(24.dp))
        Text(texto, fontFamily = Poppins, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = AzulNoche)
    }
}

/** Enlace morado de 48 de alto, para que sea fácil de tocar. */
@Composable
fun EnlaceLimpio(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, negrita: Boolean = false) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            fontFamily = Poppins,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium,
            color = AcentoMorado,
            textAlign = TextAlign.Center
        )
    }
}

/** Ícono dentro de un círculo de 48. */
@Composable
fun IconoEnCirculo(icono: ImageVector, fondo: Color = ChipLavanda, tinte: Color = AcentoMorado) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(24.dp))
    }
}

/** Etiqueta pequeña con ícono: "Médico", "Cuidador", "Paciente". */
@Composable
fun ChipRol(icono: ImageVector, texto: String) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ChipLavanda)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icono, contentDescription = null, tint = AcentoMorado, modifier = Modifier.size(16.dp))
        Text(texto, fontFamily = Poppins, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = AcentoMorado)
    }
}

/** Bloque lavanda de arriba con el botón de regresar y el frasco de medicina. */
@Composable
fun BloqueFrasco(etiquetaRegresar: String, onRegresar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(FondoLavandaSuave)
            .statusBarsPadding()
            .height(176.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(role = Role.Button, onClick = onRegresar)
                .semantics { contentDescription = etiquetaRegresar },
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Atras, contentDescription = null, tint = AzulNoche, modifier = Modifier.size(24.dp))
        }
        Image(
            painterResource(R.drawable.frasco),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .width(232.dp)
                .height(128.dp)
        )
    }
}

/**
 * Mensaje en una línea, sin recuadro: ícono y texto en rojo si es error,
 * o en morado si salió bien ("Te mandamos un correo...").
 */
@Composable
fun MensajeEnLinea(texto: String, esError: Boolean = true) {
    val color = if (esError) RojoError else AcentoMorado
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(if (esError) IconoAlerta else IconoListo, contentDescription = null, tint = color,
            modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Text(texto, fontFamily = Poppins, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

/** "🔒 Tu información viaja cifrada", centrado. */
@Composable
fun FilaCifrada() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Iconos.Candado, contentDescription = null, tint = TextoSuave, modifier = Modifier.size(16.dp))
        Text("Tu información viaja cifrada", fontFamily = Poppins, fontSize = 14.sp, lineHeight = 20.sp, color = TextoSuave)
    }
}

/**
 * Campo de texto blanco con borde fino (morado al escribir), con su etiqueta arriba.
 * Si [error] no es null, el borde se pone rojo y el error sale debajo; si no, sale la [ayuda].
 */
@Composable
fun CampoLimpio(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    esContrasena: Boolean = false,
    ultimo: Boolean = false,
    onListo: () -> Unit = {},
    ayuda: String? = null,
    error: String? = null,
    modifier: Modifier = Modifier       // para saber cuándo lo tocan (onFocusChanged)
) {
    var verContrasena by remember { mutableStateOf(false) }
    val chico = TextStyle(fontFamily = Poppins, fontSize = 14.sp, lineHeight = 20.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(etiqueta, fontFamily = Poppins, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = AzulNoche)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = true,
            isError = error != null,
            textStyle = TextStyle(fontFamily = Poppins, fontSize = 18.sp, color = AzulNoche),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AcentoMorado,
                unfocusedBorderColor = BordeCampo,
                errorBorderColor = RojoError,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                cursorColor = AcentoMorado,
                errorCursorColor = RojoError,
                errorTrailingIconColor = TextoSuave,
                focusedSupportingTextColor = TextoSuave,
                unfocusedSupportingTextColor = TextoSuave,
                errorSupportingTextColor = RojoError
            ),
            supportingText = when {
                error != null -> {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconoAlerta, contentDescription = null, tint = RojoError, modifier = Modifier.size(16.dp))
                            Text(error, style = chico)
                        }
                    }
                }
                ayuda != null -> { { Text(ayuda, style = chico) } }
                else -> null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = teclado,
                imeAction = if (ultimo) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = { onListo() }),
            visualTransformation = if (esContrasena && !verContrasena) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = { verContrasena = !verContrasena }) {
                        Icon(
                            if (verContrasena) Iconos.OjoTachado else Iconos.Ojo,
                            contentDescription = if (verContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else null,
            modifier = modifier.fillMaxWidth()
        )
    }
}

/**
 * Pantalla de formulario del rediseño (iniciar sesión, crear cuenta, tu nombre):
 * bloque con el frasco, etiqueta del rol, título, campos, mensaje, botón y pie.
 * Al tocar un campo, la pantalla baja sola hasta que se ve el botón y el pie.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormularioConFrasco(
    etiquetaRegresar: String,
    onRegresar: () -> Unit,
    chipIcono: ImageVector,
    chipTexto: String,
    tituloAntes: String,
    tituloAcento: String,
    subtitulo: String,
    boton: String,
    onBoton: () -> Unit,
    ocupado: Boolean = false,
    mensaje: String? = null,
    mensajeEsError: Boolean = true,
    mostrarCifrado: Boolean = true,
    // true: al tocar un campo baja hasta el botón (pantallas cortas como Iniciar sesión).
    // false: solo sube lo necesario para ver el campo (formularios largos como Crear cuenta).
    bajarAlBoton: Boolean = true,
    pie: (@Composable () -> Unit)? = null,
    campos: @Composable ColumnScope.(alTocar: Modifier) -> Unit
) {
    val verAbajo = remember { BringIntoViewRequester() }
    var toques by remember { mutableIntStateOf(0) }
    LaunchedEffect(toques) {
        if (toques > 0) {
            delay(350)   // espera a que el teclado termine de subir
            verAbajo.bringIntoView()
        }
    }
    val alTocar = if (bajarAlBoton) Modifier.onFocusChanged { if (it.isFocused) toques++ } else Modifier

    ConPoppins {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                // Primero el espacio del teclado y luego el scroll: así lo que escribes sube y se ve
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            BloqueFrasco(etiquetaRegresar, onRegresar)
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipRol(chipIcono, chipTexto)
                    TituloConAcento(tituloAntes, tituloAcento)
                    Text(subtitulo, fontSize = 16.sp, lineHeight = 24.sp, color = TextoSuave)
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { campos(alTocar) }
                // Todo el bloque de abajo (mensaje, botón y pie) es lo que se hace visible al escribir
                Column(
                    modifier = Modifier.bringIntoViewRequester(verAbajo),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mensaje?.let { MensajeEnLinea(it, mensajeEsError) }
                    BotonOscuro(boton, ocupado = ocupado, onClick = onBoton)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pie?.invoke()
                        if (mostrarCifrado) FilaCifrada()
                    }
                }
            }
        }
    }
}
