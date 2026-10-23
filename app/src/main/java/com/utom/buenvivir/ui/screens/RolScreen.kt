package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.medico.BotonPrincipal
import com.utom.buenvivir.ui.theme.*

enum class RolElegido { Paciente, Familiar, Medico }

/** M1: la primera vez que se abre la app en un celular. */
@Composable
fun RolScreen(onContinuar: (RolElegido) -> Unit) {
    var elegido by rememberSaveable { mutableStateOf<RolElegido?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Lavanda)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(Iconos.Logo, contentDescription = "Logo de BuenVivir", modifier = Modifier.size(96.dp))
            Text(
                "BuenVivir",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text("¿Quién va a usar esta app?", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OpcionRol("Soy paciente", "Me recuerda mis pastillas", Iconos.Persona,
                elegido == RolElegido.Paciente) { elegido = RolElegido.Paciente }
            OpcionRol("Soy familiar", "Veo si mi familiar tomó su medicina", Iconos.Familia,
                elegido == RolElegido.Familiar) { elegido = RolElegido.Familiar }
            OpcionRol("Soy médico", "Doy recetas y sigo a mis pacientes", Iconos.Estetoscopio,
                elegido == RolElegido.Medico) { elegido = RolElegido.Medico }
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            if (elegido != null) {
                BotonPrincipal("Continuar") { elegido?.let(onContinuar) }
            }
        }
    }
}

@Composable
private fun OpcionRol(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(if (seleccionado) FondoSeleccion else Blanco)
            .border(2.dp, if (seleccionado) AzulBoton else BordeTarjeta, forma)
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (seleccionado) AzulBoton else Lavanda),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = if (seleccionado) Blanco else TextoChip, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
            Text(descripcion, fontSize = 16.sp, lineHeight = 21.sp, color = TextoSecundario)
        }
        if (seleccionado) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(AzulBoton),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Blanco, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview(name = "M1 · Elegir rol", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewRol() = BuenVivirTheme { RolScreen {} }
