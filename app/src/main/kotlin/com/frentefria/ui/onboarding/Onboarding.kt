package com.frentefria.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.frentefria.FrenteFriaApp
import com.frentefria.Icone
import com.frentefria.motor.avisos.Horario
import com.frentefria.motor.kit.estoqueInicial
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.termos
import com.frentefria.ui.EscolherData
import com.frentefria.ui.curta
import com.frentefria.ui.completa
import com.frentefria.ui.trava.TecladoPin
import com.frentefria.ui.trava.biometriaDisponivel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private const val PASSOS = 7

@Composable
fun Onboarding() {
    val context = LocalContext.current
    val app = context.applicationContext as FrenteFriaApp
    val activity = context as FragmentActivity
    val escopo = rememberCoroutineScope()

    var passo by remember { mutableIntStateOf(0) }
    var modo by remember { mutableStateOf(Modo.CLIMA) }
    var querTrava by remember { mutableStateOf(true) }
    var pin by remember { mutableStateOf<String?>(null) }
    var biometria by remember { mutableStateOf(true) }
    var apelido by remember { mutableStateOf("") }
    var ultima by remember { mutableStateOf<LocalDate?>(null) }
    var duracaoCiclo by remember { mutableIntStateOf(28) }
    var duracaoMenstruacao by remember { mutableIntStateOf(5) }
    val anteriores = remember { mutableStateListOf<LocalDate>() }
    var manha by remember { mutableIntStateOf(8) }
    var lembreteNoite by remember { mutableStateOf(true) }
    val t = termos(modo)
    val clima = modo == Modo.CLIMA

    fun concluir() = escopo.launch {
        val p = app.preferencias
        p.mudarModo(modo)
        p.mudarApelido(apelido)
        p.mudarCiclo(duracaoCiclo)
        p.mudarMenstruacao(duracaoMenstruacao)
        p.mudarHorario(Horario.MANHA, LocalTime.of(manha, 0))
        p.mudarLembreteNoite(lembreteNoite)
        if (querTrava && pin != null) {
            app.trava.definir(pin!!)
            p.mudarBiometria(biometria && biometriaDisponivel(activity))
        }
        (listOfNotNull(ultima) + anteriores).distinct().forEach { app.repositorio.registrarInicio(it) }
        app.repositorio.semearKit(estoqueInicial(modo))
        p.concluirOnboarding()
        app.agendador.garantirAgendado()
        Icone.aplicar(context, modo, reabrir = activity)
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        LinearProgressIndicator(progress = { (passo + 1f) / PASSOS }, modifier = Modifier.fillMaxWidth())
        AnimatedContent(passo, label = "passo", modifier = Modifier.weight(1f)) { atual ->
            when (atual) {
                0 -> Passo(
                    emoji = "🌦️",
                    titulo = "Frente Fria",
                    texto = "A previsão do tempo do seu relacionamento.\n\nO app acompanha o ciclo dela, avisa quando o tempo vai fechar, diz o que fazer (e principalmente o que NÃO fazer) e aprende com o que você anota.\n\nTudo fica só neste celular. Nada vai para a internet.",
                )
                1 -> Passo("🎭", "Disfarce", "Como o app aparece no celular? Dá para trocar depois.") {
                    Opcao(clima, "☁️ Previsão do tempo", "Ícone de nuvem. Tudo vira meteorologia: frente fria, temporada de chuvas, tempestade. Se ela vir uma notificação, é só a previsão.") { modo = Modo.CLIMA }
                    Opcao(!clima, "⚡ Direto", "Fala TPM, menstruação e ciclo com todas as letras. Para quem não tem nada a esconder (ou é corajoso).") { modo = Modo.DIRETO }
                }
                2 -> if (querTrava && pin == null) {
                    var primeiro by remember { mutableStateOf<String?>(null) }
                    var erro by remember { mutableStateOf<String?>(null) }
                    TecladoPin(
                        titulo = if (primeiro == null) "Crie um PIN" else "Confirme o PIN",
                        subtitulo = "4 a 6 números. Use um diferente do PIN do celular.",
                        erro = erro,
                        aoCompletar = { digitado ->
                            when {
                                primeiro == null -> { primeiro = digitado; erro = null }
                                primeiro == digitado -> pin = digitado
                                else -> { primeiro = null; erro = "Não bateu. De novo." }
                            }
                        },
                    ) { TextButton(onClick = { querTrava = false }) { Text("Não quero trava") } }
                } else {
                    Passo("🔒", "Trava", if (pin != null) "PIN criado." else "Sem trava: qualquer um que abrir o app vê tudo.") {
                        Opcao(querTrava, "PIN do app", "Pede o PIN ao abrir e esconde o app na tela de recentes.") { querTrava = true }
                        Opcao(!querTrava, "Sem trava", "Abre direto.") { querTrava = false; pin = null }
                        if (querTrava && pin != null && biometriaDisponivel(activity)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Liberar também com a digital", Modifier.weight(1f))
                                Switch(biometria, { biometria = it })
                            }
                            Text("Atenção: se a digital dela estiver cadastrada no seu celular, ela também abre.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                3 -> Passo("💁‍♀️", "Como chamar ela?", "Aparece nos textos do app. Pode ser o nome, um apelido ou deixar em branco (fica \"ela\").") {
                    OutlinedTextField(apelido, { apelido = it.take(20) }, label = { Text("Apelido") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                4 -> {
                    var escolhendo by remember { mutableStateOf(false) }
                    Passo(if (clima) "🌧️" else "🩸", if (clima) "Última chuva" else "Última menstruação", if (clima) "Quando começou a última temporada de chuvas? Se não lembra exatamente, chute: o app corrige com o tempo." else "Quando começou a última? Se não lembra exatamente, chute: o app corrige com o tempo.") {
                        OutlinedButton(onClick = { escolhendo = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(ultima?.let { "Começou em ${it.curta()}" } ?: "Escolher a data")
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Duração ${t.doCiclo}: $duracaoCiclo dias", fontWeight = FontWeight.Medium)
                        Slider(duracaoCiclo.toFloat(), { duracaoCiclo = it.toInt() }, valueRange = 21f..40f, steps = 18)
                        Text(if (clima) "Do início de uma chuva até o início da próxima. Não sabe? Deixe 28." else "Do primeiro dia de uma menstruação até o primeiro da próxima. Não sabe? Deixe 28.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Text("Quantos dias dura a ${t.menstruacao}: $duracaoMenstruacao", fontWeight = FontWeight.Medium)
                        Slider(duracaoMenstruacao.toFloat(), { duracaoMenstruacao = it.toInt() }, valueRange = 2f..10f, steps = 7)
                    }
                    if (escolhendo) EscolherData(ultima ?: LocalDate.now(), { ultima = it }, { escolhendo = false }, ate = LocalDate.now())
                }
                5 -> {
                    var escolhendo by remember { mutableStateOf(false) }
                    Passo("📅", "Datas anteriores (opcional)", if (clima) "Lembra de outras chuvas? Cada data a mais melhora a previsão desde já. Dá para adicionar depois no calendário." else "Lembra de outras datas? Cada uma melhora a previsão desde já. Dá para adicionar depois no calendário.") {
                        anteriores.sortedDescending().forEach { d ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.completa(), Modifier.weight(1f))
                                TextButton(onClick = { anteriores.remove(d) }) { Text("Tirar") }
                            }
                        }
                        OutlinedButton(onClick = { escolhendo = true }) { Text("+ Adicionar data") }
                    }
                    if (escolhendo) EscolherData((anteriores.minOrNull() ?: ultima ?: LocalDate.now()).minusDays(duracaoCiclo.toLong()), { if (it != ultima) anteriores.add(it) }, { escolhendo = false }, ate = LocalDate.now())
                }
                else -> {
                    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    Passo("🔔", "Avisos", if (clima) "O app avisa quando a frente fria se aproxima, quando abastecer a despensa e quando a chuva está prevista. As notificações só falam do tempo." else "O app avisa quando a TPM se aproxima, quando repor o kit e quando a menstruação está prevista.") {
                        if (Build.VERSION.SDK_INT >= 33) {
                            Button(onClick = { permissao.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Permitir notificações") }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Resumo da manhã às:", fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(6, 7, 8, 9, 10).forEach { h -> FilterChip(manha == h, { manha = h }, label = { Text("${h}h") }) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Lembrete à noite para registrar o dia")
                                Text(if (clima) "Só nos dias de frente fria, às 21h." else "Só na TPM, às 21h.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(lembreteNoite, { lembreteNoite = it })
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (passo > 0) TextButton(onClick = { passo-- }) { Text("Voltar") } else Spacer(Modifier)
            val podeAvancar = when (passo) {
                2 -> !querTrava || pin != null
                4 -> ultima != null
                else -> true
            }
            Button(onClick = { if (passo < PASSOS - 1) passo++ else concluir() }, enabled = podeAvancar) {
                Text(if (passo < PASSOS - 1) "Continuar" else "Começar")
            }
        }
    }
}

@Composable
private fun Passo(emoji: String, titulo: String, texto: String, conteudo: @Composable ColumnScope.() -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(emoji, fontSize = 56.sp)
        Text(titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        conteudo()
    }
}

@Composable
fun Opcao(selecionada: Boolean, titulo: String, texto: String, aoEscolher: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = aoEscolher),
        colors = CardDefaults.cardColors(
            containerColor = if (selecionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = if (selecionada) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
