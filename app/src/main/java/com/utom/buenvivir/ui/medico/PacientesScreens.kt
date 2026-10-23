package com.utom.buenvivir.ui.medico

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Busqueda
import com.utom.buenvivir.data.PacienteVinculado
import com.utom.buenvivir.data.TomaNube
import com.utom.buenvivir.data.TomasPaciente
import com.utom.buenvivir.data.Vinculos
import com.utom.buenvivir.data.diaTexto
import com.utom.buenvivir.data.horaDeRegistro
import com.utom.buenvivir.data.horaTexto
import com.utom.buenvivir.data.nombreDia
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.theme.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Resumen de los últimos 7 días ----------

/** A partir de qué porcentaje de tomas se considera que "va bien" (80 %). */
private const val META = 0.8

enum class EstadoDia { Todas, Falto, SinDatos }

data class DiaResumen(val fecha: LocalDate, val estado: EstadoDia)

data class Resumen(
    val tomadas: Int,
    val registradas: Int,
    val dias: List<DiaResumen>,
    val motivos: List<Pair<String, Int>>,
    val ultimaFalta: TomaNube?,
    val recientes: List<TomaNube>
) {
    val vaBien get() = registradas > 0 && tomadas.toDouble() / registradas >= META
    val porcentaje get() = if (registradas == 0) 0f else tomadas.toFloat() / registradas
}

/** Desde qué día se piden las tomas: hoy y los 6 días anteriores. */
fun desdeSemana(hoy: LocalDate = LocalDate.now()): LocalDate = hoy.minusDays(6)

fun resumir(tomas: List<TomaNube>, hoy: LocalDate = LocalDate.now()): Resumen {
    val semana = tomas.filter { !it.fecha.isBefore(desdeSemana(hoy)) && !it.fecha.isAfter(hoy) }
    val porDia = semana.groupBy { it.fecha }
    val dias = (6 downTo 0).map { atras ->
        val f = hoy.minusDays(atras.toLong())
        val del = porDia[f].orEmpty()
        DiaResumen(
            f,
            when {
                del.isEmpty() -> EstadoDia.SinDatos
                del.any { !it.tomada } -> EstadoDia.Falto
                else -> EstadoDia.Todas
            }
        )
    }
    val faltas = semana.filter { !it.tomada }
    val ordenadas = semana.sortedWith(compareByDescending<TomaNube> { it.fecha }.thenByDescending { it.minutos })
    return Resumen(
        tomadas = semana.count { it.tomada },
        registradas = semana.size,
        dias = dias,
        motivos = faltas.groupingBy { it.motivo ?: "Otro motivo" }.eachCount().toList().sortedByDescending { it.second },
        ultimaFalta = ordenadas.firstOrNull { !it.tomada },
        recientes = ordenadas.take(8)
    )
}

private fun veces(n: Int) = if (n == 1) "1 vez" else "$n veces"

private val FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-MX"))

private fun fechaDe(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(FECHA_LARGA)

// ---------- M4 · Mis pacientes ----------

@Composable
fun PacientesScreen(
    nombre: String,
    pacientes: List<PacienteVinculado>?,
    onVincular: () -> Unit,
    onAbrir: (PacienteVinculado) -> Unit
) {
    // Cada tarjeta avisa cómo va su paciente, para sumar el resumen de arriba
    val resumenes = remember { mutableStateMapOf<String, Resumen>() }
    val vigentes = pacientes.orEmpty().mapNotNull { resumenes[it.id] }
    val vanBien = vigentes.count { it.vaBien }
    val revisar = vigentes.count { it.registradas > 0 && !it.vaBien }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Lavanda)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(saludo(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
                Text(
                    nombre,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
            }
            if (!pacientes.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Blanco)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Dato(pacientes.size, if (pacientes.size == 1) "paciente" else "pacientes", TextoOscuro, Modifier.weight(1f))
                    Dato(vanBien, if (vanBien == 1) "va bien" else "van bien", Violeta, Modifier.weight(1f))
                    Dato(revisar, "por revisar", GrisAviso, Modifier.weight(1f))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                pacientes == null -> Cargando()
                pacientes.isEmpty() -> SinPacientes()
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (pacientes.size == 1) "Tu paciente" else "Tus pacientes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextoNormal,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text("Últimos 7 días", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario)
                    }
                    pacientes.forEach { p ->
                        TarjetaPaciente(p, onResumen = { resumenes[p.id] = it }) { onAbrir(p) }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            BotonPrincipal("Vincular paciente", onClick = onVincular)
        }
    }
}

