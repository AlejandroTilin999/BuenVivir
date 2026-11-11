package com.utom.buenvivir.ui.medico

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.CampoLimpio
import com.utom.buenvivir.ui.components.EnlaceLimpio
import com.utom.buenvivir.ui.components.FormularioConFrasco
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.TextoSuave
import com.utom.buenvivir.ui.theme.BuenVivirTheme

private const val MINIMO_CONTRASENA = 8

/**
 * M2: iniciar sesión (médico o familiar). Estilo limpio.
 * Si falta algo, el campo se pone rojo y dice qué falta debajo (sin recuadros).
 */
@Composable
fun LoginScreen(
    esFamiliar: Boolean = false,
    ocupado: Boolean,
    aviso: AvisoMedico?,
    onEntrar: (correo: String, contrasena: String) -> Unit,
    onOlvide: (correo: String) -> Unit,
    onCrearCuenta: () -> Unit,
    onCambiarRol: () -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var revisar by remember { mutableStateOf(false) }          // ya tocó Entrar
    var pidioRecuperar by remember { mutableStateOf(false) }   // tocó "¿Olvidaste...?" sin correo

    val errorCorreo = when {
        correo.isNotBlank() -> null
        pidioRecuperar -> "Escribe tu correo para recuperar tu contraseña"
        revisar -> "Escribe tu correo"
        else -> null
    }
    val errorClave = if (revisar && contrasena.isBlank()) "Escribe tu contraseña" else null

    val entrar: () -> Unit = {
        revisar = true
        pidioRecuperar = false
        if (correo.isNotBlank() && contrasena.isNotBlank()) onEntrar(correo.trim(), contrasena)
    }
    val olvide: () -> Unit = {
        pidioRecuperar = true
        if (correo.isNotBlank()) onOlvide(correo.trim())
    }

    FormularioConFrasco(
        etiquetaRegresar = "Cambiar rol",
        onRegresar = onCambiarRol,
        chipIcono = if (esFamiliar) Iconos.Familia else Iconos.Estetoscopio,
        chipTexto = if (esFamiliar) "Cuidador" else "Médico",
        tituloAntes = "Hola de ",
        tituloAcento = "nuevo",
        subtitulo = if (esFamiliar) "Entra para ver cómo va la persona que cuidas." else "Entra para ver a tus pacientes.",
        boton = "Entrar",
        onBoton = entrar,
        ocupado = ocupado,
        mensaje = aviso?.texto,
        mensajeEsError = aviso?.esError ?: true,
        pie = { PreguntaConEnlace("¿No tienes cuenta?", "Crear cuenta", onCrearCuenta) }
    ) { alTocar ->
        CampoLimpio("Correo electrónico", correo, { correo = it }, teclado = KeyboardType.Email,
            error = errorCorreo, modifier = alTocar)
        Column {
            CampoLimpio("Contraseña", contrasena, { contrasena = it }, teclado = KeyboardType.Password,
                esContrasena = true, ultimo = true, onListo = entrar, error = errorClave, modifier = alTocar)
            EnlaceLimpio("¿Olvidaste tu contraseña?", olvide, Modifier.align(Alignment.End))
        }
    }
}

