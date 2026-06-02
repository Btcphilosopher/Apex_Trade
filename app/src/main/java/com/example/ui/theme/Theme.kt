package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CryptoTerminalColorScheme = darkColorScheme(
    primary = TerminalPrimary,
    secondary = TerminalSecondary,
    tertiary = TerminalTertiary,
    background = TerminalBackground,
    surface = TerminalSurface,
    onPrimary = TerminalBackground,
    onSecondary = TerminalBackground,
    onBackground = TerminalPrimary,
    onSurface = TerminalPrimary,
    outline = TerminalBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CryptoTerminalColorScheme,
        typography = Typography,
        content = content
    )
}
