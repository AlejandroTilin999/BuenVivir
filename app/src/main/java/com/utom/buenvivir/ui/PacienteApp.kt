package com.utom.buenvivir.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utom.buenvivir.data.Repositorio
import com.utom.buenvivir.ui.components.Iconos
import com.utom.buenvivir.ui.components.rememberLector
import androidx.compose.ui.platform.LocalContext
import com.utom.buenvivir.data.Ajustes
import com.utom.buenvivir.data.Medicina
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.utom.buenvivir.alarmas.Notificaciones
import com.utom.buenvivir.alarmas.Programador
import com.utom.buenvivir.data.AjustesStore
import com.utom.buenvivir.data.RepositorioLocal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.utom.buenvivir.data.MiembroEquipo
import com.utom.buenvivir.data.Nube
import com.utom.buenvivir.ui.screens.CodigoScreen
import com.utom.buenvivir.ui.screens.EstadoCodigo
import com.utom.buenvivir.ui.screens.AjustesScreen
import com.utom.buenvivir.ui.screens.DetalleScreen
import com.utom.buenvivir.ui.screens.EstadoToma
import com.utom.buenvivir.ui.screens.FamiliaScreen
import com.utom.buenvivir.ui.screens.InicioScreen
import com.utom.buenvivir.ui.screens.MedicinasScreen
import com.utom.buenvivir.ui.screens.MotivoSheet
import com.utom.buenvivir.ui.screens.SinMedicinasScreen
import com.utom.buenvivir.ui.theme.AzulMarino
import com.utom.buenvivir.ui.theme.Blanco
import kotlinx.coroutines.delay

private val FondoOscurecido = Color(0x6B151834) // rgba(21,24,52,0.42)
private const val DURACION_AVISO_MS = 5_000L

