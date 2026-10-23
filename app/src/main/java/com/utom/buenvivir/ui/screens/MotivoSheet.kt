package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.EnlaceSecundario
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*

/** Hoja flotante para elegir por qué no se tomó la pastilla. */
@Composable
fun MotivoSheet(
    motivos: List<String>,
    onEscuchar: () -> Unit,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var elegido by rememberSaveable { mutableStateOf<Int?>(null) }
    val forma = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(10.dp)
            .shadow(24.dp, forma)
            .clip(forma)
            .background(Blanco)
            // Evita que un toque dentro de la hoja la cierre
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(48.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AsaHoja)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "¿Por qué no te la tomaste?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 29.sp,
                    color = TextoOscuro,
                    modifier = Modifier.semantics { heading() }
                )
                Text("Tu familia y tu médico lo verán.", fontSize = 16.sp, color = TextoSecundario)
            }
            BotonEscuchar("Escuchar las opciones", LavandaChip, onEscuchar)
        }

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            motivos.forEachIndexed { i, motivo ->
                OpcionMotivo(texto = motivo, seleccionada = elegido == i) { elegido = i }
            }
        }

        Column {
            Button(
                onClick = { elegido?.let { onGuardar(motivos[it]) } },
                enabled = elegido != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulBoton,
                    contentColor = Blanco,
                    disabledContainerColor = AzulBoton.copy(alpha = 0.35f),
                    disabledContentColor = Blanco
                ),
                elevation = null
            ) {
                Text("Guardar motivo", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            EnlaceSecundario("Cancelar", onCancelar)
        }
    }
}

@Composable
private fun OpcionMotivo(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(forma)
            .border(
                width = if (seleccionada) 2.5.dp else 2.dp,
                color = if (seleccionada) TextoChip else Lavanda,
                shape = forma
            )
            .background(if (seleccionada) FondoSeleccion else Blanco)
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (seleccionada) Modifier.background(TextoChip)
                    else Modifier.border(2.5.dp, BordeRadio, CircleShape).background(Blanco)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionada) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Blanco, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            texto,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (seleccionada) TextoOscuro else TextoNormal
        )
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFF8A8BA0)
@Composable
private fun MotivoPreview() = BuenVivirTheme {
    MotivoSheet(Repositorio.motivos, onEscuchar = {}, onGuardar = {}, onCancelar = {})
}
