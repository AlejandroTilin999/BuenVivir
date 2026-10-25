package com.utom.buenvivir.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.utom.buenvivir.data.Cuenta
import com.utom.buenvivir.data.RepositorioLocal
import com.utom.buenvivir.data.SesionStore
import com.utom.buenvivir.trabajos.Trabajos
import com.utom.buenvivir.ui.medico.AccesoMedico
import com.utom.buenvivir.ui.medico.MedicoApp
import com.utom.buenvivir.ui.screens.RolElegido
import com.utom.buenvivir.ui.screens.RolScreen
import com.utom.buenvivir.ui.screens.NombreScreen
import com.utom.buenvivir.ui.theme.Blanco
import com.utom.buenvivir.data.Nube
import com.utom.buenvivir.data.Resultado
import com.utom.buenvivir.ui.components.rememberLector
import com.utom.buenvivir.ui.medico.AvisoMedico
import com.utom.buenvivir.ui.screens.AvisoPrivacidadScreen
import com.utom.buenvivir.ui.screens.Consentimiento
import com.utom.buenvivir.ui.screens.RecuperarPacienteScreen
import com.utom.buenvivir.ui.screens.TEXTO_AVISO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CARGANDO = "…"

/**
 * Decide qué pantallas abrir según el rol guardado en este celular:
 * sin rol -> Elegir rol; paciente -> PacienteApp; médico o familiar -> MedicoApp.
 */
@Composable
fun Raiz(abrirMotivo: Boolean, onMotivoAbierto: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sesion = remember { SesionStore(context) }
    val rol by sesion.rol.collectAsState(initial = CARGANDO)
    val nombre by sesion.nombre.collectAsState(initial = CARGANDO)
    var revisado by remember { mutableStateOf(false) }
    // Médico o familiar que todavía no inicia sesión (los dos usan correo y contraseña)
    var entrandoComo by rememberSaveable { mutableStateOf<String?>(null) }
    // Aviso de privacidad: se acepta una vez, la primera vez que se abre la app
    var aceptoAviso by remember { mutableStateOf(Consentimiento.aceptado(context)) }
    val lector = rememberLector()
    // Paciente que cambió de celular: entra con el correo de su respaldo
    var recuperando by rememberSaveable { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var avisoRecuperar by remember { mutableStateOf<AvisoMedico?>(null) }

    // Celulares que ya se usaban como paciente antes de que existieran los roles:
    // se quedan como paciente sin preguntar (y sin perder su cuenta).
    LaunchedEffect(Unit) {
        if (sesion.rol.first() == null &&
            (RepositorioLocal.get(context).tieneDatos() || Cuenta.esPacienteAnonimo())
        ) {
            sesion.guardarRol(SesionStore.PACIENTE)
        }
        // El celular de la demostración ya era de Don José antes de que se pidiera el nombre
        if (sesion.rol.first() == SesionStore.PACIENTE && sesion.nombre.first() == null &&
            RepositorioLocal.get(context).tieneDatos()
        ) {
            sesion.guardarNombre("Don José")
        }
        revisado = true
    }

    fun guardar(nuevo: String?) = scope.launch { sesion.guardarRol(nuevo) }

    when {
        !revisado || rol == CARGANDO || nombre == CARGANDO -> Box(Modifier.fillMaxSize().background(Blanco))

        !aceptoAviso -> AvisoPrivacidadScreen(
            onAceptar = { lector.callar(); Consentimiento.aceptar(context); aceptoAviso = true },
            onEscuchar = { lector.leer(TEXTO_AVISO) }
        )

        // Paciente nuevo: primero escribe su nombre
        rol == SesionStore.PACIENTE && nombre == null -> NombreScreen(
            onListo = { scope.launch { sesion.guardarNombre(it) } }
        )

        rol == SesionStore.PACIENTE -> PacienteApp(
            nombre = nombre!!,
            onCambiarNombre = { scope.launch { sesion.guardarNombre(it) } },
            onDejarDeSerPaciente = {
                Trabajos.cancelarTodos(context)
                scope.launch { sesion.olvidar() }
            },
            abrirMotivo = abrirMotivo,
            onMotivoAbierto = onMotivoAbierto
        )

        // El familiar usa las mismas pantallas del médico (ver tomas, vincular, agregar medicinas)
        rol == SesionStore.MEDICO || rol == SesionStore.FAMILIAR -> MedicoApp(
            rol = rol!!,
            onSalir = {
                Trabajos.cancelarTodos(context)
                Cuenta.salir()
                entrandoComo = null
                guardar(null)
            }
        )

        entrandoComo != null -> AccesoMedico(
            rol = entrandoComo!!,
            onEntro = { guardar(entrandoComo) },
            onCambiarRol = { entrandoComo = null }
        )

        recuperando -> RecuperarPacienteScreen(
            ocupado = ocupado,
            aviso = avisoRecuperar,
            onRecuperar = { correo, contrasena ->
                if (!ocupado) scope.launch {
                    ocupado = true
                    avisoRecuperar = null
                    val (r, nombreGuardado) = Nube.recuperarPaciente(correo, contrasena)
                    when (r) {
                        Resultado.Bien -> {
                            // Sus medicinas, su código y su médico llegan solos desde Firebase
                            sesion.guardarNombre(nombreGuardado?.takeIf { it.isNotBlank() } ?: "Paciente")
                            sesion.guardarRol(SesionStore.PACIENTE)
                            recuperando = false
                        }
                        is Resultado.Mal -> avisoRecuperar = AvisoMedico(r.mensaje, esError = true)
                    }
                    ocupado = false
                }
            },
            onRegresar = { avisoRecuperar = null; recuperando = false }
        )

        else -> RolScreen(onYaTenia = { recuperando = true }) { elegido ->
            when (elegido) {
                RolElegido.Paciente -> guardar(SesionStore.PACIENTE)
                RolElegido.Medico -> entrandoComo = SesionStore.MEDICO
                RolElegido.Familiar -> entrandoComo = SesionStore.FAMILIAR
            }
        }
    }
}