@Composable
private fun Dato(numero: Int, texto: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("$numero", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario)
    }
}

@Composable
private fun SinPacientes() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, BordeTarjeta, RoundedCornerShape(22.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Lavanda),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Familia, contentDescription = null, tint = TextoChip, modifier = Modifier.size(32.dp))
        }
        Text("Todavía no tienes pacientes", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
        Text(
            "Pídele a tu paciente el código que aparece en su app, en Mi familia → Agregar a alguien.",
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = TextoSecundario
        )
    }
}

/** Anillo con el porcentaje, nombre, "Tomó X de Y" y cómo va. Se actualiza solo. */
@Composable
private fun TarjetaPaciente(p: PacienteVinculado, onResumen: (Resumen) -> Unit, onClick: () -> Unit) {
    val hoy = LocalDate.now()
    val estado by remember(p.id) { Vinculos.tomas(p.id, desdeSemana(hoy)) }
        .collectAsState(initial = TomasPaciente.Cargando)
    val r = (estado as? TomasPaciente.Listas)?.let { resumir(it.tomas, hoy) }
    LaunchedEffect(r) { r?.let(onResumen) }
    val revisar = r != null && r.registradas > 0 && !r.vaBien
    val forma = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(if (revisar) FondoOmitida else Blanco)
            .border(2.dp, if (revisar) GrisBorde else BordeTarjeta, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Anillo(
            porcentaje = r?.porcentaje ?: 0f,
            color = if (revisar) GrisAviso else Violeta,
            texto = if (r == null || r.registradas == 0) "—" else "${(r.porcentaje * 100).roundToInt()}%"
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(p.nombre, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextoNormal)
            Text(
                when {
                    estado is TomasPaciente.SinPermiso -> "Te quitó de su equipo"
                    r == null -> "Cargando…"
                    r.registradas == 0 -> "Todavía no registra tomas"
                    else -> "Tomó ${r.tomadas} de ${r.registradas}"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoSecundario
            )
        }
        when {
            estado is TomasPaciente.SinPermiso -> Etiqueta("Ya no comparte", GrisAviso, Blanco)
            r == null || r.registradas == 0 -> {}
            revisar -> Etiqueta("Revisar", GrisAviso, Blanco)
            else -> Etiqueta("Va bien", MoradoAviso, TextoMorado)
        }
        Icon(Iconos.Flecha, contentDescription = null, tint = TextoChip, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Anillo(porcentaje: Float, color: Color, texto: String) {
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 6.dp.toPx()
            val esquina = Offset(grosor / 2, grosor / 2)
            val tamano = Size(size.width - grosor, size.height - grosor)
            drawArc(Lavanda, 0f, 360f, useCenter = false, topLeft = esquina, size = tamano, style = Stroke(grosor))
            if (porcentaje > 0f) {
                drawArc(
                    color, -90f, 360f * porcentaje.coerceIn(0f, 1f), useCenter = false,
                    topLeft = esquina, size = tamano, style = Stroke(grosor, cap = StrokeCap.Round)
                )
            }
        }
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextoOscuro)
    }
}

// ---------- M5 · Vincular ----------

@Composable
fun VincularScreen(onRegresar: () -> Unit, onVinculado: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var codigo by rememberSaveable { mutableStateOf("") }
    var encontrado by remember { mutableStateOf<Busqueda.Encontrado?>(null) }
    var aviso by remember { mutableStateOf<AvisoMedico?>(null) }
    var ocupado by remember { mutableStateOf(false) }

    fun buscarOVincular() {
        if (ocupado) return
        scope.launch {
            ocupado = true
            aviso = null
            val p = encontrado
            if (p == null) {
                when (val r = Vinculos.buscar(codigo)) {
                    is Busqueda.Encontrado -> encontrado = r
                    is Busqueda.Mal -> aviso = AvisoMedico(r.mensaje, esError = true)
                }
            } else {
                when (val r = Vinculos.vincular(p)) {
                    com.utom.buenvivir.data.Resultado.Bien -> onVinculado(p.nombre)
                    is com.utom.buenvivir.data.Resultado.Mal -> aviso = AvisoMedico(r.mensaje, esError = true)
                }
            }
            ocupado = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Regresar("Pacientes", onRegresar)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Vincular paciente",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextoOscuro,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Pídele a tu paciente el código que aparece en su app, en Mi familia → Agregar a alguien.",
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = TextoSecundario
            )
        }
        Campo(
            etiqueta = "Código del paciente",
            valor = codigo,
            onCambio = { nuevo ->
                codigo = nuevo.uppercase().filter { it.isLetterOrDigit() }.take(6)
                encontrado = null
                aviso = null
            },
            ayuda = "6 letras y números",
            teclado = KeyboardType.Ascii,
            mayusculas = true,
            grande = true,
            ultimo = true,
            onListo = { buscarOVincular() }
        )

        encontrado?.let { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Lavanda)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Inicial(p.nombre, fondo = Blanco, tamano = 52)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Se encontró a", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
                    Text(p.nombre, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TextoOscuro)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Iconos.Candado, contentDescription = null, tint = TextoGrisMedio, modifier = Modifier.size(20.dp))
                Text(
                    "${p.nombre} verá en su app que ahora puedes ver sus tomas.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = TextoGrisMedio
                )
            }
        }
        aviso?.let { Aviso(it) }
        Spacer(Modifier.height(8.dp))
        BotonPrincipal(if (encontrado == null) "Buscar" else "Vincular", ocupado) { buscarOVincular() }
    }
}

