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
import com.utom.buenvivir.alarmas.ModoPrueba
import com.utom.buenvivir.alarmas.PruebaRapida
import com.utom.buenvivir.reloj.Reloj
import com.utom.buenvivir.trabajos.Trabajos
import com.utom.buenvivir.trabajos.AvisosDosis
import com.google.android.gms.wearable.Wearable
import com.utom.buenvivir.data.AjustesStore
import com.utom.buenvivir.data.RepositorioLocal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.utom.buenvivir.data.MedicinaNube
import com.utom.buenvivir.data.Recetas
import com.utom.buenvivir.data.SesionStore
import com.utom.buenvivir.ui.medico.AvisoMedico
import com.utom.buenvivir.ui.medico.RecetaScreen
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
import com.utom.buenvivir.ui.screens.textoSinMedicinas
import com.utom.buenvivir.ui.screens.RespaldoScreen
import com.utom.buenvivir.ui.screens.TEXTO_RESPALDO
import com.utom.buenvivir.ui.screens.NombreScreen
import com.utom.buenvivir.ui.screens.TonoScreen
import com.utom.buenvivir.ui.screens.MiMedicoScreen
import com.utom.buenvivir.ui.screens.PrivacidadScreen
import com.utom.buenvivir.ui.screens.AyudaScreen
import com.utom.buenvivir.ui.screens.AcercaDeScreen
import com.utom.buenvivir.ui.screens.CambiarRolScreen
import com.utom.buenvivir.data.Resultado
import com.utom.buenvivir.ui.screens.TEXTO_PRIVACIDAD
import com.utom.buenvivir.ui.screens.TEXTO_AYUDA
import com.utom.buenvivir.ui.screens.TEXTO_ACERCA
import com.utom.buenvivir.alarmas.Tonos
import com.utom.buenvivir.ui.theme.AzulMarino
import com.utom.buenvivir.ui.theme.Blanco
import kotlinx.coroutines.delay

private val FondoOscurecido = Color(0x6B151834) // rgba(21,24,52,0.42)
private const val DURACION_AVISO_MS = 5_000L

