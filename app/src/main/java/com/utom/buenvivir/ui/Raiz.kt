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
import com.utom.buenvivir.ui.medico.AccesoMedico
import com.utom.buenvivir.ui.medico.MedicoApp
import com.utom.buenvivir.ui.screens.RolElegido
import com.utom.buenvivir.ui.screens.RolScreen
import com.utom.buenvivir.ui.theme.Blanco
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val CARGANDO = "…"

/**
 * Decide qué pantallas abrir según el rol guardado en este celular:
 * sin rol -> Elegir rol; paciente -> PacienteApp; médico -> MedicoApp.
 */
@Composable
fun Raiz(abrirMotivo: Boolean, onMotivoAbierto: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sesion = remember { SesionStore(context) }
    val rol by sesion.rol.collectAsState(initial = CARGANDO)
    var revisado by remember { mutableStateOf(false) }
    var entrandoMedico by rememberSaveable { mutableStateOf(false) }

    // Celulares que ya se usaban como paciente antes de que existieran los roles:
    // se quedan como paciente sin preguntar (y sin perder su cuenta).
    LaunchedEffect(Unit) {
        if (sesion.rol.first() == null &&
            (RepositorioLocal.get(context).tieneDatos() || Cuenta.esPacienteAnonimo())
        ) {
            sesion.guardarRol(SesionStore.PACIENTE)
        }
        revisado = true
    }

    fun guardar(nuevo: String?) = scope.launch { sesion.guardarRol(nuevo) }

    when {
        !revisado || rol == CARGANDO -> Box(Modifier.fillMaxSize().background(Blanco))

        rol == SesionStore.PACIENTE -> PacienteApp(abrirMotivo, onMotivoAbierto)

        rol == SesionStore.MEDICO -> MedicoApp(
            onSalir = {
                Cuenta.salir()
                entrandoMedico = false
                guardar(null)
            }
        )

        entrandoMedico -> AccesoMedico(
            onEntro = { guardar(SesionStore.MEDICO) },
            onCambiarRol = { entrandoMedico = false }
        )

        else -> RolScreen { elegido ->
            when (elegido) {
                RolElegido.Paciente -> guardar(SesionStore.PACIENTE)
                RolElegido.Medico -> entrandoMedico = true
                RolElegido.Familiar ->
                    Toast.makeText(context, "El rol de familiar llega pronto", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
