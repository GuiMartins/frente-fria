package com.frentefria.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import com.frentefria.motor.ciclo.Fase

// Azul de céu no primário, cinza de tempestade no secundário: cara de app de
// previsão do tempo, nos dois modos. Sem cor dinâmica do Android 12.

private val Claro = lightColorScheme(
    primary = Color(0xFF2F6AA3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF545F71),
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF8E4E6C),
    tertiaryContainer = Color(0xFFFFD8E7),
    onTertiaryContainer = Color(0xFF3A0726),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF6F9FE),
    surface = Color(0xFFF6F9FE),
    surfaceContainerLow = Color(0xFFF0F4FA),
    surfaceContainer = Color(0xFFEAEEF4),
    surfaceContainerHigh = Color(0xFFE4E8EE),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFFA2C9FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF0E4A82),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFBCC7DC),
    secondaryContainer = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFFFB0D1),
    tertiaryContainer = Color(0xFF713754),
    onTertiaryContainer = Color(0xFFFFD8E7),
    background = Color(0xFF0F1419),
    surface = Color(0xFF0F1419),
    surfaceContainerLow = Color(0xFF171C22),
    surfaceContainer = Color(0xFF1B2026),
    surfaceContainerHigh = Color(0xFF252A30),
)

@Composable
fun FrenteFriaTema(content: @Composable () -> Unit) {
    // A Surface por baixo de tudo define a cor do texto: sem ela, as telas sem
    // Scaffold (trava, onboarding) escreviam em preto sobre o fundo escuro.
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Escuro else Claro) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}

/** Cor de cada fase no calendário e nos gráficos (mesma nos dois temas, com alfa). */
fun corDaFase(fase: Fase): Color = when (fase) {
    Fase.MENSTRUACAO -> Color(0xFF5C7FB8) // chuva
    Fase.FOLICULAR -> Color(0xFF6DBE8C) // primavera
    Fase.OVULACAO -> Color(0xFFF2A541) // calor
    Fase.LUTEA -> Color(0xFFB08A63) // outono
    Fase.TPM -> Color(0xFF7E6BC4) // frente fria
    Fase.ATRASADA -> Color(0xFFC05A6E)
}

/** Do céu limpo ao furacão. */
fun corDoNivel(nivel: Int): Color = listOf(
    Color(0xFF4FA3E0),
    Color(0xFF7D9BB8),
    Color(0xFF5E6E8C),
    Color(0xFF4B4F7A),
    Color(0xFF6B2E4A),
)[nivel.coerceIn(0, 4)]