/** Flujo del rol paciente. Las medicinas, las tomas y los ajustes se guardan en el celular. */
@Composable
fun PacienteApp(
    nombre: String,
    onCambiarNombre: (String) -> Unit = {},
    onDejarDeSerPaciente: () -> Unit = {},
    abrirMotivo: Boolean = false,
    onMotivoAbierto: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lector = rememberLector()
    val repo = remember { RepositorioLocal.get(context) }
    val store = remember { AjustesStore(context) }

    var mostrarMotivo by remember { mutableStateOf(false) }
    var mostrarAviso by remember { mutableStateOf(false) }
    var avisos by remember { mutableIntStateOf(0) }
    var textoAviso by remember { mutableStateOf("Tu respuesta se guardó") }
    var pestana by rememberSaveable { mutableIntStateOf(0) } // 0 Inicio, 1 Mis medicinas, 2 Mi familia, 3 Detalle, 4 Ajustes, 5 Mi código, 6 Mi nombre … 13 Agregar medicina, 14 Cambiar medicina

    // Su médico y su familia, en vivo desde Firebase
    val equipo by remember { Nube.escucharEquipo() }.collectAsState(initial = emptyList())

    // "Mi código": se pide a Firebase al abrir la pantalla (la primera vez se crea)
    var estadoCodigo by remember { mutableStateOf<EstadoCodigo>(EstadoCodigo.Cargando) }
    var intentosCodigo by remember { mutableIntStateOf(0) }
    LaunchedEffect(pestana, intentosCodigo) {
        if (pestana == 5 && estadoCodigo !is EstadoCodigo.Listo) {
            estadoCodigo = EstadoCodigo.Cargando
            Nube.miCodigo(nombre) { codigo ->
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

    // Entra a Firebase como usuario anónimo (la primera vez crea su cuenta) y guarda su nombre
    LaunchedEffect(nombre) { Nube.entrar(nombre) }

    // Datos guardados
    LaunchedEffect(Unit) {
        repo.prepararSiVacia()
        Programador.programarTodas(context)
        // Las medicinas que estaban solo en el celular se suben, y después se escucha la nube:
        // si su médico o su familia le agregan o cambian una medicina, aquí aparece sola con sus alarmas
        repo.subirSinNube()
        Nube.escucharMisMedicinas().collect { lista ->
            if (lista == null) return@collect
            // Si su médico o su cuidador le cambiaron la dosis, le llega una notificación
            AvisosDosis.revisar(context, lista)
            if (repo.sincronizar(lista)) Programador.programarTodas(context)
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

    // ¿Hay reloj conectado de verdad? Se revisa cada 5 segundos mientras está abierto Ajustes
    var relojConectado by remember { mutableStateOf<String?>(null) } // su nombre, o null si no hay
    LaunchedEffect(pestana) {
        while (pestana == 4) {
            Wearable.getNodeClient(context).connectedNodes
                .addOnSuccessListener { nodos -> relojConectado = nodos.firstOrNull()?.displayName }
                .addOnFailureListener { relojConectado = null }
            delay(5_000)
        }
    }

    // El reloj muestra cuál es la siguiente pastilla: se actualiza al cambiar las medicinas o al registrar una toma
    LaunchedEffect(medsCargadas, tomas.size, ahora.hour) { Reloj.enviarSiguiente(context) }
    // Cada 15 minutos, aunque la app esté cerrada: trae las medicinas que le pongan y programa sus alarmas
    LaunchedEffect(Unit) { Trabajos.programarPaciente(context) }

    // El reloj guarda todos los horarios y programa sus propias alarmas (suena aunque se aleje del celular)
    LaunchedEffect(medsCargadas, ajustes.avisarEnReloj, ajustes.vibrarEnReloj, ajustes.sonarEnReloj) {
        Reloj.enviarHorarios(context, ajustes.avisarEnReloj, ajustes.vibrarEnReloj, ajustes.sonarEnReloj)
    }
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
            in 6..12, 15 -> 4 // de Mi nombre, Tono, Mi médico, Privacidad, Ayuda, Acerca de, Quién usa o Respaldar, a Ajustes
            13 -> 1 // de Agregar medicina, a Mis medicinas
            14 -> 3 // de Cambiar medicina, a su detalle
            else -> 0
        }
    }

    // Si se abre o se cierra la hoja de motivo, o se cambia de pantalla, deja de hablar
    LaunchedEffect(mostrarMotivo, pestana) { lector.callar() }

    fun avisar(texto: String) {
        textoAviso = texto
        avisos++
    }

    // Sus medicinas como están en Firebase: para saber cuáles agregó él mismo (esas las puede cambiar o quitar)
    val misNube by remember { Nube.escucharMisMedicinas() }.collectAsState(initial = null)
    var cambiando by remember { mutableStateOf<MedicinaNube?>(null) }
    var correoRespaldo by remember { mutableStateOf(Nube.correoDeRespaldo()) }
    var respaldando by remember { mutableStateOf(false) }
    var avisoRespaldo by remember { mutableStateOf<AvisoMedico?>(null) }
    var guardandoMed by remember { mutableStateOf(false) }
    var errorMed by remember { mutableStateOf<String?>(null) }

    /** Guarda en Firebase. Sin internet, Firebase la guarda en el celular y la sube después. */
    fun guardarMedicina(m: MedicinaNube, alTerminar: () -> Unit) {
        if (guardandoMed) return
        scope.launch {
            guardandoMed = true
            errorMed = null
            val uid = Nube.miId()
            if (uid == null) {
                errorMed = "La primera vez se necesita internet. Conéctate e inténtalo otra vez."
            } else {
                when (val r = withTimeoutOrNull(6_000) { Recetas.guardar(uid, m) } ?: Resultado.Bien) {
                    Resultado.Bien -> alTerminar()
                    is Resultado.Mal -> errorMed = r.mensaje
                }
            }
            guardandoMed = false
        }
    }

    // Mientras se carga la base de datos la primera vez (es un instante)
    if (medsCargadas == null) {
        Box(Modifier.fillMaxSize().background(Blanco))
        return
    }

    // Ajustes → Mi perfil: cambiar su nombre
    if (pestana == 6) {
        NombreScreen(
            inicial = nombre,
            editando = true,
            onRegresar = { pestana = 4 },
            onListo = { onCambiarNombre(it); pestana = 4 }
        )
        return
    }

    // Quién usa este celular: borrar los datos del paciente y regresar a elegir rol
    if (pestana == 12) {
        var borrando by remember { mutableStateOf(false) }
        var errorBorrar by remember { mutableStateOf<String?>(null) }
        CambiarRolScreen(
            nombre = nombre,
            ocupado = borrando,
            error = errorBorrar,
            onConfirmar = {
                if (!borrando) scope.launch {
                    borrando = true
                    errorBorrar = null
                    when (val r = Nube.borrarPaciente()) {
                        Resultado.Bien -> {
                            repo.borrarTodo()
                            onDejarDeSerPaciente()
                        }
                        is Resultado.Mal -> errorBorrar = r.mensaje
                    }
                    borrando = false
                }
            },
            onRegresar = { pestana = 4 },
            onEscuchar = {
                lector.leer(
                    "Este celular es de $nombre. Para usarlo como médico, como cuidador o para otro paciente, " +
                        "hay que borrar sus datos. Se borran sus medicinas, sus alarmas y su historial. No se puede deshacer."
                )
            }
        )
        return
    }

    // Ajustes → Respaldar mi cuenta: correo y contraseña para recuperarla en otro celular
    if (pestana == 15) {
        RespaldoScreen(
            correoActual = correoRespaldo,
            ocupado = respaldando,
            aviso = avisoRespaldo,
            onRespaldar = { correo, contrasena ->
                if (!respaldando) scope.launch {
                    respaldando = true
                    avisoRespaldo = null
                    when (val r = Nube.respaldar(correo, contrasena)) {
                        Resultado.Bien -> {
                            correoRespaldo = Nube.correoDeRespaldo()
                            avisar("Tu cuenta quedó respaldada")
                        }
                        is Resultado.Mal -> avisoRespaldo = AvisoMedico(r.mensaje, esError = true)
                    }
                    respaldando = false
                }
            },
            onRegresar = { avisoRespaldo = null; pestana = 4 },
            onEscuchar = { lector.leer(TEXTO_RESPALDO) }
        )
        return
    }

    // El paciente agrega (13) o cambia (14) una medicina él mismo
    if (pestana == 13 || pestana == 14) {
        val editando = if (pestana == 14) cambiando else null
        RecetaScreen(
            paciente = if (editando != null) "Regresar" else "Mis medicinas",
            medico = nombre,
            rol = SesionStore.PACIENTE,
            ocupado = guardandoMed,
            aviso = errorMed?.let { AvisoMedico(it, esError = true) },
            onRegresar = { errorMed = null; pestana = if (editando != null) 3 else 1 },
            inicial = editando,
            onGuardar = { m ->
                guardarMedicina(m) {
                    pestana = if (editando != null) 3 else 1
                    avisar(if (editando != null) "Se guardaron los cambios" else "Se agregó ${m.nombre}")
                }
            }
        )
        return
    }

    // Pantallas que se abren desde Ajustes
    if (pestana in 7..11) {
        val alAjustes = { pestana = 4 }
        when (pestana) {
            7 -> TonoScreen(
                elegido = Tonos.valido(ajustes.tono),
                opciones = Tonos.opciones,
                onElegir = { t ->
                    scope.launch { store.guardar(ajustes.copy(tono = t)) }
                    Notificaciones.probarVolumen(context, ajustes.volumen, t) // suena para que lo escuche
                },
                onRegresar = alAjustes,
                onEscuchar = {
                    lector.leer("Tono. Toca un tono para escucharlo. Las opciones son: " + Tonos.opciones.joinToString(", ") + ".")
                }
            )
            8 -> MiMedicoScreen(
                equipo = equipo,
                onVerCodigo = { pestana = 5 },
                onRegresar = alAjustes,
                onEscuchar = {
                    val medicos = equipo.filter { it.esMedico }
                    lector.leer(
                        if (medicos.isEmpty()) "Todavía no tienes médico en la app. Dale tu código para que pueda ver tus tomas."
                        else "Tu médico: " + medicos.joinToString(" y ") { it.nombre } + "."
                    )
                }
            )
            9 -> PrivacidadScreen(onRegresar = alAjustes, onEscuchar = { lector.leer(TEXTO_PRIVACIDAD) })
            10 -> AyudaScreen(onRegresar = alAjustes, onEscuchar = { lector.leer(TEXTO_AYUDA) })
            else -> AcercaDeScreen(onRegresar = alAjustes, onEscuchar = { lector.leer(TEXTO_ACERCA) })
        }
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
                    onIrMedicinas = { pestana = 1 }
                )
                5 -> CodigoScreen(
                    estado = estadoCodigo,
                    onRegresar = { pestana = 0 },
                    onEscuchar = {
                        (estadoCodigo as? EstadoCodigo.Listo)?.let { lector.leer(textoCodigo(it.codigo)) }
                    },
                    onReintentar = { intentosCodigo++ },
                    onIrInicio = { pestana = 0 },
                    onIrMedicinas = { pestana = 1 }
                )
                4 -> AjustesScreen(
                    ajustes = ajustes,
                    nombre = nombre,
                    relojNombre = relojConectado ?: "Reloj inteligente",
                    relojConectado = relojConectado != null,
                    onCambiar = { nuevos -> scope.launch { store.guardar(nuevos) } },
                    onProbarVolumen = { nivel -> Notificaciones.probarVolumen(context, nivel, ajustes.tono) },
                    onRegresar = { pestana = 0 },
                    onEscuchar = { lector.leer(textoAjustes(ajustes)) },
                    onAbrirOpcion = { pestana = pestanaDeOpcion(it) ?: pestana },
                    respaldada = correoRespaldo != null,
                    mostrarDemo = false,
                    puedeProbar = false,
                    onIrMedicinas = { pestana = 1 },
                    onIrFamilia = { pestana = 2 }
                )
                1 -> MedicinasScreen(
                    semana = semanaVacia(hoy),
                    tomadasSemana = 0,
                    totalSemana = 0,
                    tomasHoy = emptyList(),
                    medicinas = emptyList(),
                    onEscuchar = { lector.leer("Mis medicinas. Todavía no tienes medicinas. Puedes agregar una con el botón.") },
                    onAgregar = { errorMed = null; pestana = 13 },
                    onIrInicio = { pestana = 0 },
                    onIrFamilia = { pestana = 2 }
                )
                else -> SinMedicinasScreen(
                    nombre = nombre,
                    acompanantes = equipo.map { it.nombre },
                    onEscuchar = {
                        lector.leer("Todavía no tienes medicinas. " + textoSinMedicinas(equipo.map { it.nombre }))
                    },
                    onVerCodigo = { pestana = 5 },
                    onAgregar = { errorMed = null; pestana = 13 },
                    onAjustes = { pestana = 4 },
                    onIrMedicinas = { pestana = 1 },
                    onIrFamilia = { pestana = 2 }
                )
            }
        }
        return
    }

    fun registrar(tomada: Boolean, motivo: String?) {
        val esPrueba = ModoPrueba.activa(context, p.horario.id, p.hoy)
        scope.launch { repo.registrar(p.horario, p.hoy, tomada, motivo) }
        // Ya respondió: se quita la notificación y deja de repetirse
        Notificaciones.quitar(context, p.horario.id)
        Programador.cancelarRepeticion(context, p.horario.id)
        avisar(if (esPrueba) "Era una prueba: no se guardó" else "Tu respuesta se guardó")
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
                onAgregar = { errorMed = null; pestana = 13 },
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
            3 -> {
                val nubeId = meds.firstOrNull { it.medicina.id == medicina.id }?.medicina?.nubeId
                val propia = misNube?.firstOrNull { it.id == nubeId }?.takeIf { it.agregadaPor == SesionStore.PACIENTE }
                DetalleScreen(
                medicina = medicina,
                historial = historial,
                quienLaPuso = misNube?.firstOrNull { it.id == nubeId }?.let { quienLaPuso(it) },
                cambios = misNube?.firstOrNull { it.id == nubeId }?.cambios.orEmpty(),
                onCambiar = propia?.let { n -> { errorMed = null; cambiando = n; pestana = 14 } },
                onQuitar = propia?.let { n ->
                    {
                        scope.launch {
                            val uid = Nube.miId() ?: return@launch
                            withTimeoutOrNull(6_000) { Recetas.quitar(uid, n.id) }
                            pestana = 1
                            avisar("Se quitó ${n.nombre}")
                        }
                        Unit
                    }
                },
                onRegresar = { pestana = 1 },
                onEscuchar = { lector.leer(textoDetalle(medicina)) },
                onVerVideo = {}, // el video se conecta después
                onIrInicio = { pestana = 0 },
                onIrFamilia = { pestana = 2 }
            )
            }
            4 -> AjustesScreen(
                ajustes = ajustes,
                nombre = nombre,
                relojNombre = relojConectado ?: "Reloj inteligente",
                relojConectado = relojConectado != null,
                onCambiar = { nuevos -> scope.launch { store.guardar(nuevos) } },
                onProbarVolumen = { nivel -> Notificaciones.probarVolumen(context, nivel, ajustes.tono) },
                onRegresar = { pestana = 0 },
                onEscuchar = { lector.leer(textoAjustes(ajustes)) },
                    onAbrirOpcion = { pestana = pestanaDeOpcion(it) ?: pestana },
                    respaldada = correoRespaldo != null,
                onProbarRecordatorio = {
                    PruebaRapida.programar(context, p.horario.id, p.hoy)
                    avisar("Sonará en ${PruebaRapida.SEGUNDOS} segundos")
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
                nombre = nombre,
                tomadas = p.tomadasHoy,
                total = p.totalHoy,
                toma = p.toma,
                estado = p.estado,
                horaTomada = p.horaTomada,
                siguiente = p.siguiente,
                onEscuchar = { lector.leer(textoInicio(p, nombre)) },
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

/** "Te la puso tu médico", "Te la puso tu cuidador" o "La agregaste tú". */
private fun quienLaPuso(m: MedicinaNube): String = when (m.agregadaPor) {
    SesionStore.MEDICO -> "Te la puso tu médico" + (if (m.quien.isNotBlank()) ": ${m.quien}" else "")
    SesionStore.FAMILIAR -> "Te la puso tu cuidador" + (if (m.quien.isNotBlank()) ": ${m.quien}" else "")
    SesionStore.PACIENTE -> "La agregaste tú"
    else -> ""
}

/** Qué pantalla abre cada opción de Ajustes (null = no abre nada). */
private fun pestanaDeOpcion(opcion: String): Int? = when (opcion) {
    "Mi perfil" -> 6
    "tono" -> 7
    "Mi médico" -> 8
    "Privacidad" -> 9
    "Ayuda" -> 10
    "Acerca de BuenVivir" -> 11
    "Quién usa este celular" -> 12
    "Respaldar mi cuenta" -> 15
    else -> null
}

/** Deletreado y despacio: "Tu código es: K, 7, M. 2, Q, 9." */
private fun textoCodigo(codigo: String): String =
    "Tu código es: " + codigo.chunked(3).joinToString(". ") { it.toList().joinToString(", ") } + "."

/** Lo que se lee en la tarjeta de la pastilla, según cómo va la toma. */
private fun textoInicio(p: Panorama, nombre: String): String {
    val t = p.toma
    val s = p.siguiente
    val despues = "Tu siguiente pastilla es ${s.medicina}, ${s.cuando.replaceFirstChar { it.lowercase() }}."
    return when (val e = p.estado) {
        EstadoToma.Pendiente ->
            "${nombre}, ahora te toca ${t.medicina}, ${t.para.replaceFirstChar { it.lowercase() }}. " +
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
