package com.utom.buenvivir.ui.medico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*

/** Un aviso debajo de los campos: error (gris oscuro) o información (lavanda). */
data class AvisoMedico(val texto: String, val esError: Boolean)

/** Campo de texto con su etiqueta arriba, como en el diseño. */
@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    esContrasena: Boolean = false,
    ultimo: Boolean = false,
    onListo: () -> Unit = {}
) {
    var verContrasena by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(etiqueta, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextoGris)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = true,
            textStyle = TextStyle(fontSize = 18.sp, color = TextoNormal),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzulBoton,
                unfocusedBorderColor = BordeTarjeta,
                focusedContainerColor = Blanco,
                unfocusedContainerColor = Blanco,
                cursorColor = AzulBoton
            ),
            visualTransformation = if (esContrasena && !verContrasena) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = teclado,
                imeAction = if (ultimo) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = { onListo() }),
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = { verContrasena = !verContrasena }) {
                        Icon(
                            if (verContrasena) Iconos.OjoTachado else Iconos.Ojo,
                            contentDescription = if (verContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = TextoChip,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = etiqueta }
        )
        ayuda?.let { Text(it, fontSize = 15.sp, color = TextoSecundario) }
    }
}

/** Botón azul grande en forma de píldora; muestra un círculo girando mientras trabaja. */
@Composable
fun BotonPrincipal(texto: String, ocupado: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !ocupado,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(8.dp, RoundedCornerShape(32.dp), ambientColor = AzulBoton, spotColor = AzulBoton),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AzulBoton,
            contentColor = Blanco,
            disabledContainerColor = AzulBoton,
            disabledContentColor = Blanco
        ),
        elevation = null
    ) {
        if (ocupado) {
            CircularProgressIndicator(color = Blanco, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        } else {
            Text(texto, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun Aviso(aviso: AvisoMedico) {
    Text(
        aviso.texto,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp,
        color = if (aviso.esError) Blanco else TextoMorado,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (aviso.esError) GrisAviso else MoradoAviso)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/** "‹ Cambiar rol", "‹ Iniciar sesión"... */
@Composable
fun Regresar(texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Iconos.Atras, contentDescription = null, tint = TextoChip, modifier = Modifier.size(24.dp))
        Text(texto, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoChip)
    }
}

/** Menú de abajo del médico: Pacientes y Mi perfil. */
@Composable
fun MenuMedico(seleccionado: Int, onPacientes: () -> Unit, onPerfil: () -> Unit) {
    Column(modifier = Modifier.background(Blanco)) {
        HorizontalDivider(thickness = 1.5.dp, color = LineaMenu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 18.dp)
        ) {
            ElementoMenuMedico("Pacientes", Iconos.Familia, seleccionado == 0, onPacientes, Modifier.weight(1f))
            ElementoMenuMedico("Mi perfil", Iconos.Persona, seleccionado == 1, onPerfil, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ElementoMenuMedico(
    texto: String,
    icono: ImageVector,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val color = if (activo) AzulBoton else TextoSecundario
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = activo }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(texto, fontSize = 16.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.SemiBold, color = color)
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (activo) AzulBoton else Blanco)
        )
    }
}
