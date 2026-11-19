package com.utom.buenvivir.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/** Íconos dibujados con los mismos trazos del prototipo. */
object Iconos {
    val Reloj = icono("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M12 7v5l3 2", grosor = 2.4f)
    val Bocina = icono("M11 5 6 9H3v6h3l5 4V5z", "M15.5 8.5a5 5 0 0 1 0 7", "M18.5 5.5a9 9 0 0 1 0 13")
    val Pastilla = icono("M8.5 8.5l7 7", girados = listOf("M7 8h10a4 4 0 0 1 0 8H7a4 4 0 0 1 0 -8z"))
    val Casa = icono("M3 11 12 4l9 7", "M5 10v10h14V10")
    val Familia = icono(
        "M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0",
        "M14.5 9a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        "M2.5 20a6.5 6.5 0 0 1 13 0",
        "M15 14.5a5 5 0 0 1 6.5 4.5"
    )
    val Palomita = icono("M5 12.5 10 17.5 19 7", grosor = 3f)
    val Menos = icono("M6 12h12", grosor = 2.6f)
    val Flecha = icono("M9 6l6 6l-6 6", grosor = 2.6f)
    val Engrane = icono(
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        grosor = 2.1f
    )
    val Campana = icono("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val RelojPulsera = icono(
        "M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0",
        "M12 10v2l1 1",
        "M16.13 7.66l-.81-4.05a2 2 0 0 0-2-1.61h-2.68a2 2 0 0 0-2 1.61l-.78 4.05",
        "M7.88 16.36l.8 4a2 2 0 0 0 2 1.61h2.72a2 2 0 0 0 2-1.61l.81-4.05"
    )
    val Estetoscopio = icono(
        "M6 3v6a6 6 0 0 0 12 0V3",
        "M12 15v2a4 4 0 0 0 8 0v-2",
        "M18 13a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
    )
    val Atras = icono("M15 6l-6 6l6 6", grosor = 2.6f)
    val Microfono = icono("M9 6a3 3 0 0 1 6 0v5a3 3 0 0 1 -6 0z", "M5 11a7 7 0 0 0 14 0", "M12 18v3", grosor = 2.4f)
    val Camara = icono(
        "M3 9a2 2 0 0 1 2-2h2l1.5-2h7L17 7h2a2 2 0 0 1 2 2v9a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2-2z",
        "M9 13a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", grosor = 2.4f
    )
    val Mas = icono("M12 5v14", "M5 12h14", grosor = 2.8f)
    val Candado = icono(
        "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2H7a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2z",
        "M8 11V8a4 4 0 0 1 8 0v3"
    )
    val Play: ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        addPath(pathData = addPathNodes("M8 5v14l11-7z"), fill = SolidColor(Color.Black))
    }.build()
    // Rellenos: la pestaña activa del menú de abajo
    val CasaLlena = relleno("M3 11 12 4l9 7v9a1 1 0 0 1 -1 1h-5v-6h-6v6H4a1 1 0 0 1 -1 -1z")
    val PastillaLlena = relleno(girados = listOf("M7 8h10a4 4 0 0 1 0 8H7a4 4 0 0 1 0 -8z"))
    val FamiliaLlena = relleno(
        "M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0",
        "M14.5 9a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        "M2.5 20a6.5 6.5 0 0 1 13 0z",
        "M15.6 20a7.4 7.4 0 0 0 -1.9 -5.1A5 5 0 0 1 21.5 19v1z"
    )
    val Tache = icono("M6 6l12 12", "M18 6L6 18", grosor = 3f)
    val Lupa = icono("M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0", "M20 20l-4-4", grosor = 2.4f)
    val Persona = icono("M8 8a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M4 21a8 8 0 0 1 16 0")
    val Ojo = icono("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0")
    val OjoTachado = icono(
        "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M3 3l18 18"
    )

    /**
     * Logo de BuenVivir: botiquín azul con una pastilla. Ya trae sus colores,
     * así que se dibuja con Image(), no con Icon() (Icon lo pintaría de un solo color).
     */
    val Logo: ImageVector = ImageVector.Builder(
        defaultWidth = 120.dp, defaultHeight = 120.dp, viewportWidth = 120f, viewportHeight = 120f
    ).apply {
        val bordePastilla = SolidColor(Color(0xFF3470F2))
        addPath(
            pathData = addPathNodes("M44 30v-7a6 6 0 0 1 6-6h20a6 6 0 0 1 6 6v7"),
            stroke = SolidColor(Color(0xFF004EE0)),
            strokeLineWidth = 6f,
            strokeLineCap = StrokeCap.Round
        )
        addPath(
            pathData = addPathNodes("M28 28h64a18 18 0 0 1 18 18v42a18 18 0 0 1 -18 18H28a18 18 0 0 1 -18 -18V46a18 18 0 0 1 18 -18z"),
            fill = SolidColor(Color(0xFF3D7BFF))
        )
        group(rotate = -35f, pivotX = 60f, pivotY = 67f) {
            addPath(
                pathData = addPathNodes("M43 56h16v22H43a11 11 0 0 1 0-22z"),
                fill = SolidColor(Color.White),
                stroke = bordePastilla,
                strokeLineWidth = 0.9f,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M61 56h16a11 11 0 0 1 0 22H61z"),
                fill = SolidColor(Color(0xFFC4B5FD)),
                stroke = bordePastilla,
                strokeLineWidth = 0.9f,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

private fun icono(vararg trazos: String, girados: List<String> = emptyList(), grosor: Float = 2.2f): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        group(rotate = -45f, pivotX = 12f, pivotY = 12f) {
            girados.forEach { trazo(it, grosor) }
        }
        trazos.forEach { trazo(it, grosor) }
    }.build()

private fun ImageVector.Builder.trazo(d: String, grosor: Float) {
    addPath(
        pathData = addPathNodes(d),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = grosor,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )
}

/** Ícono relleno (sin trazo), para la pestaña activa. */
private fun relleno(vararg formas: String, girados: List<String> = emptyList()): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        group(rotate = -45f, pivotX = 12f, pivotY = 12f) {
            girados.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }
        formas.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
    }.build()
