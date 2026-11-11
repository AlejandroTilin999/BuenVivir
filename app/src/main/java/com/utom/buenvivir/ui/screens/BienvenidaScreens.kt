package com.utom.buenvivir.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.utom.buenvivir.ui.components.AzulNoche
import com.utom.buenvivir.ui.components.BotonContorno
import com.utom.buenvivir.ui.components.BotonOscuro
import com.utom.buenvivir.ui.components.ConPoppins
import com.utom.buenvivir.ui.components.FondoLavandaSuave
import com.utom.buenvivir.ui.components.IconoEnCirculo
import com.utom.buenvivir.ui.components.TextoSuave
import com.utom.buenvivir.ui.components.TituloConAcento
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.medico.Aviso
import com.utom.buenvivir.ui.medico.AvisoMedico
import com.utom.buenvivir.ui.medico.BotonPrincipal
import com.utom.buenvivir.ui.medico.Campo
import com.utom.buenvivir.ui.medico.Regresar
import com.utom.buenvivir.ui.theme.*

// ---------- Aviso de privacidad (la primera vez que se abre la app) ----------

/** Recuerda si ya aceptó el aviso de privacidad en este celular. */
object Consentimiento {
    private const val VERSION = 1   // si el aviso cambia, se sube y se vuelve a pedir

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("consentimiento", Context.MODE_PRIVATE)

    fun aceptado(context: Context) = prefs(context).getInt("aviso", 0) >= VERSION

    fun aceptar(context: Context) = prefs(context).edit().putInt("aviso", VERSION).apply()
}

const val TEXTO_AVISO =
    "Antes de empezar. BuenVivir guarda tus medicinas y si te las tomas. " +
        "Solo lo ven tú y las personas que agregues con tu código. " +
        "No se vende ni se usa para anuncios. Más detalles en Ajustes, Privacidad."

@Composable
fun AvisoPrivacidadScreen(onAceptar: () -> Unit, onEscuchar: () -> Unit) {
    ConPoppins {
        Column(modifier = Modifier.fillMaxSize().background(Blanco)) {
            // Bloque lavanda con el logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(FondoLavandaSuave)
                    .statusBarsPadding()
                    .height(192.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(Blanco),
                    contentAlignment = Alignment.Center
                ) {
                    Image(Iconos.Logo, contentDescription = "Logo de BuenVivir", modifier = Modifier.size(80.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TituloConAcento("Antes de ", "empezar")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PuntoAviso(Iconos.Pastilla, "BuenVivir guarda tus medicinas y si te las tomas.")
                    PuntoAviso(Iconos.Familia, "Solo lo ven tú y las personas que agregues con tu código.")
                    PuntoAviso(Iconos.Candado, "No se vende ni se usa para anuncios.")
                }
                Text("Más detalles en Ajustes → Privacidad.", fontSize = 16.sp, lineHeight = 24.sp, color = TextoSuave)
                BotonContorno("Escuchar el aviso", Iconos.Bocina, onEscuchar)
            }

            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                BotonOscuro("Acepto", onClick = onAceptar)
            }
        }
    }
}

@Composable
private fun PuntoAviso(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconoEnCirculo(icono)
        Text(texto, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = AzulNoche)
    }
}

// ---------- Respaldar la cuenta (Ajustes → Respaldar mi cuenta) ----------

const val TEXTO_RESPALDO =
    "Respaldar mi cuenta. Si cambias de celular o se borra la app, con un correo y una contraseña recuperas " +
        "tus medicinas, tu código y a tu médico. Escribe un correo y una contraseña de mínimo 8 letras o números, y toca Respaldar."

@Composable
fun RespaldoScreen(
    correoActual: String?,          // si ya está respaldada, con qué correo
    ocupado: Boolean,
    aviso: AvisoMedico?,
    onRespaldar: (correo: String, contrasena: String) -> Unit,
    onRegresar: () -> Unit,
    onEscuchar: () -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }

    PantallaAjuste("Respaldar mi cuenta", onRegresar, onEscuchar) {
        if (correoActual != null) {
            Nota("Tu cuenta ya está respaldada con $correoActual.")
            Bloque("Si cambias de celular", "Instala BuenVivir, toca \"Ya usaba BuenVivir\" y escribe ese correo y tu contraseña.")
        } else {
            Nota("Si cambias de celular o se borra la app, con este correo y contraseña recuperas tus medicinas, tu código y a tu médico.")
            Campo(etiqueta = "Correo", valor = correo, onCambio = { correo = it }, teclado = KeyboardType.Email)
            Campo(
                etiqueta = "Contraseña",
                valor = contrasena,
                onCambio = { contrasena = it },
                ayuda = "Mínimo 8 letras o números. Anótala en un lugar seguro.",
                esContrasena = true,
                ultimo = true,
                onListo = { onRespaldar(correo, contrasena) }
            )
            aviso?.let { Aviso(it) }
            BotonPrincipal("Respaldar", ocupado = ocupado) { onRespaldar(correo, contrasena) }
        }
    }
}

// ---------- Recuperar la cuenta en un celular nuevo ----------

@Composable
fun RecuperarPacienteScreen(
    ocupado: Boolean,
    aviso: AvisoMedico?,
    onRecuperar: (correo: String, contrasena: String) -> Unit,
    onRegresar: () -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Regresar("Regresar", onRegresar)
        Text(
            "Ya usaba BuenVivir",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            "Escribe el correo y la contraseña con los que respaldaste tu cuenta. Regresarán tus medicinas, tu código y tu médico.",
            fontSize = 18.sp, lineHeight = 25.sp, color = TextoSecundario
        )
        Campo(etiqueta = "Correo", valor = correo, onCambio = { correo = it }, teclado = KeyboardType.Email)
        Campo(
            etiqueta = "Contraseña",
            valor = contrasena,
            onCambio = { contrasena = it },
            esContrasena = true,
            ultimo = true,
            onListo = { onRecuperar(correo, contrasena) }
        )
        aviso?.let { Aviso(it) }
        BotonPrincipal("Recuperar mi cuenta", ocupado = ocupado) { onRecuperar(correo, contrasena) }
    }
}

@Preview(name = "Aviso de privacidad", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewAviso() = BuenVivirTheme { AvisoPrivacidadScreen({}, {}) }

@Preview(name = "Respaldar", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewRespaldo() = BuenVivirTheme { RespaldoScreen(null, false, null, { _, _ -> }, {}, {}) }
