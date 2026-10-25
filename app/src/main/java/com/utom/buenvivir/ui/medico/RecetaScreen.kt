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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.utom.buenvivir.data.Voz
import com.utom.buenvivir.ui.components.rememberLector
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
    inicial: MedicinaNube? = null,          // si viene, es para cambiar esa medicina
    onGuardar: (MedicinaNube) -> Unit
) {
    // Al cambiar una medicina, todo empieza con lo que ya tenía
    val ini = remember(inicial) { valoresIniciales(inicial) }
    var busqueda by remember { mutableStateOf(ini.busqueda) }
    var elegida by remember { mutableStateOf(ini.elegida) }
    var aMano by remember { mutableStateOf(ini.aMano) }          // "No está en la lista"
    var para by remember { mutableStateOf(ini.para) }
    var cambiandoPara by remember { mutableStateOf(false) }
    var presentacion by remember { mutableStateOf(ini.presentacion) }
    var cantidad by remember { mutableStateOf(ini.cantidad) }
    var otraCantidad by remember { mutableStateOf(ini.otraCantidad) }
    var momento by remember { mutableStateOf(ini.momento) }
    var horarios by remember { mutableStateOf(ini.horarios) }
    var eligiendoHora by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nombre = elegida?.nombre ?: if (aMano) busqueda.trim() else ""
    // El paciente agrega sus propias medicinas con la misma pantalla, con textos para él
    val esPaciente = rol == SesionStore.PACIENTE
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
            Recetas.conCambio(inicial, MedicinaNube(
                id = inicial?.id ?: Recetas.nuevoId(),
                nombre = nombre,
                para = para.trim(),
                cantidad = cantidadCompleta,
                indicacion = cuando,
                horarios = horarios.sorted().mapIndexed { i, m -> HorarioNube("h$i", m, cuando) },
                agregadaPor = rol,
                quien = medico
            ))
        )
    }

    // ---------- Asistente de voz (solo el paciente) ----------
    // La app pregunta en voz alta, abre el micrófono de Google y va llenando el formulario.
    val lector = if (esPaciente) rememberLector() else null
    var paso by remember { mutableStateOf<PasoVoz?>(null) }
    var fallos by remember { mutableIntStateOf(0) }

    fun resumenVoz(): String {
        val horas = horarios.sorted().joinToString(" y ") { horaTexto(it) }
        return buildString {
            append("Listo. $nombre")
            if (cantidadCompleta.isNotEmpty()) append(", $cantidadCompleta")
            momento?.let { append(", $it") }
            if (horas.isNotEmpty()) append(", a las $horas")
            append(". Revisa que esté bien y toca Guardar.")
        }
    }

    // Lo que contestó: se guarda aquí y abajo se procesa
    var respuesta by remember { mutableStateOf<Pair<PasoVoz, String>?>(null) }
    val microfono = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val oido = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        val actual = paso
        if (oido.isNullOrBlank() || actual == null) paso = null   // canceló o no dijo nada
        else respuesta = actual to oido
    }

    fun preguntar(nuevo: PasoVoz?, antes: String = "") {
        paso = nuevo
        val l = lector ?: return
        if (nuevo == null) return
        if (nuevo == PasoVoz.Listo) { paso = null; l.decir(resumenVoz()); return }
        val pregunta = antes + preguntaDe(nuevo)
        l.decirYLuego(pregunta) {
            val intento = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, preguntaDe(nuevo))
            try {
                microfono.launch(intento)
            } catch (_: ActivityNotFoundException) {
                paso = null
                error = "Este celular no tiene reconocimiento de voz. Llena los datos con el teclado."
            }
        }
    }

    // Lo que se entendió en cada paso, y cuál sigue
    fun procesar(actual: PasoVoz, oido: String) {
        var entendio = true
        val siguiente: PasoVoz = when (actual) {
            PasoVoz.Nombre -> {
                val m = CatalogoMedicinas.encontrarEnFrase(oido)
                if (m != null) elegir(m) else {
                    elegida = null; aMano = true; para = ""
                    busqueda = oido.trim().replaceFirstChar(Char::uppercase)
                }
                if (m != null && m.presentaciones.size > 1) PasoVoz.Presentacion else PasoVoz.Cantidad
            }
            PasoVoz.Presentacion -> {
                val op = elegida?.let { Voz.presentacion(oido, it.presentaciones) }
                if (op != null) presentacion = op else entendio = false
                PasoVoz.Cantidad
            }
            PasoVoz.Cantidad -> {
                val c = Voz.cantidad(oido, esInyeccion)
                if (c == null) entendio = false
                else if (esInyeccion || aMano) otraCantidad = c
                else if (c in CANTIDADES_TABLETA) { cantidad = c; otraCantidad = "" }
                else { cantidad = OTRA; otraCantidad = c }
                PasoVoz.Momento
            }
            PasoVoz.Momento -> {
                val m = Voz.momento(oido)
                if (m != null) momento = m else entendio = false
                PasoVoz.Hora
            }
            PasoVoz.Hora -> {
                val h = Voz.hora(oido)
                if (h != null) { if (h !in horarios) horarios = horarios + h } else entendio = false
                PasoVoz.OtraHora
            }
            PasoVoz.OtraHora -> when (Voz.siONo(oido)) {
                true -> PasoVoz.Hora
                false -> PasoVoz.Listo
                null -> { entendio = false; PasoVoz.Listo }
            }
            PasoVoz.Listo -> PasoVoz.Listo
        }
        // Si no le entendió, le pregunta otra vez (una sola vez); después sigue y se llena a mano
        if (!entendio && fallos == 0) {
            fallos = 1
            preguntar(actual, "No te entendí. ")
        } else {
            fallos = 0
            preguntar(siguiente)
        }
    }
    LaunchedEffect(respuesta) {
        respuesta?.let { (actual, oido) -> respuesta = null; procesar(actual, oido) }
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
                if (inicial != null) "Cambiar medicina" else "Agregar medicina",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                if (esPaciente) "Escríbela como te la indicó tu médico. Te sonará la alarma a las horas que elijas."
                else "Le llega a $paciente en cuanto la guardes.",
                fontSize = 17.sp, lineHeight = 24.sp, color = TextoSecundario
            )
        }

        if (esPaciente && inicial == null) {
            BotonVoz(
                escuchando = paso != null,
                onClick = {
                    if (paso != null) { paso = null; lector?.callar() } else { fallos = 0; preguntar(PasoVoz.Nombre) }
                }
            )
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
                ayuda = when {
                    !aMano -> null
                    esPaciente -> "Escribe el nombre como viene en la caja."
                    else -> "Escribe el nombre como lo dirías al paciente."
                }
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
                    Text(if (esPaciente) "Así te lo explica la app." else "Así se lo explica la app al paciente.", fontSize = 14.sp, color = TextoLavanda)
                }
            } else {
                Campo(
                    etiqueta = "Para qué sirve",
                    valor = para,
                    onCambio = { para = it },
                    ayuda = if (esPaciente) "Ejemplo: Para controlar el azúcar." else "Ejemplo: Para controlar el azúcar. Así se lo explica la app al paciente."
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
                    Text(if (esPaciente) "Así lo verás" else "Así lo verá $paciente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoLavanda)
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

        BotonPrincipal(
            when {
                inicial != null -> "Guardar cambios"
                esPaciente -> "Guardar"
                else -> "Guardar y enviar"
            },
            ocupado = ocupado, onClick = ::guardar
        )
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

/** Las preguntas del asistente de voz, en orden. */
private enum class PasoVoz { Nombre, Presentacion, Cantidad, Momento, Hora, OtraHora, Listo }

private fun preguntaDe(p: PasoVoz): String = when (p) {
    PasoVoz.Nombre -> "¿Cómo se llama tu medicina?"
    PasoVoz.Presentacion -> "¿De cuántos miligramos es? Lo dice la caja."
    PasoVoz.Cantidad -> "¿Cuánto tomas cada vez? Por ejemplo: una pastilla, media o dos."
    PasoVoz.Momento -> "¿La tomas antes de comer, con la comida o después de comer?"
    PasoVoz.Hora -> "¿A qué hora la tomas? Por ejemplo: a las 8 de la mañana."
    PasoVoz.OtraHora -> "¿La tomas a otra hora más? Di sí o no."
    PasoVoz.Listo -> ""
}

/** Botón grande para llenar la medicina hablando. */
@Composable
private fun BotonVoz(escuchando: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (escuchando) AzulBoton else Lavanda)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(Blanco),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Microfono, contentDescription = null, tint = AzulBoton, modifier = Modifier.size(30.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (escuchando) "Te estoy preguntando…" else "Dímela con tu voz",
                fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                color = if (escuchando) Blanco else TextoOscuro
            )
            Text(
                if (escuchando) "Toca aquí para parar." else "Te pregunto y tú me contestas.",
                fontSize = 16.sp, color = if (escuchando) Blanco else TextoLavanda
            )
        }
    }
}

