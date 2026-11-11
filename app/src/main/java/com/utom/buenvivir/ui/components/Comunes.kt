package com.utom.buenvivir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.ui.theme.*

/** Botón redondo de bocina para leer en voz alta. */
@Composable
fun BotonEscuchar(descripcion: String, fondo: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(fondo)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Iconos.Bocina, contentDescription = descripcion, tint = AzulBoton, modifier = Modifier.size(24.dp))
    }
}

/** Enlace subrayado en lavanda ("No me la tomé", "Cancelar"...). */
@Composable
fun EnlaceSecundario(texto: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(texto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextoGris)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Subrayado)
            )
        }
    }
}

/** Menú de abajo del paciente, estilo Android actual: píldora azul clara detrás del ícono activo. */
@Composable
fun MenuInferior(
    seleccionado: Int,
    onInicio: () -> Unit,
    onMedicinas: () -> Unit,
    onFamilia: () -> Unit
) {
    Column(modifier = Modifier.background(Blanco)) {
        HorizontalDivider(thickness = 1.dp, color = BordeSuave)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            ElementoMenu("Inicio", Iconos.Casa, seleccionado == 0, onInicio, Modifier.weight(1f))
            ElementoMenu("Mis medicinas", Iconos.Pastilla, seleccionado == 1, onMedicinas, Modifier.weight(1f))
            ElementoMenu("Mi familia", Iconos.Familia, seleccionado == 2, onFamilia, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ElementoMenu(
    texto: String,
    icono: ImageVector,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = activo }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (activo) FondoLavandaSuave else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = if (activo) AcentoMorado else TextoSuave, modifier = Modifier.size(24.dp))
        }
        Text(
            texto,
            fontFamily = Poppins,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            color = if (activo) AzulNoche else TextoSuave,
            maxLines = 1,
            softWrap = false
        )
    }
}
