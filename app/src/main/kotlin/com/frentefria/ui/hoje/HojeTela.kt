package com.frentefria.ui.hoje

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.dias
import com.frentefria.motor.conteudo.conteudoDoDia
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.emojiDoNivel
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDoNivel
import com.frentefria.motor.texto.resumoDaFase
import com.frentefria.motor.texto.termos
import com.frentefria.motor.agenda.emojiDoTipo
import com.frentefria.ui.Cartao
import com.frentefria.ui.Estado
import com.frentefria.ui.PrincipalViewModel
import com.frentefria.ui.Secao
import com.frentefria.ui.Tela
import com.frentefria.ui.corDaFase
import com.frentefria.ui.corDoNivel
import com.frentefria.ui.curta
import com.frentefria.ui.emDias
import com.frentefria.ui.semana
import java.time.LocalDate

@Composable
fun HojeTela(e: Estado, vm: PrincipalViewModel, aoRegistrar: (LocalDate) -> Unit, aoDr: () -> Unit, aoConfigurar: () -> Unit) {
    val s = e.situacao
    val modo = e.ajustes.modo
    val t = termos(modo)
    val clima = modo == Modo.CLIMA
    var confirmarInicio by remember { mutableStateOf(false) }
    var confirmarFim by remember { mutableStateOf(false) }

    Tela(
        "Frente Fria",
        acoes = { IconButton(onClick = aoConfigurar) { Icon(Icons.Filled.Settings, "Configurações") } },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            val agora = s.agora
            if (agora == null) {
                Cartao {
                    Text("Sem dados", style = MaterialTheme.typography.titleMedium)
                    Text(if (clima) "Registre a última chuva para ver a previsão." else "Registre a última menstruação para ver a previsão.")
                    Button(onClick = { confirmarInicio = true }) { Text(t.menstruouHoje) }
                }
                return@Column
            }
            val d = agora.dia
            val conteudo = conteudoDoDia(vm.conteudo, s.hoje, d.fase, agora.nivel)

            // Cartão de previsão
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(corDoNivel(agora.nivel), corDoNivel(agora.nivel).copy(alpha = 0.75f))))
                    .padding(20.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emojiDoNivel(agora.nivel), fontSize = 64.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(nomeDoNivel(agora.nivel, modo), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(nomeDaFase(d.fase, modo), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (d.fase == Fase.ATRASADA) (if (clima) "Atrasada há ${d.diasAtraso} dias" else "Atrasou ${d.diasAtraso} dias") else "Dia ${d.diaDoCiclo} ${t.doCiclo}",
                                color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    conteudo.manchete?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(it.em(modo), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(resumoDaFase(d.fase, modo), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Contagens
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val ateTpm = s.diasAteTpm
                val p = s.previsao!!
                Contagem(
                    Modifier.weight(1f),
                    titulo = if (clima) "Frente fria" else "TPM",
                    valor = when {
                        d.fase == Fase.TPM || d.fase == Fase.ATRASADA -> "agora"
                        ateTpm != null -> emDias(ateTpm)
                        else -> "—"
                    },
                    detalhe = s.proximaTpm?.curta() ?: (if (clima) "em andamento" else "em andamento"),
                    cor = corDaFase(Fase.TPM),
                )
                val ateInicio = dias(s.hoje, d.proximoInicio)
                Contagem(
                    Modifier.weight(1f),
                    titulo = if (clima) "Próxima chuva" else "Menstruação",
                    valor = if (d.fase == Fase.ATRASADA) "atrasada" else emDias(ateInicio),
                    detalhe = if (d.fase == Fase.ATRASADA) "previsto ${d.proximoInicio.curta()}" else "${d.proximoInicio.curta()} (±${p.margem})",
                    cor = corDaFase(Fase.MENSTRUACAO),
                )
            }

            // Ações rápidas
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.menstruacaoEmCurso != null) {
                    FilledTonalButton(onClick = { confirmarFim = true }, Modifier.weight(1f)) { Text(t.parouHoje) }
                } else {
                    FilledTonalButton(onClick = { confirmarInicio = true }, Modifier.weight(1f)) { Text(t.menstruouHoje) }
                }
                FilledTonalButton(onClick = { aoRegistrar(s.hoje) }, Modifier.weight(1f)) {
                    Text(if (s.registroDeHoje != null) "Editar o dia" else "Registrar o dia")
                }
            }
            OutlinedButton(
                onClick = aoDr,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("⚡ " + t.dr) }

            // Próximos 7 dias
            Secao(if (clima) "Próximos dias" else "Próximos 7 dias")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.proximosDias(7)) { dn ->
                    Column(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(if (dn.dia.data == s.hoje) "Hoje" else dn.dia.data.semana(), style = MaterialTheme.typography.labelMedium)
                        Text(emojiDoNivel(dn.nivel), fontSize = 26.sp)
                        Box(Modifier.width(28.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(corDaFase(dn.dia.fase)))
                        Text(nomeDoNivel(dn.nivel, modo), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }

            // Eventos em conflito
            val conflitos = s.conflitos(modo, ate = 30)
            if (conflitos.isNotEmpty()) {
                Secao(if (clima) "Alertas na agenda" else "Eventos em dia difícil")
                conflitos.forEach { c ->
                    Cartao(Modifier.padding(bottom = 8.dp), cor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text("${emojiDoTipo(c.evento.tipo)} ${c.evento.titulo} · ${c.evento.data.curta()} (${emDias(dias(s.hoje, c.evento.data))})", fontWeight = FontWeight.SemiBold)
                        Text("${emojiDoNivel(c.nivel)} ${nomeDoNivel(c.nivel, modo)} · ${nomeDaFase(c.dia.fase, modo)}", style = MaterialTheme.typography.bodySmall)
                        Text(c.dica, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Kit faltando perto da TPM
            val faltando = e.kit.filter { it.estoque && !it.tem }
            val ateTpm = s.diasAteTpm
            val emTpm = d.fase == Fase.TPM || d.fase == Fase.ATRASADA
            if (faltando.isNotEmpty() && (emTpm || (ateTpm != null && ateTpm <= 5) || d.fase == Fase.MENSTRUACAO)) {
                Spacer(Modifier.height(12.dp))
                Cartao(cor = MaterialTheme.colorScheme.errorContainer) {
                    Text(if (clima) "🛒 Despensa incompleta" else "🛒 Kit de sobrevivência incompleto", fontWeight = FontWeight.SemiBold)
                    Text("Falta: " + faltando.joinToString(", ") { it.nome })
                }
            }

            // Faça / não faça
            Secao("✅ Faça hoje")
            conteudo.faca.forEach { Item(it.em(modo)) }
            Secao("🚫 Não faça hoje")
            conteudo.naoFaca.forEach { Item(it.em(modo)) }

            conteudo.fato?.let {
                Spacer(Modifier.height(16.dp))
                Cartao(cor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("💡 Você sabia?", fontWeight = FontWeight.SemiBold)
                    Text(it.em(modo), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (d.fertil && !clima) {
                Spacer(Modifier.height(12.dp))
                Text("🌡️ Janela fértil estimada. Isso é estimativa, não anticoncepcional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (confirmarInicio) {
        AlertDialog(
            onDismissRequest = { confirmarInicio = false },
            title = { Text(t.menstruouHoje + "?") },
            text = { Text(if (clima) "Registra hoje como o primeiro dia de chuva e recalcula a previsão. Foi outro dia? Marque pelo calendário." else "Registra hoje como o primeiro dia e recalcula a previsão. Foi outro dia? Marque pelo calendário.") },
            confirmButton = { TextButton(onClick = { vm.menstruou(s.hoje); confirmarInicio = false }) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { confirmarInicio = false }) { Text("Cancelar") } },
        )
    }
    if (confirmarFim) {
        AlertDialog(
            onDismissRequest = { confirmarFim = false },
            title = { Text(t.parouHoje + "?") },
            text = { Text("Marca hoje como o último dia. Ajuda a prever quantos dias dura.") },
            confirmButton = { TextButton(onClick = { vm.parou(s.hoje); confirmarFim = false }) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { confirmarFim = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Contagem(modifier: Modifier, titulo: String, valor: String, detalhe: String, cor: Color) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(cor.copy(alpha = 0.18f)).padding(14.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(detalhe, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Item(texto: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•  ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
