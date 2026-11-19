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
import com.utom.buenvivir.ui.components.*
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
    onProbarDemo: (() -> Unit)? = null,  // carga la Metformina si hace falta y suena en 10 s (celular y reloj)
    onQuitarDemo: (() -> Unit)? = null,  // quita la Metformina de ejemplo
    mostrarDemo: Boolean = true,      // "Para la demostración" solo en el celular de la feria
    puedeProbar: Boolean = true,      // sin medicinas no hay qué probar
    onAbrirOpcion: (String) -> Unit = {},
    respaldada: Boolean = false,
    onIrMedicinas: () -> Unit = {},
    onIrFamilia: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tono.Blanco)
    ) {
        SisEncabezado("Ajustes", onEscuchar = onEscuchar, regresar = "Inicio" to onRegresar)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.M, vertical = Espacio.L),
            verticalArrangement = Arrangement.spacedBy(Espacio.XL)
        ) {
            // ---------- Demostración rápida (hasta arriba, para la feria) ----------
            if (onProbarDemo != null) SisSeccion("Para la demostración") {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                    SisBoton("Probar: suena en 10 segundos", onProbarDemo, icono = Iconos.Campana)
                    if (onQuitarDemo != null) SisBoton("Quitar medicina de ejemplo", onQuitarDemo, contorno = true)
                }
            }

            // ---------- Alarma ----------
            SisSeccion("Alarma de tus pastillas") {
                Tarjeta {
                    FilaInterruptor("Sonido", ajustes.sonido) { onCambiar(ajustes.copy(sonido = it)) }
                    SisSeparador()
                    FilaInterruptor("Vibración", ajustes.vibracion) { onCambiar(ajustes.copy(vibracion = it)) }
                    SisSeparador()
                    FilaOpcion("Tono", com.utom.buenvivir.alarmas.Tonos.valido(ajustes.tono)) { onAbrirOpcion("tono") }
                    SisSeparador()
                    // Cada toque cambia: 5 → 10 → 15 → 5 minutos
                    FilaOpcion("Repetir si no respondo", "Cada ${ajustes.repetirMinutos} min") {
                        val siguiente = when (ajustes.repetirMinutos) { 5 -> 10; 10 -> 15; else -> 5 }
                        onCambiar(ajustes.copy(repetirMinutos = siguiente))
                    }
                    SisSeparador()
                    Segmentos("Volumen", listOf("Bajo", "Medio", "Alto"), ajustes.volumen) {
                        onCambiar(ajustes.copy(volumen = it))
                        onProbarVolumen(it) // suena un momento para que escuche qué tan fuerte es
                    }
                }
            }

            // ---------- Reloj ----------
            SisSeccion("Reloj inteligente") {
                Tarjeta {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Medida.Fila)
                            .semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SisIconoLista(Iconos.RelojPulsera)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(relojNombre, style = Letra.TextoFuerte, color = Tono.Noche)
                            Text("Reloj inteligente", style = Letra.Etiqueta, color = Tono.Suave)
                        }
                        Text(
                            if (relojConectado) "Conectado" else "Sin conexión",
                            style = Letra.EtiquetaFuerte,
                            color = if (relojConectado) Tono.Acento else Tono.Suave
                        )
                    }
                    SisSeparador()
                    FilaInterruptor("Avisarme en el reloj", ajustes.avisarEnReloj) { onCambiar(ajustes.copy(avisarEnReloj = it)) }
                    SisSeparador()
                    FilaInterruptor("Vibrar en el reloj", ajustes.vibrarEnReloj) { onCambiar(ajustes.copy(vibrarEnReloj = it)) }
                    SisSeparador()
                    FilaInterruptor("Sonar en el reloj", ajustes.sonarEnReloj) { onCambiar(ajustes.copy(sonarEnReloj = it)) }
                }
            }

            // ---------- Pantalla y voz ----------
            SisSeccion("Pantalla y voz") {
                Tarjeta(vertical = Espacio.M, espacio = Espacio.L) {
                    Segmentos("Colores", listOf("Claro", "Oscuro"), if (ajustes.temaOscuro) 1 else 0) {
                        onCambiar(ajustes.copy(temaOscuro = it == 1))
                    }
                    Segmentos("Velocidad de la voz", listOf("Lenta", "Normal"), if (ajustes.vozLenta) 0 else 1) {
                        onCambiar(ajustes.copy(vozLenta = it == 0))
                    }
                }
            }

            // ---------- Demostración ----------
            // "Probar recordatorio" le sirve a cualquiera; "Reiniciar" solo en el celular de la feria
            if (puedeProbar || mostrarDemo) SisSeccion(if (mostrarDemo) "Para la demostración" else "Probar") {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
                    if (puedeProbar) SisBoton("Probar recordatorio en 10 segundos", onProbarRecordatorio, icono = Iconos.Campana, contorno = true)
                    if (mostrarDemo) SisBoton("Reiniciar demostración", onReiniciarDemo, icono = Iconos.Reloj, contorno = true)
                }
            }

            // ---------- Más ----------
            SisSeccion("Más") {
                Tarjeta {
                    val opciones = listOf(
                        "Mi perfil" to nombre,
                        "Mi médico" to "",
                        "Respaldar mi cuenta" to if (respaldada) "Hecho" else "",
                        "Quién usa este celular" to "Paciente",
                        "Privacidad" to "",
                        "Ayuda" to "",
                        "Acerca de BuenVivir" to ""
                    )
                    opciones.forEachIndexed { i, (texto, valor) ->
                        if (i > 0) SisSeparador()
                        FilaOpcion(texto, valor) { onAbrirOpcion(texto) }
                    }
                }
                Text(
                    "BuenVivir · versión 1.0",
                    style = Letra.Etiqueta,
                    color = Tono.Suave,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        MenuInferior(seleccionado = 0, onInicio = onRegresar, onMedicinas = onIrMedicinas, onFamilia = onIrFamilia)
    }
}

