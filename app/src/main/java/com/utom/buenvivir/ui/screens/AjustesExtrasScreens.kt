package com.utom.buenvivir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.MiembroEquipo
import com.utom.buenvivir.ui.components.BotonEscuchar
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.medico.BotonPrincipal
import com.utom.buenvivir.ui.medico.Regresar
import com.utom.buenvivir.ui.theme.*

/*
 * Pantallas que se abren desde Ajustes: Tono, Mi médico, Privacidad, Ayuda y Acerca de.
 * Todas tienen "‹ Ajustes" arriba, un título grande y el botón para escuchar.
 */

@Composable
internal fun PantallaAjuste(
    titulo: String,
    onRegresar: () -> Unit,
    onEscuchar: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Regresar("Ajustes", onRegresar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                titulo,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            BotonEscuchar("Escuchar esta pantalla", LavandaChip, onEscuchar)
        }
        contenido()
    }
}

/** Un bloque de texto con subtítulo dentro de un recuadro, para Privacidad, Ayuda y Acerca de. */
@Composable
internal fun Bloque(subtitulo: String, texto: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, BordeTarjeta, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Blanco)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(subtitulo, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
        Text(texto, fontSize = 18.sp, lineHeight = 26.sp, color = TextoGris)
    }
}

/** Texto de explicación en un recuadro azul claro. */
@Composable
internal fun Nota(texto: String) {
    Text(
        texto,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = TextoOscuro,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Lavanda)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    )
}

// ---------- Tono ----------

@Composable
fun TonoScreen(
    elegido: String,
    opciones: List<String>,
    onElegir: (String) -> Unit,
    onRegresar: () -> Unit,
    onEscuchar: () -> Unit
) {
    PantallaAjuste("Tono", onRegresar, onEscuchar) {
        Nota("Toca un tono para escucharlo. El que elijas es el que sonará cuando toque tu pastilla.")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opciones.forEach { o ->
                val activo = o == elegido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, if (activo) AzulBoton else BordeTarjeta, RoundedCornerShape(20.dp))
                        .background(if (activo) FondoSeleccion else Blanco)
                        .clickable(role = Role.RadioButton) { onElegir(o) }
                        .semantics { selected = activo }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, if (activo) AzulBoton else BordeRadio, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activo) Box(Modifier.size(14.dp).clip(CircleShape).background(AzulBoton))
                    }
                    Text(o, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextoNormal, modifier = Modifier.weight(1f))
                    Icon(Iconos.Bocina, contentDescription = null, tint = TextoChip, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ---------- Mi médico ----------

@Composable
fun MiMedicoScreen(
    equipo: List<MiembroEquipo>,
    onVerCodigo: () -> Unit,
    onRegresar: () -> Unit,
    onEscuchar: () -> Unit
) {
    val medicos = equipo.filter { it.esMedico }
    PantallaAjuste(if (medicos.size > 1) "Mis médicos" else "Mi médico", onRegresar, onEscuchar) {
        if (medicos.isEmpty()) {
            Nota("Todavía no tienes médico en la app. Dale tu código para que pueda ver tus tomas y agregarte tus medicinas.")
            BotonPrincipal("Ver mi código", onClick = onVerCodigo)
        } else {
            medicos.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Lavanda),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Iconos.Estetoscopio, contentDescription = null, tint = TextoChip, modifier = Modifier.size(28.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(m.nombre, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
                        if (m.detalle.isNotBlank()) Text(m.detalle, fontSize = 17.sp, color = TextoSecundario)
                    }
                }
            }
            Nota("Tu médico ve si tomaste tus pastillas y por qué no, y es quien te agrega tus medicinas. Para quitarlo, ve a Mi familia.")
        }
    }
}

// ---------- Privacidad ----------

const val TEXTO_PRIVACIDAD =
    "Qué se guarda. Tus medicinas, tus horarios y si te las tomas. " +
        "Quién lo ve. Solo tú y quienes agregues con tu código. Los quitas en Mi familia. " +
        "Para qué. Para recordarte tus pastillas. No se vende ni se usa para anuncios."

@Composable
fun PrivacidadScreen(onRegresar: () -> Unit, onEscuchar: () -> Unit) {
    PantallaAjuste("Privacidad", onRegresar, onEscuchar) {
        Bloque("Qué se guarda", "Tus medicinas, tus horarios y si te las tomas.")
        Bloque("Quién lo ve", "Solo tú y quienes agregues con tu código. Los quitas en Mi familia.")
        Bloque("Para qué", "Para recordarte tus pastillas. No se vende ni se usa para anuncios.")
    }
}

// ---------- Ayuda ----------