/** Flujo del rol paciente. Las medicinas, las tomas y los ajustes se guardan en el celular. */
@Composable
fun PacienteApp(abrirMotivo: Boolean = false, onMotivoAbierto: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lector = rememberLector()
    val repo = remember { RepositorioLocal.get(context) }
    val store = remember { AjustesStore(context) }

    var mostrarMotivo by remember { mutableStateOf(false) }
    var mostrarAviso by remember { mutableStateOf(false) }
    var avisos by remember { mutableIntStateOf(0) }
    var textoAviso by remember { mutableStateOf("Tu respuesta se guardó") }
    var pestana by rememberSaveable { mutableIntStateOf(0) } // 0 Inicio, 1 Mis medicinas, 2 Mi familia, 3 Detalle, 4 Ajustes, 5 Mi código

    // Su médico y su familia, en vivo desde Firebase
    val equipo by remember { Nube.escucharEquipo() }.collectAsState(initial = emptyList())

    // "Mi código": se pide a Firebase al abrir la pantalla (la primera vez se crea)
    var estadoCodigo by remember { mutableStateOf<EstadoCodigo>(EstadoCodigo.Cargando) }
    var intentosCodigo by remember { mutableIntStateOf(0) }
    LaunchedEffect(pestana, intentosCodigo) {
        if (pestana == 5 && estadoCodigo !is EstadoCodigo.Listo) {
            estadoCodigo = EstadoCodigo.Cargando
            Nube.miCodigo(Repositorio.nombrePaciente) { codigo ->
                estadoCodigo = if (codigo != null) EstadoCodigo.Listo(codigo) else EstadoCodigo.Error
            }
        }
    }

    // La hora se actualiza cada 30 segundos para que la pastilla de "ahora" cambie sola
    var ahora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            ahora = LocalDateTime.now()
        }
    }

    // Datos guardados
    LaunchedEffect(Unit) {
        // Entra a Firebase como usuario anónimo (la primera vez crea su cuenta)
        Nube.entrar(Repositorio.nombrePaciente)
        repo.prepararSiVacia()
        Programador.programarTodas(context)
        // Las medicinas que estaban solo en el celular se suben, y después se escucha la nube:
        // si su médico o su familia le agregan o cambian una medicina, aquí aparece sola con sus alarmas
        repo.subirSinNube()
        Nube.escucharMisMedicinas().collect { lista ->
            if (lista != null && repo.sincronizar(lista)) Programador.programarTodas(context)
        }
    }

    // Permiso para mostrar notificaciones (Android 13 en adelante lo pide al usuario)
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Si llegaron desde "No me la tomé" en la notificación, abre la pregunta del motivo
    LaunchedEffect(abrirMotivo) {
        if (abrirMotivo) {
            pestana = 0
            mostrarMotivo = true
            onMotivoAbierto()
        }
    }
    val medsCargadas by repo.medicinas.collectAsState(initial = null)
    val meds = medsCargadas.orEmpty()
    val hoy = ahora.toLocalDate()
    val lunes = hoy.minusDays(hoy.dayOfWeek.value - 1L)
    val tomas by remember(lunes) { repo.tomasDesde(lunes) }.collectAsState(initial = emptyList())
    // La medicina que se abrió en "Mis medicinas" (si no hay, la primera)
    var abiertaId by rememberSaveable { mutableStateOf<Long?>(null) }
    val medicinaId = meds.firstOrNull { it.medicina.id == abiertaId }?.medicina?.id ?: meds.firstOrNull()?.medicina?.id
    val ultimas by remember(medicinaId) {
        if (medicinaId == null) flowOf(emptyList()) else repo.ultimasTomas(medicinaId)
    }.collectAsState(initial = emptyList())
    val ajustes by store.ajustes.collectAsState(initial = Repositorio.ajustesIniciales)
    LaunchedEffect(ajustes.vozLenta) { lector.usarVozLenta(ajustes.vozLenta) }

    val p = calcularPanorama(meds, tomas, ahora)

    // Cada vez que se guarda algo, el aviso aparece unos segundos
    LaunchedEffect(avisos) {
        if (avisos > 0) {
            mostrarAviso = true
            delay(DURACION_AVISO_MS)
            mostrarAviso = false
        }
    }

    // El botón "atrás" del celular cierra la hoja de motivo
    BackHandler(enabled = mostrarMotivo) { mostrarMotivo = false }

    // El botón "atrás" en otra pestaña regresa a Inicio (desde el detalle, a Mis medicinas)
    BackHandler(enabled = pestana != 0 && !mostrarMotivo) {
        pestana = when (pestana) {
            3 -> 1 // del detalle, a Mis medicinas
            5 -> 2 // de Mi código, a Mi familia
            else -> 0
        }
    }

    // Si se abre o se cierra la hoja de motivo, o se cambia de pantalla, deja de hablar
    LaunchedEffect(mostrarMotivo, pestana) { lector.callar() }

    // Mientras se carga la base de datos la primera vez (es un instante)
    if (medsCargadas == null) {
        Box(Modifier.fillMaxSize().background(Blanco))
        return
    }

    // Todavía no tiene medicinas: se le explica que su médico se las agrega
    if (p == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (pestana) {
                2 -> FamiliaScreen(
                    equipo = equipo,
                    onEscuchar = { lector.leer(textoFamilia(equipo)) },
                    onAgregar = { pestana = 5 },
                    onQuitar = { m -> Nube.quitarDelEquipo(m.id) },
                    onIrInicio = { pestana = 0 },
                    onIrMedicinas = { pestana = 0 }
                )
                5 -> CodigoScreen(
                    estado = estadoCodigo,
                    onRegresar = { pestana = 0 },
                    onEscuchar = {
                        (estadoCodigo as? EstadoCodigo.Listo)?.let { lector.leer(textoCodigo(it.codigo)) }
                    },
                    onReintentar = { intentosCodigo++ },
                    onIrInicio = { pestana = 0 },
                    onIrMedicinas = { pestana = 0 }
                )
                4 -> AjustesScreen(
                    ajustes = ajustes,
                    nombre = Repositorio.nombrePaciente,
                    relojNombre = Repositorio.relojNombre,
                    relojConectado = Repositorio.relojConectado,
                    onCambiar = { nuevos -> scope.launch { store.guardar(nuevos) } },
                    onProbarVolumen = { nivel -> Notificaciones.probarVolumen(context, nivel) },
                    onRegresar = { pestana = 0 },
                    onEscuchar = { lector.leer(textoAjustes(ajustes)) },
                    mostrarDemo = false,
                    puedeProbar = false,
                    onIrMedicinas = { pestana = 0 },
                    onIrFamilia = { pestana = 2 }
                )
                else -> SinMedicinasScreen(
                    nombre = Repositorio.nombrePaciente,
                    onEscuchar = {
                        lector.leer(
                            "Todavía no tienes medicinas. Dale tu código a tu médico. " +
                                "Cuando te agregue una medicina, aparecerá aquí con su alarma."
                        )
                    },
                    onVerCodigo = { pestana = 5 },
                    onAjustes = { pestana = 4 },
                    onIrMedicinas = { pestana = 0 },
                    onIrFamilia = { pestana = 2 }
                )
            }
        }
        return
    }

    fun avisar(texto: String) {
        textoAviso = texto
        avisos++
    }

    fun registrar(tomada: Boolean, motivo: String?) {
        scope.launch { repo.registrar(p.horario, p.hoy, tomada, motivo) }
        // Ya respondió: se quita la notificación y deja de repetirse
        Notificaciones.quitar(context, p.horario.id)
        Programador.cancelarRepeticion(context, p.horario.id)
        avisar("Tu respuesta se guardó")
    }

    val medicina = p.medicinas.firstOrNull { it.id == medicinaId } ?: p.medicinas.first()
    val historial = historialDe(ultimas, p.hoy)

    Box(modifier = Modifier.fillMaxSize()) {
        when (pestana) {
            1 -> MedicinasScreen(
                semana = p.semana,
                tomadasSemana = p.tomadasSemana,
                totalSemana = p.totalSemana,
                tomasHoy = p.tomasHoy,
                medicinas = p.medicinas,
                onEscuchar = { lector.leer(textoMedicinas(p)) },
                onAbrirMedicina = { abiertaId = it.id; pestana = 3 },
                onIrInicio = { pestana = 0 },
                onIrFamilia = { pestana = 2 }
            )
            2 -> FamiliaScreen(
                equipo = equipo,
                onEscuchar = { lector.leer(textoFamilia(equipo)) },
                onAgregar = { pestana = 5 },
                onQuitar = { m ->
                    Nube.quitarDelEquipo(m.id)
                    avisar("Se quitó a ${m.nombre}")
                },
                onIrInicio = { pestana = 0 },
                onIrMedicinas = { pestana = 1 }
            )
            5 -> CodigoScreen(
                estado = estadoCodigo,
                onRegresar = { pestana = 2 },
                onEscuchar = {
                    (estadoCodigo as? EstadoCodigo.Listo)?.let { lector.leer(textoCodigo(it.codigo)) }
                },
                onReintentar = { intentosCodigo++ },
                onIrInicio = { pestana = 0 },
                onIrMedicinas = { pestana = 1 }
            )
            3 -> DetalleScreen(
                medicina = medicina,
                historial = historial,
                onRegresar = { pestana = 1 },
                onEscuchar = { lector.leer(textoDetalle(medicina)) },
                onVerVideo = {}, // el video se conecta después
                onIrInicio = { pestana = 0 },
                onIrFamilia = { pestana = 2 }
            )
            4 -> AjustesScreen(
                ajustes = ajustes,
                nombre = Repositorio.nombrePaciente,
                relojNombre = Repositorio.relojNombre,
                relojConectado = Repositorio.relojConectado,
                onCambiar = { nuevos -> scope.launch { store.guardar(nuevos) } },
                onProbarVolumen = { nivel -> Notificaciones.probarVolumen(context, nivel) },
                onRegresar = { pestana = 0 },
                onEscuchar = { lector.leer(textoAjustes(ajustes)) },
                onProbarRecordatorio = {
                    Programador.programarPrueba(context, p.horario.id, p.hoy)
                    avisar("Sonará en 1 minuto")
                },
                mostrarDemo = RepositorioLocal.MOSTRAR_REINICIAR,
                onReiniciarDemo = {
                    scope.launch { repo.reiniciarDemo() }
                    Notificaciones.quitar(context, p.horario.id)
                    avisar("Demostración reiniciada")
                },
                onIrMedicinas = { pestana = 1 },
                onIrFamilia = { pestana = 2 }
            )
            else -> InicioScreen(
                nombre = Repositorio.nombrePaciente,
                tomadas = p.tomadasHoy,
                total = p.totalHoy,
                toma = p.toma,
                estado = p.estado,
                horaTomada = p.horaTomada,
                siguiente = p.siguiente,
                onEscuchar = { lector.leer(textoInicio(p)) },
                onTomar = { registrar(tomada = true, motivo = null) },
                onNoTomada = { mostrarMotivo = true },
                onIrMedicinas = { pestana = 1 },
                onIrFamilia = { pestana = 2 },
                onAjustes = { pestana = 4 }
            )
        }

        AnimatedVisibility(
            visible = mostrarAviso && !mostrarMotivo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 108.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut()
        ) {
            AvisoGuardado(textoAviso)
        }

        AnimatedVisibility(visible = mostrarMotivo, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscurecido)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { mostrarMotivo = false }
            )
        }

        AnimatedVisibility(
            visible = mostrarMotivo,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            MotivoSheet(
                motivos = Repositorio.motivos,
                onEscuchar = { lector.leer(textoMotivo()) },
                onGuardar = { motivo ->
                    registrar(tomada = false, motivo = motivo)
                    mostrarMotivo = false
                },
                onCancelar = { mostrarMotivo = false }
            )
        }
    }
}

