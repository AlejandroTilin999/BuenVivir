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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.Espacio
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.Letra
import com.utom.buenvivir.ui.components.Medida
import com.utom.buenvivir.ui.components.Tono
import com.utom.buenvivir.ui.theme.*

/**
 * Hoja que sube desde abajo para elegir por qué no se tomó la pastilla.
 * Primero elige un motivo y luego toca Guardar (así un toque por error no guarda nada).
 */
@Composable
fun MotivoSheet(
    motivos: List<String>,
    onEscuchar: () -> Unit,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var elegido by rememberSaveable { mutableStateOf<Int?>(null) }
    val forma = RoundedCornerShape(topStart = Espacio.XL, topEnd = Espacio.XL)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Tono.Blanco)
            // Evita que un toque dentro de la hoja la cierre
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = Espacio.M, end = Espacio.M, top = Espacio.S, bottom = Espacio.L),
        verticalArrangement = Arrangement.spacedBy(Espacio.M)
    ) {
        // Asa
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(Espacio.XL)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Tono.BordeCampo)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "¿Por qué no te la tomaste?",
                    style = Letra.Subtitulo,
                    color = Tono.Noche,
                    modifier = Modifier.semantics { heading() }
                )
                Text("Así tu médico sabe cómo ayudarte.", style = Letra.Texto, color = Tono.Suave)
            }
            Box(
                modifier = Modifier
                    .size(Medida.Tactil)
                    .clip(CircleShape)
                    .background(Tono.Chip)
                    .clickable(role = Role.Button, onClick = onEscuchar),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Bocina, contentDescription = "Escuchar las opciones", tint = Tono.Acento, modifier = Modifier.size(24.dp))
            }
        }

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Espacio.S)
        ) {
            motivos.forEachIndexed { i, motivo ->
                OpcionMotivo(texto = motivo, seleccionada = elegido == i) { elegido = i }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
            Button(
                onClick = { elegido?.let { onGuardar(motivos[it]) } },
                enabled = elegido != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Medida.Boton),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Tono.Noche,
                    contentColor = Tono.Blanco,
                    disabledContainerColor = Tono.Gris,
                    disabledContentColor = Tono.Suave
                ),
                elevation = null
            ) {
                Text("Guardar motivo", style = Letra.Boton)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Medida.Tactil)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onCancelar),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancelar", style = Letra.Texto.copy(fontWeight = FontWeight.SemiBold), color = Tono.Acento)
            }
        }
    }
}

/** Una opción de motivo: 56 de alto, con círculo que se marca al elegirla. */
@Composable
private fun OpcionMotivo(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(Medida.RadioCaja)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Medida.Boton)
            .clip(forma)
            .border(if (seleccionada) 2.dp else 1.dp, if (seleccionada) Tono.Acento else Tono.BordeCampo, forma)
            .background(if (seleccionada) Tono.Chip else Tono.Blanco)
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Espacio.M, vertical = Espacio.S),
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (seleccionada) Modifier.background(Tono.Acento)
                    else Modifier.border(2.dp, Tono.BordeCampo, CircleShape).background(Tono.Blanco)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionada) {
                Icon(Iconos.Palomita, contentDescription = null, tint = Tono.Blanco, modifier = Modifier.size(16.dp))
            }
        }
        Text(texto, style = Letra.Texto.copy(fontWeight = FontWeight.SemiBold), color = Tono.Noche)
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFF8A8BA0)
@Composable
private fun MotivoPreview() = BuenVivirTheme {
    MotivoSheet(Repositorio.motivos, onEscuchar = {}, onGuardar = {}, onCancelar = {})
}