// ---------- M6 · Detalle del paciente ----------

@Composable
fun DetallePacienteScreen(
    p: PacienteVinculado,
    onRegresar: () -> Unit,
    onQuitar: () -> Unit
) {
    val hoy = LocalDate.now()
    val estado by remember(p.id) { Vinculos.tomas(p.id, desdeSemana(hoy)) }
        .collectAsState(initial = TomasPaciente.Cargando)
    var confirmar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Regresar("Pacientes", onRegresar)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Inicial(p.nombre, fondo = Lavanda, tamano = 64)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    p.nombre,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoOscuro,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
                if (p.desde > 0) Text("Vinculado desde el ${fechaDe(p.desde)}", fontSize = 16.sp, color = TextoSecundario)
            }
        }

        when (val e = estado) {
            TomasPaciente.Cargando -> Cargando()
            TomasPaciente.SinPermiso ->
                Aviso(AvisoMedico("${p.nombre} te quitó de su equipo, así que ya no puedes ver sus tomas.", esError = true))
            is TomasPaciente.Listas -> {
                val r = resumir(e.tomas, hoy)
                TarjetaSemana(r)
                if (r.motivos.isNotEmpty()) Motivos(r.motivos)
                if (r.recientes.isNotEmpty()) UltimasTomas(r.recientes, hoy)
            }
        }

        OutlinedButton(
            onClick = { confirmar = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, AzulBoton),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulBoton)
        ) {
            Text("Dejar de ver a este paciente", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Dejar de ver a ${p.nombre}?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "Ya no verás sus tomas. Para volver a verlo, te tendrá que dar su código otra vez.",
                    fontSize = 17.sp,
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onQuitar() }) {
                    Text("Sí, dejar de verlo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AzulBoton)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) {
                    Text("Cancelar", fontSize = 17.sp, color = TextoGris)
                }
            },
            containerColor = Blanco
        )
    }
}

