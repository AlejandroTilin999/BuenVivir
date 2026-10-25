package com.utom.buenvivir.reloj

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.launch
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent

// Colores (los mismos de la app del celular, sobre fondo negro de reloj)
private val Negro = Color(0xFF000000)
private val Blanco = Color(0xFFFFFFFF)
private val GrisTexto = Color(0xFFC9CBD6)
private val AzulBoton = Color(0xFF004EE0)
private val GrisBoton = Color(0xFF3A3A52)
private val Violeta = Color(0xFF8B5CF6)
private val Lavanda = Color(0xFFD3E3FD)

private enum class Paso { Toma, Motivos, Listo }

/**
 * La app del reloj: muestra la pastilla que toca y deja contestar
 * "Ya me la tomé" o "No me la tomé" + motivo. La respuesta se manda al celular.
 */
class MainActivity : ComponentActivity() {

    private var toma by mutableStateOf<Toma?>(null)
    private var siguiente by mutableStateOf<Siguiente?>(null)
    private var abrirMotivo by mutableStateOf(false)

    // Si la toma cambia mientras la app está abierta (llegó otra o ya respondió en el celular)
    private val alCambiar = SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
        if (clave == Pendiente.CLAVE) toma = Pendiente.leer(this)
        if (clave == Pendiente.CLAVE_SIGUIENTE) siguiente = Pendiente.leerSiguiente(this)
    }

    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Voz que lee la pantalla en español (como en la app del celular)
    private var voz: TextToSpeech? = null
    /** Lee el texto; si estaba hablando, se calla y dice lo nuevo. Texto vacío = solo callarse. */
    private fun leer(texto: String) {
        if (texto.isBlank()) voz?.stop()
        else voz?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "pantalla")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prende la pantalla y se muestra aunque el reloj esté bloqueado; la pantalla no se apaga mientras está abierta
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        leer(intent)
        voz = TextToSpeech(this) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                voz?.language = Locale.forLanguageTag("es-MX")
                voz?.setSpeechRate(0.85f) // un poco más lento, para que se entienda bien
            }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            PantallaReloj(
                toma = toma,
                siguiente = siguiente,
                empezarEnMotivos = abrirMotivo,
                onTomada = { t -> Telefono.responder(this, t, tomada = true, motivo = null) },
                onMotivo = { t, motivo -> Telefono.responder(this, t, tomada = false, motivo = motivo) },
                onTerminar = { finish() },
                onLeer = { leer(it) }
            )
        }
    }

    override fun onDestroy() {
        voz?.shutdown()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leer(intent)
    }

    override fun onStart() {
        super.onStart()
        Pendiente.preferencias(this).registerOnSharedPreferenceChangeListener(alCambiar)
        toma = Pendiente.leer(this)
        siguiente = Pendiente.leerSiguiente(this)
    }

    override fun onStop() {
        Pendiente.preferencias(this).unregisterOnSharedPreferenceChangeListener(alCambiar)
        super.onStop()
    }

    private fun leer(intent: Intent?) {
        toma = Pendiente.leer(this)
        abrirMotivo = intent?.getBooleanExtra(Aviso.EXTRA_MOTIVO, false) == true
    }
}

@Composable
private fun PantallaReloj(
    toma: Toma?,
    siguiente: Siguiente?,
    empezarEnMotivos: Boolean,
    onTomada: (Toma) -> Unit,
    onMotivo: (Toma, String) -> Unit,
    onTerminar: () -> Unit,
    onLeer: (String) -> Unit = {}
) {
    // Se guarda la toma que se está contestando, para que no desaparezca al responder
    var actual by remember { mutableStateOf(toma) }
    var paso by remember { mutableStateOf(if (empezarEnMotivos) Paso.Motivos else Paso.Toma) }
    var tomada by remember { mutableStateOf(true) }
    LaunchedEffect(toma, empezarEnMotivos) {
        if (paso != Paso.Listo) {
            actual = toma
            paso = if (empezarEnMotivos) Paso.Motivos else Paso.Toma
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro),
        contentAlignment = Alignment.Center
    ) {
        val t = actual
        when {
            paso == Paso.Listo -> Listo(tomada, siguiente, onTerminar)
            t == null -> Espera(siguiente, onLeer)
            paso == Paso.Motivos -> Motivos(t.motivos, onLeer) { motivo ->
                onLeer("") // se calla
                tomada = false
                paso = Paso.Listo
                onMotivo(t, motivo)
            }
            else -> PantallaToma(
                t,
                onLeer = onLeer,
                onTomada = { onLeer(""); tomada = true; paso = Paso.Listo; onTomada(t) },
                onNoTomada = { onLeer(""); paso = Paso.Motivos }
            )
        }
    }
}

// ---------- Pantallas ----------

/** Sin pastilla pendiente. */
@Composable
private fun Espera(siguiente: Siguiente?, onLeer: (String) -> Unit) {
    Columna {
        BotonEscuchar {
            onLeer(
                if (siguiente == null) "Aquí te aviso cuando toque tu pastilla."
                else "Tu siguiente pastilla es ${siguiente.medicina}, ${siguiente.cantidad}, ${siguiente.cuando.replace("·", "a las")}."
            )
        }
        Image(painterResource(R.drawable.ic_buenvivir), contentDescription = null, modifier = Modifier.size(44.dp))
        if (siguiente == null) {
            Texto("BuenVivir", 18.sp, Blanco, FontWeight.ExtraBold)
            Texto("Aquí te aviso cuando toque tu pastilla.", 14.sp, GrisTexto)
        } else {
            TarjetaSiguiente(siguiente)
        }
    }
}