const val TEXTO_AYUDA =
    "Cuando suene la alarma, toca Ya me la tomé, o No me la tomé y elige por qué. " +
        "Si no respondes, la alarma vuelve a sonar dos veces más. " +
        "Tus medicinas te las agrega tu médico o quien te cuida. Para eso, dales tu código: Mi familia, Agregar a alguien. " +
        "También puedes agregarlas tú en Mis medicinas, escribiendo o con tu voz. " +
        "En cada pantalla puedes tocar la bocina para que la app te lea lo que dice. " +
        "Si la alarma suena muy bajito, en Ajustes cambia el volumen a Alto."

@Composable
fun AyudaScreen(onRegresar: () -> Unit, onEscuchar: () -> Unit) {
    PantallaAjuste("Ayuda", onRegresar, onEscuchar) {
        Bloque("Cuando suene la alarma", "Toca \"Ya me la tomé\", o \"No me la tomé\" y elige por qué. Si no respondes, vuelve a sonar dos veces más.")
        Bloque("¿Cómo agrego mis medicinas?", "Te las agrega tu médico o quien te cuida: dales tu código en Mi familia → Agregar a alguien. También puedes agregarlas tú en Mis medicinas, escribiendo o con tu voz.")
        Bloque("Si cambio de celular", "En Ajustes → Respaldar mi cuenta, pon un correo y una contraseña. En el celular nuevo elige \"Ya usaba BuenVivir\" y escríbelos.")
        Bloque("Si no puedo leer bien", "Toca la bocina que hay en cada pantalla y la app te lee lo que dice.")
        Bloque("Si la alarma suena bajito", "En Ajustes, en Volumen, elige \"Alto\". También puedes cambiar el tono.")
    }
}

// ---------- Acerca de ----------

const val TEXTO_ACERCA =
    "BuenVivir, versión 1.0. Una app para recordar a los adultos mayores con diabetes tipo 2 que tomen sus medicinas, " +
        "saber por qué no las toman y conectarlos con su médico y su familia. " +
        "Proyecto para la Feria Michoacana de Ciencias e Ingenierías, FEMICI 2026. " +
        "Las medicinas que puede elegir el médico vienen del Compendio Nacional de Insumos para la Salud."

@Composable
fun AcercaDeScreen(onRegresar: () -> Unit, onEscuchar: () -> Unit) {
    PantallaAjuste("Acerca de", onRegresar, onEscuchar) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.foundation.Image(
                Iconos.Logo,
                contentDescription = "Logo de BuenVivir",
                modifier = Modifier.size(96.dp)
            )
            Text("BuenVivir", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextoOscuro)
            Text("Versión 1.0", fontSize = 17.sp, color = TextoSecundario)
        }
        Bloque(
            "Para qué es",
            "Recuerda a los adultos mayores con diabetes tipo 2 que tomen sus medicinas, guarda por qué no las tomaron y los conecta con su médico y su familia."
        )
        Bloque("Proyecto", "Hecho para la Feria Michoacana de Ciencias e Ingenierías (FEMICI) 2026.")
        Bloque("Medicinas", "Las medicinas que puede elegir el médico vienen del Compendio Nacional de Insumos para la Salud.")
    }
}

// ---------- Quién usa este celular ----------

@Composable
fun CambiarRolScreen(
    nombre: String,
    ocupado: Boolean,
    error: String?,
    onConfirmar: () -> Unit,
    onRegresar: () -> Unit,
    onEscuchar: () -> Unit
) {
    PantallaAjuste("Quién usa este celular", onRegresar, onEscuchar) {
        Nota("Ahora este celular es de $nombre, como paciente.")
        Bloque(
            "Si quieres cambiarlo",
            "Para usar este celular como médico, como cuidador o para otro paciente, primero hay que borrar los datos de $nombre."
        )
        Bloque(
            "Qué se borra",
            "Sus medicinas, sus alarmas, su historial de tomas y su código, en este celular y en la nube. Su médico y quienes lo cuidan ya no lo verán. No se puede deshacer."
        )
        error?.let { com.utom.buenvivir.ui.medico.Aviso(com.utom.buenvivir.ui.medico.AvisoMedico(it, esError = true)) }
        BotonPrincipal(if (ocupado) "Borrando…" else "Borrar datos y cambiar", ocupado = ocupado, onClick = onConfirmar)
    }
}

@Preview(name = "Tono", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewTono() = BuenVivirTheme {
    TonoScreen("Alarma", listOf("Alarma", "Timbre", "Aviso"), {}, {}, {})
}

@Preview(name = "Mi médico", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewMiMedico() = BuenVivirTheme {
    MiMedicoScreen(listOf(MiembroEquipo("1", "Dra. Laura Méndez", "medico", "Centro de Salud")), {}, {}, {})
}

@Preview(name = "Privacidad", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun PreviewPrivacidad() = BuenVivirTheme { PrivacidadScreen({}, {}) }

@Preview(name = "Acerca de", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun PreviewAcerca() = BuenVivirTheme { AcercaDeScreen({}, {}) }