@Composable
private fun TarjetaSemana(r: Resumen) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Lavanda)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Últimos 7 días",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextoLavanda,
            modifier = Modifier.semantics { heading() }
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${r.tomadas} de ${r.registradas}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = TextoOscuro)
            Text("tomadas", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda, modifier = Modifier.padding(bottom = 6.dp))
        }
        Barra(r.porcentaje, fondo = Blanco)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            r.dias.forEach { d -> CirculoDia(d) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Leyenda(Violeta, Iconos.Palomita, "Las tomó todas")
            Leyenda(GrisAviso, Iconos.Tache, "Faltó una")
        }
    }
}

@Composable
private fun CirculoDia(d: DiaResumen) {
    val descripcion = nombreDia(d.fecha) + ": " + when (d.estado) {
        EstadoDia.Todas -> "las tomó todas"
        EstadoDia.Falto -> "faltó una"
        EstadoDia.SinDatos -> "sin registros"
    }
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(nombreDia(d.fecha).take(1), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextoLavanda)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (d.estado) {
                        EstadoDia.Todas -> Violeta
                        EstadoDia.Falto -> GrisAviso
                        EstadoDia.SinDatos -> Blanco
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (d.estado) {
                EstadoDia.Todas -> Icon(Iconos.Palomita, contentDescription = null, tint = Blanco, modifier = Modifier.size(16.dp))
                EstadoDia.Falto -> Icon(Iconos.Tache, contentDescription = null, tint = Blanco, modifier = Modifier.size(14.dp))
                EstadoDia.SinDatos -> {}
            }
        }
    }
}

@Composable
private fun Leyenda(color: Color, icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = Blanco, modifier = Modifier.size(11.dp))
        }
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextoLavanda)
    }
}

@Composable
private fun Motivos(motivos: List<Pair<String, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "¿Por qué no se la tomó?",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoNormal,
            modifier = Modifier.semantics { heading() }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, GrisBorde, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            motivos.forEachIndexed { i, (motivo, n) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(motivo, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextoNormal, modifier = Modifier.weight(1f))
                    Etiqueta(veces(n), GrisAviso, Blanco)
                }
                if (i < motivos.lastIndex) Box(Modifier.fillMaxWidth().height(1.5.dp).background(BordeOmitida))
            }
        }
    }
}

@Composable
private fun UltimasTomas(tomas: List<TomaNube>, hoy: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Últimas tomas",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoNormal,
            modifier = Modifier.semantics { heading() }
        )
        tomas.forEach { t ->
            val forma = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(if (t.tomada) Blanco else GrisFondo)
                    .border(2.dp, if (t.tomada) Lavanda else GrisBorde, forma)
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${diaTexto(t.fecha, hoy)} · ${horaTexto(t.minutos)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextoNormal)
                    if (t.tomada) {
                        if (t.registradaEn > 0) {
                            Text("A las ${horaDeRegistro(t.registradaEn)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextoMoradoMedio)
                        }
                    } else {
                        Text(t.motivo ?: "Otro motivo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextoGrisMedio)
                    }
                }
                if (t.tomada) Etiqueta("Tomada", Violeta, Blanco) else Etiqueta("No la tomó", GrisAviso, Blanco)
            }
        }
    }
}

// ---------- Piezas chicas ----------

@Composable
private fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Text(
        texto,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun Barra(porcentaje: Float, fondo: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(fondo)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(porcentaje.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(Violeta)
        )
    }
}

@Composable
private fun Inicial(nombre: String, fondo: Color, tamano: Int) {
    // "Don José" -> "J"; si no tiene "Don/Doña", la primera letra
    val letra = nombre.split(" ").lastOrNull { it.isNotBlank() }?.take(1)?.uppercase() ?: "?"
    Box(
        modifier = Modifier.size(tamano.dp).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Text(letra, fontSize = (tamano * 0.42f).sp, fontWeight = FontWeight.ExtraBold, color = TextoChip)
    }
}

@Composable
private fun Cargando() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AzulBoton)
    }
}

private fun saludo(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Buenos días,"
    in 12..18 -> "Buenas tardes,"
    else -> "Buenas noches,"
}

@Preview(name = "M4 · Mis pacientes (vacío)", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewPacientesVacio() = BuenVivirTheme {
    PacientesScreen("Laura Méndez", emptyList(), {}, {})
}
