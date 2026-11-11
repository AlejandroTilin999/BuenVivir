package com.utom.buenvivir.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.utom.buenvivir.ui.components.Poppins

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

/** Toda la app con letra Poppins, igual que las pantallas del rediseño. */
private fun androidx.compose.material3.Typography.conPoppins() = androidx.compose.material3.Typography(
    displayLarge = displayLarge.copy(fontFamily = Poppins),
    displayMedium = displayMedium.copy(fontFamily = Poppins),
    displaySmall = displaySmall.copy(fontFamily = Poppins),
    headlineLarge = headlineLarge.copy(fontFamily = Poppins),
    headlineMedium = headlineMedium.copy(fontFamily = Poppins),
    headlineSmall = headlineSmall.copy(fontFamily = Poppins),
    titleLarge = titleLarge.copy(fontFamily = Poppins),
    titleMedium = titleMedium.copy(fontFamily = Poppins),
    titleSmall = titleSmall.copy(fontFamily = Poppins),
    bodyLarge = bodyLarge.copy(fontFamily = Poppins),
    bodyMedium = bodyMedium.copy(fontFamily = Poppins),
    bodySmall = bodySmall.copy(fontFamily = Poppins),
    labelLarge = labelLarge.copy(fontFamily = Poppins),
    labelMedium = labelMedium.copy(fontFamily = Poppins),
    labelSmall = labelSmall.copy(fontFamily = Poppins)
)

// Siempre usa la paleta clara de BuenVivir (el tema oscuro opcional se agrega después)
@Composable
fun BuenVivirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BuenVivirColores,
        typography = Typography.conPoppins(),   // Typography viene de Type.kt
        content = content
    )
}