/** Lo que ya tenía la medicina, acomodado en los campos de la pantalla. */
private class ValoresIniciales(
    val busqueda: String = "",
    val elegida: MedicinaCatalogo? = null,
    val aMano: Boolean = false,
    val para: String = "",
    val presentacion: String? = null,
    val cantidad: String? = null,
    val otraCantidad: String = "",
    val momento: String? = null,
    val horarios: List<Int> = emptyList()
)

private fun valoresIniciales(m: MedicinaNube?): ValoresIniciales {
    if (m == null) return ValoresIniciales()
    val cat = CatalogoMedicinas.lista.firstOrNull { it.nombre.equals(m.nombre, ignoreCase = true) }
    val momento = m.indicacion.takeIf { it in MOMENTOS }
    val horas = m.horarios.map { it.minutos }
    // "1 tableta de 850 mg" -> cuánto = "1 tableta", presentación = "850 mg"
    val partes = m.cantidad.split(" de ", limit = 2)
    val presentacion = partes.getOrNull(1)?.takeIf { p -> cat?.presentaciones?.contains(p) == true }
    val cuanto = if (presentacion != null) partes[0] else m.cantidad
    return when {
        cat == null -> ValoresIniciales(m.nombre, null, true, m.para, null, null, m.cantidad, momento, horas)
        cat.forma == Forma.INYECCION ->
            ValoresIniciales(m.nombre, cat, false, m.para, presentacion ?: cat.presentaciones.singleOrNull(), null, cuanto.removeSuffix(" unidades"), momento, horas)
        cuanto in CANTIDADES_TABLETA ->
            ValoresIniciales(m.nombre, cat, false, m.para, presentacion ?: cat.presentaciones.singleOrNull(), cuanto, "", momento, horas)
        else ->
            ValoresIniciales(m.nombre, cat, false, m.para, presentacion ?: cat.presentaciones.singleOrNull(), OTRA, cuanto, momento, horas)
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
