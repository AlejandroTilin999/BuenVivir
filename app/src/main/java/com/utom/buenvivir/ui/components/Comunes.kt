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

@Composable
fun MenuInferior(
    seleccionado: Int,
    onInicio: () -> Unit,
    onMedicinas: () -> Unit,
    onFamilia: () -> Unit
) {
    Column(modifier = Modifier.background(Blanco)) {
        HorizontalDivider(thickness = 1.5.dp, color = LineaMenu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 18.dp)
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
        Text(
            texto,
            fontSize = 16.sp,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (activo) AzulBoton else Color.Transparent)
        )
    }
}