/** Deletreado y despacio: "Tu código es: K, 7, M. 2, Q, 9." */
private fun textoCodigo(codigo: String): String =
    "Tu código es: " + codigo.chunked(3).joinToString(". ") { it.toList().joinToString(", ") } + "."

/** Lo que se lee en la tarjeta de la pastilla, según cómo va la toma. */
private fun textoInicio(p: Panorama): String {
    val t = p.toma
    val s = p.siguiente
    val despues = "Tu siguiente pastilla es ${s.medicina}, ${s.cuando.replaceFirstChar { it.lowercase() }}."
    return when (val e = p.estado) {
        EstadoToma.Pendiente ->
            "${Repositorio.nombrePaciente}, ahora te toca ${t.medicina}, ${t.para.replaceFirstChar { it.lowercase() }}. " +
                "Tómate ${t.cantidad} ${t.indicacion}, a las ${t.hora}."
        EstadoToma.Tomada -> "Ya te tomaste la ${t.medicina}. $despues"
        is EstadoToma.Omitida -> "No te tomaste la ${t.medicina}. El motivo fue: ${e.motivo}. $despues"
    }
}

private fun textoMedicinas(p: Panorama): String {
    val tomas = p.tomasHoy.joinToString(". ") { t ->
        "A las ${t.hora}, ${t.medicina}, " + when (t.estado) {
            EstadoToma.Tomada -> "ya te la tomaste"
            EstadoToma.Pendiente -> "todavía no"
            is EstadoToma.Omitida -> "no te la tomaste"
        }
    }
    val medicinas = p.medicinas.joinToString(". ") { "${it.nombre}, ${it.frecuencia}" }
    return "Esta semana te tomaste ${p.tomadasSemana} de ${p.totalSemana} pastillas. Hoy: $tomas. Tus medicinas: $medicinas."
}

