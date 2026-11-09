package com.frentefria.ui.registro

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frentefria.motor.registro.Humor
import com.frentefria.motor.registro.MotivoDr
import com.frentefria.motor.registro.RegistroDia
import com.frentefria.motor.registro.Sintoma
import com.frentefria.motor.registro.Tatica
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.emojiDaTatica
import com.frentefria.motor.texto.emojiDoSintoma
import com.frentefria.motor.texto.nomeDaDr
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDaTatica
import com.frentefria.motor.texto.nomeDoMotivo
import com.frentefria.motor.texto.nomeDoSintoma
import com.frentefria.ui.Estado
import com.frentefria.ui.PrincipalViewModel
import com.frentefria.ui.Secao
import com.frentefria.ui.Tela
import com.frentefria.ui.curta
import java.time.LocalDate

private val HUMORES = listOf(Humor.PESSIMO to "😡", Humor.RUIM to "😟", Humor.NEUTRO to "😐", Humor.BOM to "🙂", Humor.OTIMO to "🥰")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroTela(e: Estado, vm: PrincipalViewModel, data: LocalDate, comDr: Boolean, aoVoltar: () -> Unit) {
    val modo = e.ajustes.modo
    val clima = modo == Modo.CLIMA
    val original = e.situacao.dados.registros[data] ?: RegistroDia(data)
    var r by remember(data) { mutableStateOf(if (comDr && original.dr == 0) original.copy(dr = 2) else original) }
    val fase = e.situacao.dia(data)?.dia?.fase
    val nome = e.ajustes.nomeDela

    Tela(
        (if (data == e.situacao.hoje) "Hoje" else data.curta()),
        aoVoltar = aoVoltar,
        acoes = {
            if (!original.vazio) IconButton(onClick = { vm.apagarRegistro(data); aoVoltar() }) { Icon(Icons.Filled.Delete, "Apagar registro") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            fase?.let { Text(nomeDaFase(it, modo), color = MaterialTheme.colorScheme.onSurfaceVariant) }

            Secao("Como $nome estava?")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HUMORES.forEach { (h, emoji) ->
                    FilterChip(
                        selected = r.humor == h,
                        onClick = { r = r.copy(humor = if (r.humor == h) null else h) },
                        label = { Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(vertical = 6.dp)) },
                    )
                }
            }

            Secao("O que você percebeu")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sintoma.entries.forEach { s ->
                    FilterChip(
                        selected = s in r.sintomas,
                        onClick = { r = r.copy(sintomas = if (s in r.sintomas) r.sintomas - s else r.sintomas + s) },
                        label = { Text("${emojiDoSintoma(s)} ${nomeDoSintoma(s, modo)}") },
                    )
                }
            }

            Secao(if (clima) "Trovoada?" else "Teve DR?")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                (0..3).forEach { n ->
                    SegmentedButton(
                        selected = r.dr == n,
                        onClick = { r = r.copy(dr = n, motivos = if (n == 0) emptySet() else r.motivos) },
                        shape = SegmentedButtonDefaults.itemShape(n, 4),
                    ) { Text(nomeDaDr(n)) }
                }
            }
            if (r.dr > 0) {
                Text("Motivo", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MotivoDr.entries.forEach { m ->
                        FilterChip(
                            selected = m in r.motivos,
                            onClick = { r = r.copy(motivos = if (m in r.motivos) r.motivos - m else r.motivos + m) },
                            label = { Text(nomeDoMotivo(m)) },
                        )
                    }
                }
            }

            Secao("O que você tentou")
            Text("Toque uma vez: usou. Duas: funcionou 👍. Três: não funcionou 👎. Quatro: limpa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tatica.entries.forEach { t ->
                    val usada = t in r.taticas
                    val valor = r.taticas[t]
                    val sufixo = when { !usada -> ""; valor == true -> " 👍"; valor == false -> " 👎"; else -> "" }
                    FilterChip(
                        selected = usada,
                        onClick = {
                            r = r.copy(
                                taticas = when {
                                    !usada -> r.taticas + (t to null)
                                    valor == null -> r.taticas + (t to true)
                                    valor == true -> r.taticas + (t to false)
                                    else -> r.taticas - t
                                },
                            )
                        },
                        label = { Text("${emojiDaTatica(t)} ${nomeDaTatica(t, modo)}$sufixo") },
                        colors = when (valor) {
                            true -> FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF6DBE8C).copy(alpha = 0.4f))
                            false -> FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
                            else -> FilterChipDefaults.filterChipColors()
                        },
                    )
                }
            }

            Secao("Nota")
            OutlinedTextField(
                r.nota, { r = r.copy(nota = it) },
                placeholder = { Text("O que aconteceu, o que ela comentou, o que evitar…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(Modifier.height(20.dp))
            Button(onClick = { vm.salvarRegistro(r); aoVoltar() }, modifier = Modifier.fillMaxWidth()) { Text("Salvar") }
        }
    }
}
