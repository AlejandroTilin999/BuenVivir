package com.utom.buenvivir.ui.medico

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Cuenta
import com.utom.buenvivir.data.PerfilMedico
import com.utom.buenvivir.data.Resultado
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Antes de entrar: iniciar sesión (M2) o crear cuenta (M3). */
@Composable
fun AccesoMedico(onEntro: () -> Unit, onCambiarRol: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var registrando by rememberSaveable { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<AvisoMedico?>(null) }

    fun trabajar(trabajo: suspend () -> Unit) {
        if (ocupado) return
        scope.launch {
            ocupado = true
            aviso = null
            trabajo()
            ocupado = false
        }
    }

    BackHandler {
        if (registrando) registrando = false else onCambiarRol()
        aviso = null
    }

    if (registrando) {
        RegistroScreen(
            ocupado = ocupado,
            aviso = aviso,
            onCrear = { nombre, cedula, lugar, correo, contrasena ->
                trabajar {
                    when (val r = Cuenta.crear(nombre, cedula, lugar, correo, contrasena)) {
                        Resultado.Bien -> {
                            Toast.makeText(context, "Te mandamos un correo para confirmar tu cuenta", Toast.LENGTH_LONG).show()
                            onEntro()
                        }
                        is Resultado.Mal -> aviso = AvisoMedico(r.mensaje, esError = true)
                    }
                }
            },
            onIniciarSesion = { registrando = false; aviso = null }
        )
    } else {
        LoginScreen(
            ocupado = ocupado,
            aviso = aviso,
            onEntrar = { correo, contrasena ->
                trabajar {
                    when (val r = Cuenta.entrar(correo, contrasena)) {
                        Resultado.Bien -> onEntro()
                        is Resultado.Mal -> aviso = AvisoMedico(r.mensaje, esError = true)
                    }
                }
            },
            onOlvide = { correo ->
                trabajar {
                    aviso = when (val r = Cuenta.recuperar(correo)) {
                        Resultado.Bien -> AvisoMedico("Te mandamos un correo para cambiar tu contraseña.", esError = false)
                        is Resultado.Mal -> AvisoMedico(r.mensaje, esError = true)
                    }
                }
            },
            onCrearCuenta = { registrando = true; aviso = null },
            onCambiarRol = onCambiarRol
        )
    }
}

/** Ya dentro: Mis pacientes (M4) y Mi perfil. */
@Composable
fun MedicoApp(onSalir: () -> Unit) {
    val context = LocalContext.current
    var pestana by rememberSaveable { mutableIntStateOf(0) } // 0 Pacientes, 1 Mi perfil
    var perfil by remember { mutableStateOf<PerfilMedico?>(null) }

    LaunchedEffect(Unit) {
        // Si la sesión se cerró por fuera (por ejemplo, se borró la cuenta), regresa a elegir rol
        if (!Cuenta.tieneSesion()) {
            onSalir()
            return@LaunchedEffect
        }
        perfil = Cuenta.miPerfil()
    }

    BackHandler(enabled = pestana != 0) { pestana = 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (pestana) {
                1 -> PerfilMedicoScreen(perfil, onSalir)
                else -> PacientesScreen(
                    nombre = perfil?.nombre ?: "",
                    onVincular = {
                        // Paso 2: aquí se abre "Vincular paciente"
                        Toast.makeText(context, "Vincular paciente llega en el siguiente paso", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        MenuMedico(seleccionado = pestana, onPacientes = { pestana = 0 }, onPerfil = { pestana = 1 })
    }
}

/** M4: por ahora sin pacientes; la lista llega con "Vincular". */
@Composable
fun PacientesScreen(nombre: String, onVincular: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Lavanda)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(saludo(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
            Text(
                nombre,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Lavanda),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Iconos.Familia, contentDescription = null, tint = TextoChip, modifier = Modifier.size(32.dp))
                }
                Text("Todavía no tienes pacientes", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
                Text(
                    "Pídele a tu paciente el código que aparece en su app, en Mi familia → Agregar a alguien.",
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = TextoSecundario
                )
            }
            BotonPrincipal("Vincular paciente", onClick = onVincular)
        }
    }
}

/** Mi perfil: sus datos y el botón para cerrar sesión. */
@Composable
fun PerfilMedicoScreen(perfil: PerfilMedico?, onSalir: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            "Mi perfil",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.semantics { heading() }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Dato("Nombre", perfil?.nombre)
            Dato("Cédula profesional", perfil?.cedula)
            Dato("Dónde das consulta", perfil?.lugar)
            Dato("Correo", perfil?.correo, ultimo = true)
        }
        if (perfil != null && !perfil.correoConfirmado) {
            Aviso(AvisoMedico("Todavía no confirmas tu correo. Revisa tu bandeja de entrada.", esError = false))
        }
        OutlinedButton(
            onClick = onSalir,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, AzulBoton),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulBoton)
        ) {
            Text("Cerrar sesión", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String?, ultimo: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(etiqueta, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario)
        Text(if (valor.isNullOrBlank()) "—" else valor, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoNormal)
    }
    if (!ultimo) Box(Modifier.fillMaxWidth().height(1.5.dp).background(LavandaSuave))
}

private fun saludo(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Buenos días,"
    in 12..18 -> "Buenas tardes,"
    else -> "Buenas noches,"
}

@Preview(name = "M4 · Mis pacientes (vacío)", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewPacientes() = BuenVivirTheme {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { PacientesScreen("Laura Méndez") {} }
        MenuMedico(0, {}, {})
    }
}

@Preview(name = "Mi perfil (médico)", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewPerfil() = BuenVivirTheme {
    PerfilMedicoScreen(PerfilMedico("Laura Méndez", "", "Centro de Salud", "laura@correo.com", false)) {}
}
