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
import com.utom.buenvivir.data.PacienteVinculado
import com.utom.buenvivir.data.PerfilMedico
import com.utom.buenvivir.data.Vinculos
import com.utom.buenvivir.data.Recetas
import com.utom.buenvivir.data.SesionStore
import com.utom.buenvivir.data.Resultado
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Antes de entrar: iniciar sesión (M2) o crear cuenta (M3). Sirve para médico y familiar. */
@Composable
fun AccesoMedico(rol: String = SesionStore.MEDICO, onEntro: () -> Unit, onCambiarRol: () -> Unit) {
    val esFamiliar = rol == SesionStore.FAMILIAR
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
            esFamiliar = esFamiliar,
            ocupado = ocupado,
            aviso = aviso,
            onCrear = { nombre, cedula, lugar, correo, contrasena ->
                trabajar {
                    val r = if (esFamiliar) {
                        Cuenta.crear(nombre, "", "", correo, contrasena, rol = rol, parentesco = lugar)
                    } else {
                        Cuenta.crear(nombre, cedula, lugar, correo, contrasena, rol = rol)
                    }
                    when (r) {
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
            esFamiliar = esFamiliar,
            ocupado = ocupado,
            aviso = aviso,
            onEntrar = { correo, contrasena ->
                trabajar {
                    when (val r = Cuenta.entrar(correo, contrasena, rol)) {
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

/** Ya dentro: Mis pacientes (M4), Vincular (M5), Detalle (M6) y Mi perfil. */
@Composable
fun MedicoApp(rol: String = SesionStore.MEDICO, onSalir: () -> Unit) {
    CompositionLocalProvider(LocalEsCuidador provides (rol == SesionStore.FAMILIAR)) {
        PantallasMedico(onSalir)
    }
}

@Composable
private fun PantallasMedico(onSalir: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pestana by rememberSaveable { mutableIntStateOf(0) } // 0 Pacientes, 1 Mi perfil
    var vinculando by rememberSaveable { mutableStateOf(false) }
    var abierto by remember { mutableStateOf<PacienteVinculado?>(null) }
    var recetando by remember { mutableStateOf(false) }       // M7 abierta para el paciente abierto
    var guardando by remember { mutableStateOf(false) }
    var avisoReceta by remember { mutableStateOf<AvisoMedico?>(null) }
    var perfil by remember { mutableStateOf<PerfilMedico?>(null) }
    val pacientes by remember { Vinculos.misPacientes() }.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        // Si la sesión se cerró por fuera (por ejemplo, se borró la cuenta), regresa a elegir rol
        if (!Cuenta.tieneSesion()) {
            onSalir()
            return@LaunchedEffect
        }
        perfil = Cuenta.miPerfil()
    }

    // Atrás: del detalle o de Vincular a la lista; de Mi perfil a Pacientes
    BackHandler(enabled = abierto != null || vinculando || pestana != 0) {
        when {
            recetando -> { recetando = false; avisoReceta = null }
            abierto != null -> abierto = null
            vinculando -> vinculando = false
            else -> pestana = 0
        }
    }

    abierto?.let { p ->
        if (recetando) {
            RecetaScreen(
                paciente = p.nombre,
                medico = perfil?.nombre ?: "",
                rol = perfil?.rol ?: SesionStore.MEDICO,
                ocupado = guardando,
                aviso = avisoReceta,
                onRegresar = { recetando = false; avisoReceta = null },
                onGuardar = { m ->
                    if (!guardando) scope.launch {
                        guardando = true
                        avisoReceta = null
                        when (val r = Recetas.guardar(p.id, m)) {
                            Resultado.Bien -> {
                                recetando = false
                                Toast.makeText(context, "Listo, ${m.nombre} se le envió a ${p.nombre}", Toast.LENGTH_SHORT).show()
                            }
                            is Resultado.Mal -> avisoReceta = AvisoMedico(r.mensaje, esError = true)
                        }
                        guardando = false
                    }
                }
            )
            return
        }
        DetallePacienteScreen(
            p = p,
            onRegresar = { abierto = null },
            onAgregarMedicina = { recetando = true },
            onQuitar = {
                scope.launch {
                    when (val r = Vinculos.desvincular(p.id)) {
                        Resultado.Bien -> {
                            abierto = null
                            Toast.makeText(context, "Ya no ves a ${p.nombre}", Toast.LENGTH_SHORT).show()
                        }
                        is Resultado.Mal -> Toast.makeText(context, r.mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        return
    }

    if (vinculando) {
        VincularScreen(
            onRegresar = { vinculando = false },
            onVinculado = { nombre ->
                vinculando = false
                Toast.makeText(context, "Listo, ya ves a $nombre", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

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
                    pacientes = pacientes,
                    onVincular = { vinculando = true },
                    onAbrir = { abierto = it }
                )
            }
        }
        MenuMedico(seleccionado = pestana, onPacientes = { pestana = 0 }, onPerfil = { pestana = 1 })
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
            if (perfil?.esFamiliar == true) {
                Dato("Qué eres de la persona que cuidas", perfil?.parentesco)
            } else {
                Dato("Cédula profesional", perfil?.cedula)
                Dato("Dónde das consulta", perfil?.lugar)
            }
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

@Preview(name = "Mi perfil (médico)", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewPerfil() = BuenVivirTheme {
    PerfilMedicoScreen(PerfilMedico("Laura Méndez", "", "Centro de Salud", "laura@correo.com", false)) {}
}
