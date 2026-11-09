package com.frentefria.ui.calendario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frentefria.motor.agenda.emojiDoTipo
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.emojiDoNivel
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDoNivel
import com.frentefria.motor.texto.termos
import com.frentefria.ui.Estado
import com.frentefria.ui.PT
import com.frentefria.ui.PrincipalViewModel
import com.frentefria.ui.Tela
import com.frentefria.ui.corDaFase
import com.frentefria.ui.porExtenso
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarioTela(e: Estado, vm: PrincipalViewModel, aoRegistrar: (LocalDate) -> Unit) {
    val s = e.situacao
    val modo = e.ajustes.modo
    val t = termos(modo)
    var mes by remember { mutableStateOf(YearMonth.from(s.hoje)) }
    var selecionado by remember { mutableStateOf<LocalDate?>(null) }
    val inicios = s.dados.menstruacoes.map { it.inicio }.toSet()
    val p = s.previsao

    Tela(if (modo == Modo.CLIMA) "Previsão" else "Calendário") { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mes = mes.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior") }
                Text(
                    mes.month.getDisplayName(TextStyle.FULL, PT).replaceFirstChar { it.uppercase() } + " " + mes.year,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                IconButton(onClick = { mes = mes.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo mês") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val primeiro = mes.atDay(1)
            val vazios = primeiro.dayOfWeek.value % 7 // domingo = 0
            val total = vazios + mes.lengthOfMonth()
            (0 until (total + 6) / 7).forEach { semana ->
                Row(Modifier.fillMaxWidth()) {
                    (0..6).forEach { col ->
                        val indice = semana * 7 + col - vazios
                        Box(Modifier.weight(1f).aspectRatio(0.85f).padding(2.dp)) {
                            if (indice in 0 until mes.lengthOfMonth()) {
                                val data = mes.atDay(indice + 1)
                                val dn = s.dia(data)
                                val registro = s.dados.registros[data]?.takeIf { !it.vazio }
                                val evento = s.dados.eventos.firstOrNull { it.data == data }
                                val cor = dn?.let { corDaFase(it.dia.fase) }
                                val projetado = dn != null && (data.isAfter(s.hoje))
                                Column(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                        .background(cor?.copy(alpha = if (projetado) 0.22f else 0.45f) ?: MaterialTheme.colorScheme.surfaceContainerLow)
                                        .then(if (data == s.hoje) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(10.dp)) else Modifier)
                                        .clickable { selecionado = data }
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        "${data.dayOfMonth}",
                                        fontWeight = if (data in inicios) FontWeight.ExtraBold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (data in inicios) Text("💧", fontSize = 9.sp)
                                        if (registro != null) Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                                        if (registro != null && registro.dr > 0) Text("⚡", fontSize = 9.sp)
                                        if (evento != null) Text(emojiDoTipo(evento.tipo), fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Fase.MENSTRUACAO, Fase.FOLICULAR, Fase.OVULACAO, Fase.LUTEA, Fase.TPM, Fase.ATRASADA).forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(corDaFase(f)))
                        Spacer(Modifier.width(4.dp))
                        Text(nomeDaFase(f, modo), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Cor forte: o que já aconteceu. Clara: previsão. 💧 início · ● registro · ⚡ " + (if (modo == Modo.CLIMA) "trovoada" else "DR"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (p != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Próxima ${t.menstruacao}: entre ${p.janelaInicio.porExtenso()} e ${p.janelaFim.porExtenso()}. " +
                        "Média de ${p.duracaoArredondada} dias (${p.ciclosUsados} ${if (p.ciclosUsados == 1) "medição" else "medições"}), ${t.tpm} de ${p.diasTpm} dias.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (modo == Modo.DIRETO) Text("Janela fértil é estimativa: não use como anticoncepcional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    selecionado?.let { data ->
        val dn = s.dia(data)
        val registro = s.dados.registros[data]?.takeIf { !it.vazio }
        val eInicio = data in inicios
        val emCurso = s.dados.menstruacoes.lastOrNull { !it.inicio.isAfter(data) }
        ModalBottomSheet(onDismissRequest = { selecionado = null }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(data.porExtenso(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (dn != null) {
                    Text("${emojiDoNivel(dn.nivel)} ${nomeDoNivel(dn.nivel, modo)} · ${nomeDaFase(dn.dia.fase, modo)}")
                    Text("Dia ${dn.dia.diaDoCiclo} ${t.doCiclo}" + if (data.isAfter(s.hoje)) " (previsão)" else "", style = MaterialTheme.typography.bodySmall)
                }
                s.dados.eventos.filter { it.data == data }.forEach { Text("${emojiDoTipo(it.tipo)} ${it.titulo}") }
                registro?.let { Text("Registrado: humor ${it.humor?.valor ?: "—"}/5" + (if (it.dr > 0) " · ⚡" else "") + (if (it.nota.isNotBlank()) " · \"${it.nota.take(40)}\"" else ""), style = MaterialTheme.typography.bodySmall) }
                if (!data.isAfter(s.hoje)) {
                    FilledTonalButton(onClick = { selecionado = null; aoRegistrar(data) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (registro != null) "Editar registro" else "Registrar este dia")
                    }
                    if (eInicio) {
                        OutlinedButton(onClick = { vm.apagarMenstruacao(data); selecionado = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (modo == Modo.CLIMA) "Não começou a chover aqui" else "Não começou neste dia")
                        }
                    } else {
                        OutlinedButton(onClick = { vm.menstruou(data); selecionado = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (modo == Modo.CLIMA) "Começou a chover neste dia" else "Menstruou neste dia")
                        }
                    }
                    if (emCurso != null && data.isAfter(emCurso.inicio) && data.toEpochDay() - emCurso.inicio.toEpochDay() <= 12) {
                        OutlinedButton(onClick = { vm.parou(data); selecionado = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (modo == Modo.CLIMA) "Parou de chover neste dia" else "Parou neste dia")
                        }
                    }
                }
            }
        }
    }
}
