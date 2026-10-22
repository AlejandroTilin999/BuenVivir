package com.utom.buenvivir.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BuenVivirColores = lightColorScheme(
    primary = AzulBoton,
    onPrimary = Blanco,
    secondary = Lavanda,
    onSecondary = TextoOscuro,
    background = Blanco,
    onBackground = TextoNormal,
    surface = Blanco,
    onSurface = TextoNormal
)

// Siempre usa la paleta clara de BuenVivir (el tema oscuro opcional se agrega después)
@Composable
fun BuenVivirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BuenVivirColores,
        typography = Typography,
        content = content
    )
}
