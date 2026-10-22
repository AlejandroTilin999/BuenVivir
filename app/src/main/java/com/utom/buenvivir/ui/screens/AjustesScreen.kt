package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Ajustes
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.MenuInferior
import com.utom.buenvivir.ui.theme.*

/** Pantalla 7: ajustes del paciente. Se abre con el engrane de Inicio. */
@Composable
fun AjustesScreen(
    ajustes: Ajustes,
    nombre: String,
    relojNombre: String,
    relojConectado: Boolean,
    onCambiar: (Ajustes) -> Unit,
    onProbarVolumen: (Int) -> Unit = {},
    onRegresar: () -> Unit = {},
    onEscuchar: () -> Unit = {},
    onProbarRecordatorio: () -> Unit = {},
    onReiniciarDemo: () -> Unit = {},
    onAbrirOpcion: (String) -> Unit = {},
    onIrMedicinas: () -> Unit = {},
    onIrFamilia: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Regresar + título
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button, onClick = onRegresar)
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Iconos.Atras, contentDescription = null, tint = TextoChip, modifier = Modifier.size(24.dp))
                    Text("Inicio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoChip)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ajustes",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextoOscuro,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.semantics { heading() }
                    )
                    BotonEscuchar("Escuchar esta pantalla", Lavanda, onEscuchar)
                }
            }

            // ---------- Alarma ----------
            Seccion("Alarma de tus pastillas") {
                Tarjeta(arriba = 4, abajo = 16) {
                    FilaInterruptor("Sonido", ajustes.sonido) { onCambiar(ajustes.copy(sonido = it)) }
                    Divisor()
                    FilaInterruptor("Vibración", ajustes.vibracion) { onCambiar(ajustes.copy(vibracion = it)) }
                    Divisor()
                    FilaOpcion("Tono", ajustes.tono) { onAbrirOpcion("tono") }
                    Divisor()
                    // Cada toque cambia: 5 → 10 → 15 → 5 minutos
                    FilaOpcion("Repetir si no respondo", "Cada ${ajustes.repetirMinutos} min") {
                        val siguiente = when (ajustes.repetirMinutos) { 5 -> 10; 10 -> 15; else -> 5 }
                        onCambiar(ajustes.copy(repetirMinutos = siguiente))
                    }
                    Divisor()
                    Spacer(Modifier.height(14.dp))
                    Segmentos("Volumen", listOf("Bajo", "Medio", "Alto"), ajustes.volumen) {
                        onCambiar(ajustes.copy(volumen = it))
                        onProbarVolumen(it) // suena un momento para que escuche qué tan fuerte es
                    }
                }
            }

            // ---------- Reloj ----------
            Seccion("Reloj inteligente") {
                Tarjeta(arriba = 16, abajo = 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AzulClaro)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Blanco),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Iconos.RelojPulsera, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(28.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(relojNombre, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextoAzulOscuro)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (relojConectado) AzulBoton else BordeRadio)
                                )
                                Text(
                                    if (relojConectado) "Conectado" else "Sin conexión",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextoAzul
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    FilaInterruptor("Avisarme en el reloj", ajustes.avisarEnReloj) { onCambiar(ajustes.copy(avisarEnReloj = it)) }
                    Divisor()
                    FilaInterruptor("Vibrar en el reloj", ajustes.vibrarEnReloj) { onCambiar(ajustes.copy(vibrarEnReloj = it)) }
                    Divisor()
                    FilaInterruptor(
                        "Registrar desde el reloj",
                        ajustes.registrarDesdeReloj,
                        detalle = "Marca tu pastilla desde la muñeca"
                    ) { onCambiar(ajustes.copy(registrarDesdeReloj = it)) }
                }
            }

            // ---------- Pantalla y voz ----------
            Seccion("Pantalla y voz") {
                Tarjeta(arriba = 16, abajo = 16) {
                    Segmentos("Colores", listOf("Claro", "Oscuro"), if (ajustes.temaOscuro) 1 else 0) {
                        onCambiar(ajustes.copy(temaOscuro = it == 1))
                    }
                    Spacer(Modifier.height(18.dp))
                    Segmentos("Velocidad de la voz", listOf("Lenta", "Normal"), if (ajustes.vozLenta) 0 else 1) {
                        onCambiar(ajustes.copy(vozLenta = it == 0))
                    }
                }
            }

            // ---------- Demostración ----------
            Seccion("Para la demostración") {
                Tarjeta(arriba = 16, abajo = 16) {
                    BotonContorno("Probar recordatorio en 1 minuto", Iconos.Campana, onProbarRecordatorio)
                    Spacer(Modifier.height(10.dp))
                    BotonContorno("Reiniciar demostración", Iconos.Reloj, onReiniciarDemo)
                }
            }

            // ---------- Más ----------
            Seccion("Más") {
                Tarjeta(arriba = 0, abajo = 0) {
                    val opciones = listOf(
                        "Mi perfil" to nombre,
                        "Mi médico" to "",
                        "Quién usa este celular" to "Paciente",
                        "Privacidad" to "",
                        "Ayuda" to "",
                        "Acerca de BuenVivir" to ""
                    )
                    opciones.forEachIndexed { i, (texto, valor) ->
                        if (i > 0) Divisor()
                        FilaOpcion(texto, valor) { onAbrirOpcion(texto) }
                    }
                }
                Text(
                    "BuenVivir · versión 1.0",
                    fontSize = 15.sp,
                    color = TextoSecundario,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        MenuInferior(seleccionado = 0, onInicio = onRegresar, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
    }
}

// ---------- Piezas ----------

@Composable
private fun BotonContorno(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .border(2.dp, AzulBoton, RoundedCornerShape(27.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AzulBoton)
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextoOscuro,
            modifier = Modifier.semantics { heading() }
        )
        contenido()
    }
}

