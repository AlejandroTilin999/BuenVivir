package com.utom.buenvivir.ui.medico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.EnlaceSecundario
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*

/** M2: iniciar sesión (médico o familiar). */
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

    Pantalla {
        Regresar("Cambiar rol", onCambiarRol)
        Encabezado(
            esFamiliar,
            "Inicia sesión",
            if (esFamiliar) "Con tu cuenta ves cómo va la persona que cuidas, desde cualquier celular."
            else "Con tu cuenta ves a tus pacientes desde cualquier celular."
        )
        Campo("Correo", correo, { correo = it }, teclado = KeyboardType.Email)
        Column {
            Campo("Contraseña", contrasena, { contrasena = it }, teclado = KeyboardType.Password,
                esContrasena = true, ultimo = true, onListo = { onEntrar(correo, contrasena) })
            EnlaceSecundario("¿Olvidaste tu contraseña?") { onOlvide(correo) }
        }
        aviso?.let { Aviso(it) }
        Spacer(Modifier.height(8.dp))
        BotonPrincipal("Entrar", ocupado) { onEntrar(correo, contrasena) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿No tienes cuenta? ", fontSize = 17.sp, color = TextoSecundario)
            Text(
                "Crear cuenta",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextoChip,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onCrearCuenta)
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            )
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
    val crear = { onCrear(nombre, cedula, lugar, correo, contrasena) }

    Pantalla {
        Regresar("Iniciar sesión", onIniciarSesion)
        if (esFamiliar) {
            Encabezado(true, "Crea tu cuenta", "La persona que cuidas verá tu nombre y qué eres de él o ella.")
            Campo("Nombre completo", nombre, { nombre = it })
            // Para el familiar, "lugar" guarda qué es del paciente
            Campo("¿Qué eres de la persona que cuidas?", lugar, { lugar = it }, ayuda = "Por ejemplo: Hija, Nieto, Enfermera")
        } else {
            Encabezado(false, "Crea tu cuenta", "Tus pacientes verán tu nombre y dónde das consulta.")
            Campo("Nombre completo", nombre, { nombre = it })
            Campo("Cédula profesional", cedula, { cedula = it }, teclado = KeyboardType.Number)
            Campo("Dónde das consulta", lugar, { lugar = it }, ayuda = "Por ejemplo: Centro de Salud de Maravatío")
        }
        Campo("Correo", correo, { correo = it }, teclado = KeyboardType.Email)
        Campo("Contraseña", contrasena, { contrasena = it }, ayuda = "Mínimo 8 caracteres",
            teclado = KeyboardType.Password, esContrasena = true, ultimo = true, onListo = crear)
        aviso?.let { Aviso(it) }
        Spacer(Modifier.height(8.dp))
        BotonPrincipal("Crear cuenta", ocupado, crear)
    }
}

/** Fondo blanco con scroll, márgenes y espacio entre bloques, igual en M2 y M3. */
@Composable
private fun Pantalla(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = contenido
    )
}

@Composable
private fun Encabezado(esFamiliar: Boolean, titulo: String, texto: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Lavanda)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(if (esFamiliar) Iconos.Familia else Iconos.Estetoscopio, contentDescription = null, tint = TextoChip, modifier = Modifier.size(16.dp))
            Text(if (esFamiliar) "Cuidador" else "Médico", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoChip)
        }
        Text(
            titulo,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro,
            letterSpacing = (-0.5).sp,
            modifier = Modifier
                .padding(top = 6.dp)
                .semantics { heading() }
        )
        Text(texto, fontSize = 17.sp, lineHeight = 24.sp, color = TextoSecundario)
    }
}

@Preview(name = "M2 · Iniciar sesión", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewLogin() = BuenVivirTheme {
    LoginScreen(ocupado = false, aviso = AvisoMedico("El correo o la contraseña no son correctos.", true),
        onEntrar = { _, _ -> }, onOlvide = {}, onCrearCuenta = {}, onCambiarRol = {})
}

@Preview(name = "M3 · Crear cuenta", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun PreviewRegistro() = BuenVivirTheme {
    RegistroScreen(ocupado = false, aviso = null, onCrear = { _, _, _, _, _ -> }, onIniciarSesion = {})
}

@Preview(name = "M3 · Crear cuenta (cuidador)", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun PreviewRegistroFamiliar() = BuenVivirTheme {
    RegistroScreen(esFamiliar = true, ocupado = false, aviso = null, onCrear = { _, _, _, _, _ -> }, onIniciarSesion = {})
}