/** M3: crear la cuenta. El médico pone cédula y dónde da consulta; el familiar, qué es del paciente. */
@Composable
fun RegistroScreen(
    esFamiliar: Boolean = false,
    ocupado: Boolean,
    aviso: AvisoMedico?,
    onCrear: (nombre: String, cedula: String, lugar: String, correo: String, contrasena: String) -> Unit,
    onIniciarSesion: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var cedula by rememberSaveable { mutableStateOf("") }
    var lugar by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var revisar by remember { mutableStateOf(false) }          // ya tocó Crear cuenta

    // Lo mismo que pide Firebase: nombre, correo y contraseña de 8 o más
    val errorNombre = if (revisar && nombre.isBlank()) "Escribe tu nombre" else null
    val errorCorreo = if (revisar && correo.isBlank()) "Escribe tu correo" else null
    val errorClave = when {
        !revisar -> null
        contrasena.isBlank() -> "Escribe una contraseña"
        contrasena.length < MINIMO_CONTRASENA -> "Debe tener mínimo $MINIMO_CONTRASENA caracteres"
        else -> null
    }

    val crear: () -> Unit = {
        revisar = true
        if (nombre.isNotBlank() && correo.isNotBlank() && contrasena.length >= MINIMO_CONTRASENA) {
            onCrear(nombre.trim(), cedula.trim(), lugar.trim(), correo.trim(), contrasena)
        }
    }

    FormularioConFrasco(
        etiquetaRegresar = "Regresar a iniciar sesión",
        onRegresar = onIniciarSesion,
        chipIcono = if (esFamiliar) Iconos.Familia else Iconos.Estetoscopio,
        chipTexto = if (esFamiliar) "Cuidador" else "Médico",
        tituloAntes = "Crea tu ",
        tituloAcento = "cuenta",
        subtitulo = if (esFamiliar) "Así te verá la persona que cuidas." else "Así te verán tus pacientes.",
        boton = "Crear cuenta",
        onBoton = crear,
        ocupado = ocupado,
        mensaje = aviso?.texto,
        mensajeEsError = aviso?.esError ?: true,
        bajarAlBoton = false,   // son 4 o 5 campos: solo sube lo necesario para ver el que escribes
        pie = { PreguntaConEnlace("¿Ya tienes cuenta?", "Inicia sesión", onIniciarSesion) }
    ) { alTocar ->
        CampoLimpio("Nombre completo", nombre, { nombre = it }, error = errorNombre, modifier = alTocar)
        if (esFamiliar) {
            // Para el familiar, "lugar" guarda qué es del paciente
            CampoLimpio("¿Qué eres de la persona que cuidas?", lugar, { lugar = it },
                ayuda = "Por ejemplo: Hija, Nieto, Enfermera", modifier = alTocar)
        } else {
            CampoLimpio("Cédula profesional", cedula, { cedula = it }, teclado = KeyboardType.Number, modifier = alTocar)
            CampoLimpio("Dónde das consulta", lugar, { lugar = it },
                ayuda = "Por ejemplo: Centro de Salud de Maravatío", modifier = alTocar)
        }
        CampoLimpio("Correo electrónico", correo, { correo = it }, teclado = KeyboardType.Email,
            error = errorCorreo, modifier = alTocar)
        CampoLimpio("Contraseña", contrasena, { contrasena = it }, teclado = KeyboardType.Password,
            esContrasena = true, ultimo = true, onListo = crear,
            ayuda = "Mínimo $MINIMO_CONTRASENA caracteres", error = errorClave, modifier = alTocar)
    }
}

/** "¿No tienes cuenta? Crear cuenta" */
@Composable
private fun PreguntaConEnlace(pregunta: String, enlace: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(pregunta, fontSize = 16.sp, lineHeight = 24.sp, color = TextoSuave)
        EnlaceLimpio(enlace, onClick, negrita = true)
    }
}

@Preview(name = "M2 · Iniciar sesión", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewLogin() = BuenVivirTheme {
    LoginScreen(ocupado = false, aviso = AvisoMedico("El correo o la contraseña no son correctos.", true),
        onEntrar = { _, _ -> }, onOlvide = {}, onCrearCuenta = {}, onCambiarRol = {})
}

@Preview(name = "M3 · Crear cuenta", widthDp = 360, heightDp = 1100, showBackground = true)
@Composable
private fun PreviewRegistro() = BuenVivirTheme {
    RegistroScreen(ocupado = false, aviso = null, onCrear = { _, _, _, _, _ -> }, onIniciarSesion = {})
}

@Preview(name = "M3 · Crear cuenta (cuidador)", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun PreviewRegistroFamiliar() = BuenVivirTheme {
    RegistroScreen(esFamiliar = true, ocupado = false, aviso = null, onCrear = { _, _, _, _, _ -> }, onIniciarSesion = {})
}