@Composable
private fun Tarjeta(arriba: Int, abajo: Int, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 16.dp, top = arriba.dp, bottom = abajo.dp),
        content = contenido
    )
}

@Composable
private fun Divisor() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(LavandaSuave)
    )
}

@Composable
private fun FilaInterruptor(
    texto: String,
    activo: Boolean,
    detalle: String? = null,
    onCambio: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (detalle == null) 62.dp else 76.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(texto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextoNormal)
            detalle?.let { Text(it, fontSize = 15.sp, color = TextoSecundario) }
        }
        Switch(
            checked = activo,
            onCheckedChange = null, // la fila completa es la que se toca
            colors = SwitchDefaults.colors(
                checkedThumbColor = Blanco,
                checkedTrackColor = AzulBoton,
                checkedBorderColor = AzulBoton,
                uncheckedThumbColor = BordeRadio,
                uncheckedTrackColor = Blanco,
                uncheckedBorderColor = BordeRadio
            )
        )
    }
}

@Composable
private fun FilaOpcion(texto: String, valor: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextoNormal, modifier = Modifier.weight(1f))
        if (valor.isNotEmpty()) {
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextoAzul, maxLines = 1)
        }
        Icon(Iconos.Flecha, contentDescription = null, tint = TextoAzul, modifier = Modifier.size(22.dp))
    }
}

/** Botones pegados para escoger una opción (Bajo / Medio / Alto...). */
@Composable
private fun Segmentos(titulo: String, opciones: List<String>, elegida: Int, onElegir: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextoNormal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LavandaSuave)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            opciones.forEachIndexed { i, opcion ->
                val activa = i == elegida
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activa) AzulBoton else Color.Transparent)
                        .selectable(selected = activa, role = Role.RadioButton) { onElegir(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opcion,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activa) Blanco else TextoChip
                    )
                }
            }
        }
    }
}

@Preview(name = "Ajustes", widthDp = 360, heightDp = 1700, showBackground = true)
@Composable
private fun PreviewAjustes() = BuenVivirTheme {
    AjustesScreen(
        ajustes = Repositorio.ajustesIniciales,
        nombre = Repositorio.nombrePaciente,
        relojNombre = Repositorio.relojNombre,
        relojConectado = Repositorio.relojConectado,
        onCambiar = {}
    )
}
