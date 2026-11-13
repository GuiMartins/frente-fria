package com.frentefria.ui.config

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.frentefria.FrenteFriaApp
import com.frentefria.Icone
import com.frentefria.motor.avisos.Horario
import com.frentefria.motor.backup.Backup
import com.frentefria.motor.backup.PreferenciasBackup
import com.frentefria.motor.ciclo.TPM_MAXIMA
import com.frentefria.motor.ciclo.TPM_MINIMA
import com.frentefria.motor.ciclo.dias
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.termos
import com.frentefria.ui.Estado
import com.frentefria.ui.PrincipalViewModel
import com.frentefria.ui.Secao
import com.frentefria.ui.Tela
import com.frentefria.ui.curta
import com.frentefria.ui.completa
import com.frentefria.ui.onboarding.Opcao
import com.frentefria.ui.trava.TecladoPin
import com.frentefria.ui.trava.biometriaDisponivel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun ConfigTela(e: Estado, vm: PrincipalViewModel, aoVoltar: () -> Unit, aoAbrirCiclos: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val app = context.applicationContext as FrenteFriaApp
    val p = app.preferencias
    val escopo = rememberCoroutineScope()
    val a = e.ajustes
    val t = termos(a.modo)
    var criandoPin by remember { mutableStateOf(false) }
    var apagando by remember { mutableStateOf(false) }
    var apelido by remember { mutableStateOf(a.apelido) }

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        escopo.launch {
            val r = app.repositorio
            val b = Backup(
                geradoEm = LocalDateTime.now().toString(),
                menstruacoes = r.menstruacoes.first(),
                registros = r.registros.first().values.toList(),
                eventos = r.eventos.first(),
                kit = r.kit.first(),
                preferencias = PreferenciasBackup(a.modo.name, a.apelido, a.config.duracaoCicloPadrao, a.config.duracaoMenstruacaoPadrao, a.config.tpmFixa),
            )
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(b.paraJson().toByteArray()) } }
            Toast.makeText(context, "Backup salvo", Toast.LENGTH_SHORT).show()
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        escopo.launch {
            try {
                val texto = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } } ?: return@launch
                val b = Backup.deJson(texto)
                app.repositorio.restaurar(b)
                p.mudarApelido(b.preferencias.apelido)
                p.mudarCiclo(b.preferencias.duracaoCicloPadrao)
                p.mudarMenstruacao(b.preferencias.duracaoMenstruacaoPadrao)
                p.mudarTpmFixa(b.preferencias.tpmFixa)
                apelido = b.preferencias.apelido
                Toast.makeText(context, "Restaurado: ${b.menstruacoes.size} inícios, ${b.registros.size} registros", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(context, "Arquivo inválido", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (criandoPin) {
        var primeiro by remember { mutableStateOf<String?>(null) }
        var erro by remember { mutableStateOf<String?>(null) }
        Surface(Modifier.fillMaxSize()) {
            TecladoPin(
                titulo = if (primeiro == null) "Novo PIN" else "Confirme o PIN",
                subtitulo = "4 a 6 números",
                erro = erro,
                aoCompletar = { pin ->
                    when {
                        primeiro == null -> primeiro = pin
                        primeiro == pin -> { escopo.launch { app.trava.definir(pin) }; criandoPin = false }
                        else -> { primeiro = null; erro = "Não bateu. De novo." }
                    }
                },
            ) { TextButton(onClick = { criandoPin = false }) { Text("Cancelar") } }
        }
        return
    }

    Tela("Configurações", aoVoltar = aoVoltar) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Secao("Disfarce")
            Opcao(a.modo == Modo.CLIMA, "☁️ Previsão do tempo", "Ícone de nuvem e tudo em meteorologia.") {
                escopo.launch { p.mudarModo(Modo.CLIMA); Icone.aplicar(context, Modo.CLIMA, reabrir = activity) }
            }
            Opcao(a.modo == Modo.DIRETO, "⚡ Direto", "Fala tudo com todas as letras.") {
                escopo.launch { p.mudarModo(Modo.DIRETO); Icone.aplicar(context, Modo.DIRETO, reabrir = activity) }
            }
            Text("O app reabre com o ícone novo. Se você tinha um atalho na tela inicial, alguns launchers tiram: arraste de novo da gaveta de apps.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Secao("Trava")
            if (a.travaLigada) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PIN ligado", Modifier.weight(1f))
                    TextButton(onClick = { criandoPin = true }) { Text("Trocar") }
                    TextButton(onClick = { escopo.launch { app.trava.remover() } }) { Text("Desligar") }
                }
                if (biometriaDisponivel(activity)) Linha("Liberar com a digital", a.biometria) { escopo.launch { p.mudarBiometria(it) } }
            } else {
                OutlinedButton(onClick = { criandoPin = true }) { Text("Criar PIN") }
            }

            Secao("Ela")
            OutlinedTextField(
                apelido, { apelido = it.take(20); escopo.launch { p.mudarApelido(apelido) } },
                label = { Text("Apelido") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Secao(if (a.modo == Modo.CLIMA) "Estação" else "Ciclo")
            val c = a.config
            Text("Duração padrão ${t.doCiclo}: ${c.duracaoCicloPadrao} dias", fontWeight = FontWeight.Medium)
            Slider(c.duracaoCicloPadrao.toFloat(), { escopo.launch { p.mudarCiclo(it.toInt()) } }, valueRange = 21f..40f, steps = 18)
            Text("Vale enquanto houver menos de 3 medições; depois manda o histórico.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Duração padrão da ${t.menstruacao}: ${c.duracaoMenstruacaoPadrao} dias", fontWeight = FontWeight.Medium)
            Slider(c.duracaoMenstruacaoPadrao.toFloat(), { escopo.launch { p.mudarMenstruacao(it.toInt()) } }, valueRange = 2f..10f, steps = 7)
            Text("Duração da ${t.tpm}: " + (c.tpmFixa?.let { "$it dias (fixa)" } ?: "automática (${e.situacao.previsao?.diasTpm ?: 7} dias)"), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(c.tpmFixa == null, { escopo.launch { p.mudarTpmFixa(null) } }, label = { Text("Automática") })
                FilterChip(c.tpmFixa != null, { escopo.launch { p.mudarTpmFixa(e.situacao.previsao?.diasTpm ?: 7) } }, label = { Text("Fixar") })
            }
            if (c.tpmFixa != null) Slider(c.tpmFixa!!.toFloat(), { escopo.launch { p.mudarTpmFixa(it.toInt()) } }, valueRange = TPM_MINIMA.toFloat()..TPM_MAXIMA.toFloat(), steps = TPM_MAXIMA - TPM_MINIMA - 1)
            OutlinedButton(onClick = aoAbrirCiclos, modifier = Modifier.fillMaxWidth()) { Text(if (a.modo == Modo.CLIMA) "Histórico de chuvas" else "Histórico de menstruações") }

            Secao("Avisos")
            Text("Resumo da manhã", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(6, 7, 8, 9, 10).forEach { h ->
                    FilterChip(a.manha.hour == h, { escopo.launch { p.mudarHorario(Horario.MANHA, LocalTime.of(h, 0)); app.agendador.reagendar(Horario.MANHA) } }, label = { Text("${h}h") })
                }
            }
            Linha("Resumo todo dia (não só nos dias de alerta)", a.resumoTodoDia) { escopo.launch { p.mudarResumoTodoDia(it) } }
            Linha(if (a.modo == Modo.CLIMA) "Lembrete à noite nos dias de frente fria" else "Lembrete à noite na TPM", a.lembreteNoite) { escopo.launch { p.mudarLembreteNoite(it) } }
            if (a.lembreteNoite) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(20, 21, 22, 23).forEach { h ->
                    FilterChip(a.noite.hour == h, { escopo.launch { p.mudarHorario(Horario.NOITE, LocalTime.of(h, 0)); app.agendador.reagendar(Horario.NOITE) } }, label = { Text("${h}h") })
                }
            }
            OutlinedButton(onClick = {
                if (!app.avisos.ligadas()) Toast.makeText(context, "Notificações desligadas nas configurações do Android", Toast.LENGTH_LONG).show()
                app.agendador.testarAgora()
            }) { Text("Testar agora") }

            Secao("Backup")
            Text("Um arquivo JSON com tudo, salvo onde você escolher. Guarde num lugar que ela não abre.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportar.launch("previsao-${LocalDate.now()}.json") }) { Text("Exportar") }
                OutlinedButton(onClick = { importar.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Importar") }
            }

            Secao("Zona de perigo")
            OutlinedButton(onClick = { apagando = true }) { Text("Apagar tudo", color = MaterialTheme.colorScheme.error) }
            Text(
                "Frente Fria ${vm.versaoInstalada} · toque para procurar versão nova\nSeus registros nunca saem deste celular; a internet é só para as atualizações.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp).clickable {
                    vm.procurarAtualizacao { nova ->
                        Toast.makeText(context, nova?.let { "A $it está disponível: o cartão está na tela inicial." } ?: "Você já está na última versão.", Toast.LENGTH_LONG).show()
                    }
                },
            )
        }
    }

    if (apagando) {
        AlertDialog(
            onDismissRequest = { apagando = false },
            title = { Text("Apagar tudo?") },
            text = { Text("Some todo o histórico, registros, kit e configurações. Não dá para desfazer (a não ser que você tenha um backup).") },
            confirmButton = {
                TextButton(onClick = {
                    escopo.launch {
                        app.agendador.cancelarTudo()
                        app.repositorio.limparTudo()
                        app.trava.remover()
                        app.preferencias.limparTudo()
                        Icone.aplicar(context, Modo.CLIMA)
                    }
                    apagando = false
                }) { Text("Apagar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { apagando = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Linha(texto: String, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { aoMudar(!ligado) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, Modifier.weight(1f))
        Switch(ligado, aoMudar)
    }
}

/** Lista dos inícios registrados, com a duração de cada ciclo, para corrigir enganos. */
@Composable
fun CiclosTela(e: Estado, vm: PrincipalViewModel, aoVoltar: () -> Unit) {
    val modo = e.ajustes.modo
    val clima = modo == Modo.CLIMA
    val ms = e.situacao.dados.menstruacoes.sortedByDescending { it.inicio }
    var apagar by remember { mutableStateOf<LocalDate?>(null) }

    Tela(if (clima) "Histórico de chuvas" else "Histórico", aoVoltar = aoVoltar) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Para adicionar uma data antiga, toque no dia no calendário.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            ms.forEachIndexed { i, m ->
                val seguinte = ms.getOrNull(i - 1)
                val duracao = seguinte?.let { dias(m.inicio, it.inicio) }
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.inicio.completa(), fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(
                                m.fim?.let { "${dias(m.inicio, it) + 1} dias de " + (if (clima) "chuva" else "menstruação") },
                                duracao?.let { "${if (clima) "estação" else "ciclo"} de $it dias" + if (it !in 18..50) " (fora da média)" else "" },
                            ).joinToString(" · ").ifEmpty { if (i == 0) "atual" else "" },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (m.fim != null) TextButton(onClick = { vm.apagarFim(m.inicio) }) { Text("Tirar fim") }
                    IconButton(onClick = { apagar = m.inicio }) { Icon(Icons.Filled.Delete, "Apagar") }
                }
                HorizontalDivider()
            }
        }
    }
    apagar?.let { data ->
        AlertDialog(
            onDismissRequest = { apagar = null },
            title = { Text("Apagar ${data.curta()}?") },
            text = { Text("A previsão é recalculada sem essa data.") },
            confirmButton = { TextButton(onClick = { vm.apagarMenstruacao(data); apagar = null }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { apagar = null }) { Text("Cancelar") } },
        )
    }
}