/** "Tu siguiente pastilla": nombre, cuánto y cuándo. */
@Composable
private fun TarjetaSiguiente(s: Siguiente) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C2033))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Texto("Tu siguiente pastilla", 13.sp, GrisTexto, FontWeight.SemiBold)
        Texto(s.medicina, 18.sp, Blanco, FontWeight.ExtraBold)
        if (s.cantidad.isNotBlank()) Texto(s.cantidad, 13.sp, GrisTexto)
        Texto(s.cuando, 14.sp, Lavanda, FontWeight.Bold)
    }
}

@Composable
private fun PantallaToma(t: Toma, onLeer: (String) -> Unit, onTomada: () -> Unit, onNoTomada: () -> Unit) {
    Columna {
        BotonEscuchar {
            onLeer("Hora de tu ${t.medicina}. Toma ${t.cantidad} ${t.indicacion}. Toca: Ya me la tomé, o: No me la tomé.")
        }
        Texto("Hora de tu", 14.sp, GrisTexto, FontWeight.SemiBold)
        Texto(t.medicina, 22.sp, Blanco, FontWeight.ExtraBold)
        Texto("${t.cantidad} ${t.indicacion}".trim(), 14.sp, GrisTexto)
        Texto(t.hora, 14.sp, Lavanda, FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Boton("Ya me la tomé", AzulBoton, onClick = onTomada)
        Boton("No me la tomé", GrisBoton, onClick = onNoTomada)
    }
}

@Composable
/**
 * Elige el motivo: al tocar uno se marca y la voz lo dice (si toca otro, la voz cambia a ese).
 * Luego "Enviar" lo manda. Así puede escuchar antes de decidir.
 */
private fun Motivos(motivos: List<String>, onLeer: (String) -> Unit, onElegir: (String) -> Unit) {
    var elegido by remember { mutableStateOf<String?>(null) }
    Columna {
        BotonEscuchar { onLeer("¿Por qué no te la tomaste?") }
        Texto("¿Por qué no te la tomaste?", 15.sp, Blanco, FontWeight.ExtraBold)
        motivos.forEach { m ->
            val activo = m == elegido
            Boton(m, if (activo) Violeta else GrisBoton, marcado = activo) {
                elegido = m
                onLeer(m)
            }
        }
        elegido?.let { m -> Boton("Enviar", AzulBoton) { onElegir(m) } }
    }
}

/** "Listo" un momento y se cierra sola. */
@Composable
private fun Listo(tomada: Boolean, siguiente: Siguiente?, onTerminar: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(if (siguiente != null) 4_000 else 2_000)
        onTerminar()
    }
    Columna {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(if (tomada) Violeta else GrisBoton),
            contentAlignment = Alignment.Center
        ) {
            Texto(if (tomada) "✓" else "–", 30.sp, Blanco, FontWeight.ExtraBold)
        }
        Texto(if (tomada) "¡Muy bien!" else "Gracias por avisar", 18.sp, Blanco, FontWeight.ExtraBold)
        Texto("Se guardó en tu celular.", 14.sp, GrisTexto)
        siguiente?.let { TarjetaSiguiente(it) }
    }
}

// ---------- Piezas ----------

/**
 * Columna centrada con espacio para la orilla redonda del reloj. Si no cabe, se desliza
 * con el dedo o girando el bisel.
 */
@Composable
private fun Columna(contenido: @Composable ColumnScope.() -> Unit) {
    val desliz = rememberScrollState()
    val alcance = rememberCoroutineScope()
    val foco = remember { FocusRequester() }
    // El bisel solo funciona si la pantalla tiene el "foco"
    LaunchedEffect(Unit) { foco.requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { evento ->
                alcance.launch { desliz.scrollBy(evento.verticalScrollPixels) }
                true
            }
            .focusRequester(foco)
            .focusable()
            .verticalScroll(desliz)
            .padding(horizontal = 22.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        content = contenido
    )
}

/** Botón redondo con bocina: lee en voz alta lo que dice la pantalla. */
@Composable
private fun BotonEscuchar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A3150))
            .clickable(role = Role.Button, onClickLabel = "Escuchar", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.ic_bocina),
            contentDescription = "Escuchar esta pantalla",
            colorFilter = ColorFilter.tint(Lavanda),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun Texto(texto: String, tamano: TextUnit, color: Color, peso: FontWeight = FontWeight.Normal) {
    BasicText(
        texto,
        style = TextStyle(color = color, fontSize = tamano, fontWeight = peso, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Boton(texto: String, fondo: Color, marcado: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(fondo)
            .then(if (marcado) Modifier.border(2.dp, Blanco, RoundedCornerShape(24.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Texto(texto, 15.sp, Blanco, FontWeight.Bold)
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun PreviewToma() {
    PantallaReloj(
        toma = Toma(1, "2026-10-24", "Metformina", "1 tableta de 850 mg", "después de comer", "8:00 p. m.", true, listOf("Se me olvidó")),
        siguiente = Siguiente("Sitagliptina", "1 tableta de 100 mg", "Mañana · 8:00 a. m."),
        empezarEnMotivos = false,
        onTomada = {}, onMotivo = { _, _ -> }, onTerminar = {}
    )
}
