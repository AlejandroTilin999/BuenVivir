package com.utom.buenvivir.ui.medico

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.CatalogoMedicinas
import com.utom.buenvivir.data.Forma
import com.utom.buenvivir.data.HorarioNube
import com.utom.buenvivir.data.MedicinaCatalogo
import com.utom.buenvivir.data.MedicinaNube
import com.utom.buenvivir.data.Recetas
import com.utom.buenvivir.data.SesionStore
import com.utom.buenvivir.data.horaTexto
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*

private val CANTIDADES_TABLETA = listOf("½ tableta", "1 tableta", "2 tabletas")
private const val OTRA = "Otra"
private val MOMENTOS = listOf("antes de comer", "con la comida", "después de comer")

/**
 * M7 · Agregar medicina. El médico elige la medicina del catálogo (o la escribe),
 * cuánto toma, cuándo y a qué horas. Al guardar le llega al paciente con sus alarmas.
 * Nada viene elegido de antemano: la dosis siempre la decide el médico.
 */
@Composable
fun RecetaScreen(
    paciente: String,
    medico: String,
    rol: String = SesionStore.MEDICO,
    ocupado: Boolean,
    aviso: AvisoMedico?,
    onRegresar: () -> Unit,
    onGuardar: (MedicinaNube) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var elegida by remember { mutableStateOf<MedicinaCatalogo?>(null) }
    var aMano by remember { mutableStateOf(false) }          // "No está en la lista"
    var para by remember { mutableStateOf("") }
    var cambiandoPara by remember { mutableStateOf(false) }
    var presentacion by remember { mutableStateOf<String?>(null) }
    var cantidad by remember { mutableStateOf<String?>(null) }
    var otraCantidad by remember { mutableStateOf("") }
    var momento by remember { mutableStateOf<String?>(null) }
    var horarios by remember { mutableStateOf(listOf<Int>()) }
    var eligiendoHora by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nombre = elegida?.nombre ?: if (aMano) busqueda.trim() else ""
    val esInyeccion = elegida?.forma == Forma.INYECCION
    val cuanto = when {
        esInyeccion || aMano || cantidad == OTRA -> otraCantidad.trim()
        else -> cantidad.orEmpty()
    }.let { c -> if (esInyeccion && c.isNotEmpty() && c.all { it.isDigit() }) "$c unidades" else c }
    val cantidadCompleta = if (cuanto.isNotEmpty() && presentacion != null && !esInyeccion) "$cuanto de $presentacion" else cuanto

    fun elegir(m: MedicinaCatalogo) {
        elegida = m
        aMano = false
        busqueda = m.nombre
        para = m.para
        presentacion = m.presentaciones.singleOrNull()
        cantidad = null
        otraCantidad = ""
    }

    fun guardar() {
        error = when {
            nombre.isEmpty() -> "Elige o escribe la medicina."
            cuanto.isEmpty() -> "Indica cuánto toma cada vez."
            momento == null -> "Indica si es antes, con o después de comer."
            horarios.isEmpty() -> "Agrega al menos una hora."
            else -> null
        }
        if (error != null) return
        val cuando = momento!!
        onGuardar(
            MedicinaNube(
                id = Recetas.nuevoId(),
                nombre = nombre,
                para = para.trim(),
                cantidad = cantidadCompleta,
                indicacion = cuando,
                horarios = horarios.sorted().mapIndexed { i, m -> HorarioNube("h$i", m, cuando) },
                agregadaPor = rol,
                quien = medico
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Regresar(paciente, onRegresar)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Agregar medicina",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text("Le llega a $paciente en cuanto la guardes.", fontSize = 17.sp, lineHeight = 24.sp, color = TextoSecundario)
        }

        // ---- Medicina: buscar en el catálogo ----
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo(
                etiqueta = "Medicina",
                valor = busqueda,
                onCambio = {
                    busqueda = it
                    if (elegida != null && it != elegida?.nombre) { elegida = null; presentacion = null; para = "" }
                },
                ayuda = if (aMano) "Escribe el nombre como lo dirías al paciente." else null
            )
            val resultados = if (elegida == null && !aMano) CatalogoMedicinas.buscar(busqueda) else emptyList()
            if (elegida == null && !aMano && busqueda.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, AzulBoton, RoundedCornerShape(16.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    resultados.forEach { m ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { elegir(m) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(m.nombre, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
                            Text(m.presentaciones.joinToString(" · "), fontSize = 15.sp, color = TextoSecundario)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { aMano = true; para = "" }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Iconos.Mas, contentDescription = null, tint = TextoChip, modifier = Modifier.size(18.dp))
                        Text("No está en la lista, escribirla", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextoChip)
                    }
                }
            }
        }

        if (nombre.isNotEmpty()) {
            // ---- Para qué sirve ----
            if (elegida != null && !cambiandoPara) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Lavanda)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Para qué sirve", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoLavanda)
                        Text(
                            "Cambiar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoChip,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { cambiandoPara = true }
                                .padding(6.dp)
                        )
                    }
                    Text(para, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextoOscuro)
                    Text("Así se lo explica la app al paciente.", fontSize = 14.sp, color = TextoLavanda)
                }
            } else {
                Campo(
                    etiqueta = "Para qué sirve",
                    valor = para,
                    onCambio = { para = it },
                    ayuda = "Ejemplo: Para controlar el azúcar. Así se lo explica la app al paciente."
                )
            }

            // ---- Presentación ----
            val presentaciones = elegida?.presentaciones.orEmpty()
            if (presentaciones.isNotEmpty()) {
                Opciones("Presentación", presentaciones, presentacion) { presentacion = it }
            }

            // ---- Cuánto toma cada vez ----
            if (esInyeccion) {
                Campo(
                    etiqueta = "Cuántas unidades cada vez",
                    valor = otraCantidad,
                    onCambio = { otraCantidad = it },
                    teclado = KeyboardType.Number
                )
            } else if (aMano) {
                Campo(
                    etiqueta = "Cuánto toma cada vez",
                    valor = otraCantidad,
                    onCambio = { otraCantidad = it },
                    ayuda = "Ejemplo: 1 tableta de 500 mg"
                )
            } else {
                Opciones("Cuánto toma cada vez", CANTIDADES_TABLETA + OTRA, cantidad) { cantidad = it }
                if (cantidad == OTRA) {
                    Campo(etiqueta = "Escribe cuánto", valor = otraCantidad, onCambio = { otraCantidad = it }, ayuda = "Ejemplo: 3 tabletas")
                }
            }

            // ---- Cuándo ----
            Opciones("Cuándo", MOMENTOS.map { it.replaceFirstChar(Char::uppercase) }, momento?.replaceFirstChar(Char::uppercase)) {
                momento = it.lowercase()
            }

            // ---- Horarios ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Horarios", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextoGris)
                FilaFlexible {
                    horarios.sorted().forEach { m ->
                        Row(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Lavanda)
                                .padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(horaTexto(m), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoChip)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(role = Role.Button) { horarios = horarios - m },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Iconos.Tache, contentDescription = "Quitar ${horaTexto(m)}", tint = TextoChip, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .border(2.dp, AzulBoton, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button) { eligiendoHora = true }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Iconos.Mas, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(18.dp))
                    Text("Agregar hora", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AzulBoton)
                }
            }

            // ---- Así lo verá el paciente ----
            if (cuanto.isNotEmpty() && momento != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Subrayado, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Así lo verá $paciente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoLavanda)
                    Text(
                        buildAnnotatedString {
                            append("$nombre: toma ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(cantidadCompleta) }
                            append(" $momento.")
                        },
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        color = TextoNormal
                    )
                }
            }

            elegida?.let {
                Text(it.referencia, fontSize = 14.sp, lineHeight = 20.sp, color = TextoSecundario)
            }
        }

        (error ?: aviso?.texto)?.let { Aviso(AvisoMedico(it, esError = true)) }

        BotonPrincipal("Guardar y enviar", ocupado = ocupado, onClick = ::guardar)
    }

    if (eligiendoHora) {
        ElegirHora(
            onCancelar = { eligiendoHora = false },
            onListo = { m ->
                eligiendoHora = false
                if (m !in horarios) horarios = horarios + m
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElegirHora(onCancelar: () -> Unit, onListo: (Int) -> Unit) {
    val estado = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿A qué hora?", fontWeight = FontWeight.ExtraBold) },
        text = {
            TimeInput(
                state = estado,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = Lavanda,
                    timeSelectorSelectedContentColor = TextoOscuro,
                    periodSelectorSelectedContainerColor = Lavanda,
                    periodSelectorSelectedContentColor = TextoOscuro
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onListo(estado.hour * 60 + estado.minute) }) {
                Text("Agregar", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AzulBoton)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", fontSize = 17.sp, color = TextoGris) }
        },
        containerColor = Blanco
    )
}

/** Un grupo de opciones en forma de píldora; solo se puede elegir una. */
@Composable
private fun Opciones(titulo: String, opciones: List<String>, elegida: String?, onElegir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextoGris)
        FilaFlexible {
            opciones.forEach { o ->
                val activa = o == elegida
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .border(2.dp, if (activa) AzulBoton else BordeTarjeta, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (activa) AzulBoton else Blanco)
                        .clickable(role = Role.RadioButton) { onElegir(o) }
                        .semantics { selected = activa }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(o, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (activa) Blanco else TextoChip)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaFlexible(contenido: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { contenido() }
}

@Preview(name = "M7 · Agregar medicina", widthDp = 360, heightDp = 1400, showBackground = true)
@Composable
private fun PreviewReceta() = BuenVivirTheme {
    RecetaScreen("Don José", "Dra. Méndez", ocupado = false, aviso = null, onRegresar = {}, onGuardar = {})
}
