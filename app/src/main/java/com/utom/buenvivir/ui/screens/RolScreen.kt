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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.components.*
import com.utom.buenvivir.ui.theme.BuenVivirTheme

enum class RolElegido { Paciente, Familiar, Medico }

/** M1: la primera vez que se abre la app en un celular. Estilo limpio. */
@Composable
fun RolScreen(onYaTenia: () -> Unit = {}, onContinuar: (RolElegido) -> Unit) {
    var elegido by rememberSaveable { mutableStateOf<RolElegido?>(null) }

    ConPoppins {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Aquí irá la foto de la persona adulta mayor; por ahora, el logo
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(184.dp)
                            .clip(CircleShape)
                            .background(FondoLavandaSuave),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(Iconos.Logo, contentDescription = "Logo de BuenVivir", modifier = Modifier.size(112.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TituloConAcento("¿Quién va a usar ", "BuenVivir", "?", centrado = true)
                    Text(
                        "Elige una opción para empezar.",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = TextoSuave,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpcionRol("Soy paciente", "Me recuerda mis pastillas", Iconos.Persona,
                        elegido == RolElegido.Paciente) { elegido = RolElegido.Paciente }
                    OpcionRol("Soy cuidador", "Veo si tomó su medicina", Iconos.Familia,
                        elegido == RolElegido.Familiar) { elegido = RolElegido.Familiar }
                    OpcionRol("Soy médico", "Sigo a mis pacientes", Iconos.Estetoscopio,
                        elegido == RolElegido.Medico) { elegido = RolElegido.Medico }
                }
            }

            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                BotonOscuro("Continuar", habilitado = elegido != null) { elegido?.let(onContinuar) }
                // Paciente que cambió de celular y respaldó su cuenta
                EnlaceLimpio("Ya usaba BuenVivir en otro celular", onYaTenia, Modifier.fillMaxWidth())
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
    val forma = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(forma)
            .background(if (seleccionado) SeleccionLavanda else Color.White)
            .border(if (seleccionado) 2.dp else 1.dp, if (seleccionado) AcentoMorado else BordeSuave, forma)
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconoEnCirculo(
            icono,
            fondo = if (seleccionado) AcentoMorado else ChipLavanda,
            tinte = if (seleccionado) Color.White else AzulNoche
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = AzulNoche)
            Text(descripcion, fontSize = 16.sp, lineHeight = 24.sp, color = TextoSuave)
        }
        if (seleccionado) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AcentoMorado),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview(name = "M1 · Elegir rol", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewRol() = BuenVivirTheme { RolScreen {} }
