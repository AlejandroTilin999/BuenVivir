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