// ---------- Piezas ----------

/** Caja con borde fino; las filas van separadas por líneas. */
@Composable
private fun Tarjeta(
    vertical: androidx.compose.ui.unit.Dp = Espacio.S,
    espacio: androidx.compose.ui.unit.Dp = 0.dp,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Tono.Borde, RoundedCornerShape(Medida.RadioCaja))
            .padding(horizontal = Espacio.M, vertical = vertical),
        verticalArrangement = Arrangement.spacedBy(espacio),
        content = contenido
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
            .heightIn(min = Medida.Boton)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio)
            .padding(vertical = Espacio.S),
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(texto, style = Letra.TextoFuerte, color = Tono.Noche)
            detalle?.let { Text(it, style = Letra.Etiqueta, color = Tono.Suave) }
        }
        Switch(
            checked = activo,
            onCheckedChange = null, // la fila completa es la que se toca
            colors = SwitchDefaults.colors(
                checkedThumbColor = Tono.Blanco,
                checkedTrackColor = Tono.Acento,
                checkedBorderColor = Tono.Acento,
                uncheckedThumbColor = Tono.BordeCampo,
                uncheckedTrackColor = Tono.Blanco,
                uncheckedBorderColor = Tono.BordeCampo
            )
        )
    }
}

@Composable
private fun FilaOpcion(texto: String, valor: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Medida.Boton)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Espacio.S),
        horizontalArrangement = Arrangement.spacedBy(Espacio.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(texto, style = Letra.TextoFuerte, color = Tono.Noche)
            if (valor.isNotEmpty()) Text(valor, style = Letra.Etiqueta, color = Tono.Suave, maxLines = 1)
        }
        Icon(Iconos.Flecha, contentDescription = null, tint = Tono.Suave, modifier = Modifier.size(20.dp))
    }
}

/** Botones pegados para escoger una opción (Bajo / Medio / Alto...). */
@Composable
private fun Segmentos(titulo: String, opciones: List<String>, elegida: Int, onElegir: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = Espacio.S), verticalArrangement = Arrangement.spacedBy(Espacio.S)) {
        Text(titulo, style = Letra.TextoFuerte, color = Tono.Noche)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Tono.Gris)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.S)
        ) {
            opciones.forEachIndexed { i, opcion ->
                val activa = i == elegida
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Medida.Chip)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activa) Tono.Noche else Color.Transparent)
                        .selectable(selected = activa, role = Role.RadioButton) { onElegir(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(opcion, style = Letra.EtiquetaFuerte, color = if (activa) Tono.Blanco else Tono.Noche)
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