private fun textoFamilia(equipo: List<MiembroEquipo>): String {
    if (equipo.isEmpty()) {
        return "Mi familia. Todavía no agregas a nadie. Toca Agregar a alguien y dale tu código a tu médico o a un familiar."
    }
    val personas = equipo.joinToString(". ") { m ->
        (if (m.esMedico) "Tu médico: " else "") + m.nombre + if (m.detalle.isNotBlank()) ", ${m.detalle}" else ""
    }
    return "Mi familia. $personas. Ellos ven si tomaste tus pastillas para poder ayudarte."
}

private fun textoAjustes(a: Ajustes): String {
    fun si(v: Boolean) = if (v) "encendido" else "apagado"
    val volumen = listOf("bajo", "medio", "alto")[a.volumen]
    return "Ajustes. Sonido ${si(a.sonido)}. Vibración ${if (a.vibracion) "encendida" else "apagada"}. " +
        "Volumen $volumen. La alarma se repite cada ${a.repetirMinutos} minutos si no respondes. " +
        "Avisos en el reloj ${si(a.avisarEnReloj)}. Voz ${if (a.vozLenta) "lenta" else "normal"}."
}

/** Lo que se lee en "Cómo tomarla". */
private fun textoDetalle(m: Medicina): String =
    "${m.nombre}, ${m.para.replaceFirstChar { it.lowercase() }}. Toma ${m.cantidad} ${m.indicacion}. " +
        "Te toca a las " + m.horarios.joinToString(" y a las ") + "."

private fun textoMotivo(): String =
    "¿Por qué no te la tomaste? Tu familia y tu médico lo verán. Las opciones son: " +
        Repositorio.motivos.joinToString(", ") + "."

@Composable
private fun AvisoGuardado(texto: String) {
    val forma = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, forma)
            .clip(forma)
            .background(AzulMarino)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Blanco),
            contentAlignment = Alignment.Center
        ) {
            Icon(Iconos.Palomita, contentDescription = null, tint = AzulMarino, modifier = Modifier.size(18.dp))
        }
        Text(texto, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Blanco)
    }
}
