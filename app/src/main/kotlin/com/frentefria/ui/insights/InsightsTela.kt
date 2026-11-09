package com.frentefria.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.TPM_PADRAO
import com.frentefria.motor.insights.CURVA_ANTES
import com.frentefria.motor.insights.CURVA_DEPOIS
import com.frentefria.motor.insights.Insights
import com.frentefria.motor.insights.calcularInsights
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.emojiDaTatica
import com.frentefria.motor.texto.emojiDoSintoma
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDaTatica
import com.frentefria.motor.texto.nomeDoMotivo
import com.frentefria.motor.texto.nomeDoSintoma
import com.frentefria.motor.texto.termos
import com.frentefria.ui.Cartao
import com.frentefria.ui.Estado
import com.frentefria.ui.Secao
import com.frentefria.ui.Tela
import com.frentefria.ui.corDaFase
import java.util.Locale
import kotlin.math.roundToInt

private val FASES = listOf(Fase.MENSTRUACAO, Fase.FOLICULAR, Fase.OVULACAO, Fase.LUTEA, Fase.TPM)
private fun Double.f1() = String.format(Locale("pt", "BR"), "%.1f", this)

@Composable
fun InsightsTela(e: Estado) {
    val s = e.situacao
    val modo = e.ajustes.modo
    val t = termos(modo)
    val clima = modo == Modo.CLIMA
    val nome = e.ajustes.nomeDela
    val i: Insights = remember(s) { calcularInsights(s.dados.menstruacoes, s.dados.registros, s.previsao, s.dados.config, s.hoje) }

    Tela(if (clima) "Dados do tempo" else "Insights") { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Numero(Modifier.weight(1f), "${i.diasRegistrados}", "dias registrados")
                Numero(Modifier.weight(1f), "${s.dados.menstruacoes.size}", if (clima) "chuvas anotadas" else "inícios anotados")
                Numero(Modifier.weight(1f), "${i.drsTotal}", if (clima) "trovoadas" else "DRs")
            }

            // Regularidade e previsão
            Cartao {
                Text(if (clima) "📏 Regularidade das estações" else "📏 Regularidade do ciclo", fontWeight = FontWeight.SemiBold)
                val r = i.regularidade
                if (r == null) {
                    Text(if (clima) "Preciso de pelo menos 3 chuvas anotadas. Não sou vidente (ainda)." else "Preciso de pelo menos 3 inícios anotados. Não sou vidente (ainda).")
                } else {
                    Text("Média de ${r.media.f1()} dias, ${r.classificacao} (±${r.desvio.f1()}).")
                    Text("Mais curto: ${r.menor} dias · mais longo: ${r.maior} dias · ${r.ciclos} ${if (r.ciclos == 1) "medição" else "medições"}.", style = MaterialTheme.typography.bodySmall)
                }
                i.acuracia?.let { a ->
                    Spacer(Modifier.height(4.dp))
                    Text("🎯 A previsão erra em média ${a.erroMedio.f1()} ${if (a.erroMedio == 1.0) "dia" else "dias"}; acertou com folga de 1 dia em ${a.acertosEmUmDia} de ${a.previsoes}.")
                }
            }

            Cartao {
                Text(if (clima) "🥶 A frente fria de $nome" else "🥶 A TPM de $nome", fontWeight = FontWeight.SemiBold)
                val p = s.previsao
                when {
                    s.dados.config.tpmFixa != null -> Text("Fixada em ${s.dados.config.tpmFixa} dias nas configurações.")
                    i.tpmPorCiclo.size >= 2 -> Text("Começa em média ${p?.diasTpm} dias antes. Medido em ${i.tpmPorCiclo.size} ${if (clima) "estações" else "ciclos"}: ${i.tpmPorCiclo.joinToString(", ")} dias.")
                    else -> Text("Ainda usando o padrão de $TPM_PADRAO dias. Com 2 ${if (clima) "estações" else "ciclos"} bem registrados (humor e sintomas nos dias antes), aprendo a dela.")
                }
            }

            // Curva de humor
            Secao(if (clima) "Termômetro do humor" else "Humor ao redor da menstruação")
            Cartao {
                if (i.curvaHumor.count { it.amostras > 0 } < 4) {
                    Text("Registre o humor em mais dias para ver a curva. Os 14 dias antes e os 7 depois são os que importam.")
                } else {
                    CurvaHumor(i, s.previsao?.diasTpm ?: TPM_PADRAO)
                    Text(if (clima) "Eixo: dias antes e depois do início da chuva (0). Mais alto = melhor humor." else "Eixo: dias antes e depois do início da menstruação (0). Mais alto = melhor humor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Por fase
            Secao("Por fase")
            val mult = i.multiplicadorDrTpm
            if (mult != null) {
                Cartao(cor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        if (mult >= 1.2) "⚡ ${if (clima) "Trovoada" else "DR"} é ${mult.f1()}x mais provável na ${t.tpm}."
                        else "⚡ Na ${t.tpm}, ${if (clima) "trovoada" else "DR"} não é mais comum que no resto. Parabéns, ou sorte.",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            val maxDias = i.porFase.values.maxOfOrNull { it.diasRegistrados } ?: 0
            if (maxDias == 0) {
                Text("Sem registros ainda. Use o \"Registrar o dia\" no Hoje.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FASES.forEach { f ->
                val ef = i.porFase[f] ?: return@forEach
                Cartao {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(10.dp).height(28.dp).clip(RoundedCornerShape(4.dp)).background(corDaFase(f)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nomeDaFase(f, modo), fontWeight = FontWeight.SemiBold)
                            Text("${ef.diasRegistrados} dias · humor ${ef.humorMedio?.f1() ?: "—"}/5 · ${ef.drs} ${if (clima) "trovoadas" else "DRs"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    ef.humorMedio?.let { Barra(it / 5.0, corDaFase(f)) }
                    if (ef.sintomas.isNotEmpty()) {
                        Text(ef.sintomas.joinToString("   ") { (sint, pct) -> "${emojiDoSintoma(sint)} ${nomeDoSintoma(sint, modo)} ${(pct * 100).roundToInt()}%" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Táticas
            Secao("O que funciona com $nome")
            if (i.taticas.isEmpty()) {
                Text("Marque o que você tentou nos registros (chocolate, espaço, massagem…) e se funcionou. Aqui sai o ranking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            i.taticas.forEachIndexed { pos, et ->
                Cartao {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${pos + 1}. ${emojiDaTatica(et.tatica)} ${nomeDaTatica(et.tatica, modo)}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(et.taxaSucesso?.let { "${(it * 100).roundToInt()}%" } ?: "—", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${et.usos} ${if (et.usos == 1) "vez" else "vezes"} · 👍 ${et.funcionou} · 👎 ${et.falhou}" + (et.melhorFase?.let { " · melhor na ${nomeDaFase(it, modo).lowercase()}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    et.taxaSucesso?.let { Barra(it, if (it >= 0.5) Color(0xFF6DBE8C) else MaterialTheme.colorScheme.error) }
                }
            }

            if (i.motivos.isNotEmpty()) {
                Secao(if (clima) "Por que trovejou" else "Motivos de DR")
                Cartao {
                    val max = i.motivos.first().second
                    i.motivos.take(6).forEach { (m, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nomeDoMotivo(m), Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                            Box(Modifier.weight(1f)) { Barra(n.toDouble() / max, MaterialTheme.colorScheme.tertiary) }
                            Text("  $n", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun Numero(modifier: Modifier, valor: String, legenda: String) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(legenda, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Barra(fracao: Double, cor: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Box(Modifier.fillMaxWidth(fracao.toFloat().coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(cor))
    }
}

/** Linha do humor médio de -14 a +7, com faixas de fundo para TPM e menstruação. */
@Composable
private fun CurvaHumor(i: Insights, diasTpm: Int) {
    val linha = MaterialTheme.colorScheme.primary
    val grade = MaterialTheme.colorScheme.outlineVariant
    val tpm = corDaFase(Fase.TPM).copy(alpha = 0.15f)
    val chuva = corDaFase(Fase.MENSTRUACAO).copy(alpha = 0.15f)
    val pontos = i.curvaHumor
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val total = (CURVA_ANTES + CURVA_DEPOIS).toFloat()
        fun x(d: Int) = (d + CURVA_ANTES) / total * size.width
        fun y(h: Double) = size.height - ((h - 1) / 4.0 * size.height).toFloat()
        drawRect(tpm, topLeft = Offset(x(-diasTpm), 0f), size = androidx.compose.ui.geometry.Size(x(0) - x(-diasTpm), size.height))
        drawRect(chuva, topLeft = Offset(x(0), 0f), size = androidx.compose.ui.geometry.Size(x(5) - x(0), size.height))
        (1..5).forEach { h -> drawLine(grade, Offset(0f, y(h.toDouble())), Offset(size.width, y(h.toDouble())), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))) }
        drawLine(grade, Offset(x(0), 0f), Offset(x(0), size.height), 2f)
        val caminho = Path()
        pontos.forEachIndexed { k, p ->
            val o = Offset(x(p.diaRelativo), y(p.humorMedio))
            if (k == 0) caminho.moveTo(o.x, o.y) else caminho.lineTo(o.x, o.y)
        }
        drawPath(caminho, linha, style = Stroke(width = 4f, cap = StrokeCap.Round))
        pontos.forEach { p -> drawCircle(linha, radius = 3f + minOf(p.amostras, 5), center = Offset(x(p.diaRelativo), y(p.humorMedio))) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("-14", "-7", "0", "+7").forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
    }
}
