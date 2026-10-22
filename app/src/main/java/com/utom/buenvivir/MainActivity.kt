package com.utom.buenvivir

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.utom.buenvivir.alarmas.Notificaciones
import com.utom.buenvivir.data.Nube
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.PacienteApp
import com.utom.buenvivir.ui.theme.BuenVivirTheme

class MainActivity : ComponentActivity() {

    // Se pone en true cuando abren la app desde el botón "No me la tomé" de la notificación
    private var abrirMotivo by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Íconos oscuros en la barra de estado, porque el fondo siempre es claro
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        leer(intent)
        // Entra a Firebase como usuario anónimo (la primera vez crea su cuenta)
        Nube.entrar(Repositorio.nombrePaciente)
        setContent {
            BuenVivirTheme {
                PacienteApp(
                    abrirMotivo = abrirMotivo,
                    onMotivoAbierto = { abrirMotivo = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leer(intent)
    }

    private fun leer(intent: Intent?) {
        if (intent?.getBooleanExtra(Notificaciones.EXTRA_ABRIR_MOTIVO, false) == true) {
            abrirMotivo = true
            intent.removeExtra(Notificaciones.EXTRA_ABRIR_MOTIVO)
        }
    }
}
