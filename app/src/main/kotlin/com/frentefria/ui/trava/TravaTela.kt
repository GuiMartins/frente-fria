package com.frentefria.ui.trava

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.frentefria.FrenteFriaApp
import kotlinx.coroutines.launch

fun biometriaDisponivel(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

fun pedirBiometria(activity: FragmentActivity, titulo: String, aoLiberar: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = aoLiberar()
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(titulo)
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText("Usar PIN")
        .build()
    prompt.authenticate(info)
}

/** Teclado de PIN reutilizado na trava, no onboarding e nas configurações. */
@Composable
fun TecladoPin(titulo: String, subtitulo: String?, erro: String?, aoCompletar: (String) -> Unit, extra: @Composable () -> Unit = {}) {
    var pin by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("☁️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        subtitulo?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { i ->
                Surface(
                    Modifier.size(14.dp).clip(CircleShape),
                    color = if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {}
            }
        }
        Text(erro ?: " ", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(12.dp))
        val teclas = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
        teclas.chunked(3).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                linha.forEach { t ->
                    FilledTonalButton(
                        onClick = {
                            when (t) {
                                "⌫" -> pin = pin.dropLast(1)
                                "✓" -> if (pin.length >= 4) { aoCompletar(pin); pin = "" }
                                else -> if (pin.length < 6) pin += t
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        enabled = t != "✓" || pin.length >= 4,
                    ) { Text(t, fontSize = 22.sp) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        extra()
    }
}

@Composable
fun TravaTela(biometria: Boolean) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val app = context.applicationContext as FrenteFriaApp
    val escopo = rememberCoroutineScope()
    var erro by remember { mutableStateOf<String?>(null) }
    var tentativas by remember { mutableStateOf(0) }
    var esqueceu by remember { mutableStateOf(false) }
    val podeBiometria = biometria && biometriaDisponivel(activity)

    LaunchedEffect(Unit) {
        if (podeBiometria) pedirBiometria(activity, "Frente Fria") { app.trava.liberarPorBiometria() }
    }

    Box(Modifier.fillMaxSize()) {
        TecladoPin(
            titulo = "Frente Fria",
            subtitulo = "Digite o PIN",
            erro = erro,
            aoCompletar = { pin ->
                escopo.launch {
                    if (!app.trava.conferir(pin)) {
                        tentativas++
                        erro = if (tentativas >= 3) "PIN errado. Tempo instável por aqui." else "PIN errado"
                    }
                }
            },
        ) {
            Row {
                if (podeBiometria) TextButton(onClick = { pedirBiometria(activity, "Frente Fria") { app.trava.liberarPorBiometria() } }) { Text("Usar digital") }
                if (tentativas >= 3) TextButton(onClick = { esqueceu = true }) { Text("Esqueci o PIN") }
            }
        }
    }

    if (esqueceu) {
        AlertDialog(
            onDismissRequest = { esqueceu = false },
            title = { Text("Esqueceu o PIN?") },
            text = { Text("Não tem conta nem recuperação: o único jeito é apagar tudo e começar do zero. Todos os registros somem.") },
            confirmButton = {
                TextButton(onClick = {
                    escopo.launch {
                        app.repositorio.limparTudo()
                        app.agendador.cancelarTudo()
                        app.preferencias.limparTudo()
                        app.trava.remover()
                    }
                    esqueceu = false
                }) { Text("Apagar tudo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { esqueceu = false }) { Text("Cancelar") } },
        )
    }
}
