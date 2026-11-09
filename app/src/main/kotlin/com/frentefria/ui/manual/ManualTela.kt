package com.frentefria.ui.manual

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.conteudo.Conteudo
import com.frentefria.motor.conteudo.Item
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.ui.Cartao
import com.frentefria.ui.Estado
import com.frentefria.ui.Secao
import com.frentefria.ui.Tela
import com.frentefria.ui.corDaFase

private val ABAS = listOf(Fase.TPM, Fase.MENSTRUACAO, Fase.FOLICULAR, Fase.OVULACAO, Fase.LUTEA, Fase.ATRASADA)
private const val FRASES_BOMBA = -1

@Composable
fun ManualTela(e: Estado, conteudo: Conteudo) {
    val modo = e.ajustes.modo
    val faseAtual = e.situacao.agora?.dia?.fase
    // Abre na fase de hoje.
    var aba by rememberSaveable { mutableIntStateOf(ABAS.indexOf(faseAtual).coerceAtLeast(0)) }
    val abas = ABAS.map { nomeDaFase(it, modo) } + "💣 Frases-bomba"

    Tela(if (modo == Modo.CLIMA) "Almanaque" else "Manual de sobrevivência") { padding ->
        Column(Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = aba, edgePadding = 16.dp) {
                abas.forEachIndexed { i, nome ->
                    Tab(selected = aba == i, onClick = { aba = i }, text = { Text(nome + if (i < ABAS.size && ABAS[i] == faseAtual) " •" else "") })
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                if (aba == ABAS.size) {
                    Secao("Nunca, jamais, em hipótese alguma")
                    conteudo.nuncaDizer.forEach { ItemComPorque(it, modo, "🚫") }
                    return@Column
                }
                val fase = ABAS[aba]
                conteudo.oQueAcontece[fase]?.let {
                    Cartao(Modifier.padding(top = 16.dp), cor = corDaFase(fase).copy(alpha = 0.18f)) {
                        Text("O que está acontecendo", fontWeight = FontWeight.SemiBold)
                        Text(it.em(modo), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Secao("💬 Diga")
                conteudo.dizer.filter { it.serve(fase) }.forEach { ItemComPorque(it, modo, "“") }
                Secao("✅ Faça")
                conteudo.faca.filter { it.serve(fase) }.forEach { Linha(it.em(modo)) }
                Secao("🚫 Não faça")
                conteudo.naoFaca.filter { it.serve(fase) }.forEach { Linha(it.em(modo)) }
                Secao("🎁 Ideias de agrado")
                conteudo.agrados.filter { it.serve(fase) }.forEach { Linha(it.em(modo)) }
                val fatos = conteudo.fatos.filter { it.fases.isNotEmpty() && it.serve(fase) }
                if (fatos.isNotEmpty()) {
                    Secao("💡 Curiosidades")
                    fatos.forEach { Linha(it.em(modo)) }
                }
            }
        }
    }
}

@Composable
private fun ItemComPorque(item: Item, modo: Modo, marcador: String) {
    Cartao(Modifier.padding(bottom = 8.dp)) {
        Text("$marcador ${item.em(modo)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        item.porqueEm(modo)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Linha(texto: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•  ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
