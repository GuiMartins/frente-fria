package com.frentefria.ui.kit

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frentefria.motor.agenda.Evento
import com.frentefria.motor.agenda.TipoEvento
import com.frentefria.motor.agenda.conflita
import com.frentefria.motor.agenda.emojiDoTipo
import com.frentefria.motor.agenda.nomeDoTipo
import com.frentefria.motor.kit.CategoriaFavorito
import com.frentefria.motor.kit.ItemKit
import com.frentefria.motor.kit.emojiDaCategoria
import com.frentefria.motor.kit.nomeDaCategoria
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.emojiDoNivel
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDoNivel
import com.frentefria.ui.Cartao
import com.frentefria.ui.EscolherData
import com.frentefria.ui.Estado
import com.frentefria.ui.PrincipalViewModel
import com.frentefria.ui.Secao
import com.frentefria.ui.Tela
import com.frentefria.ui.curta
import com.frentefria.ui.completa
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitTela(e: Estado, vm: PrincipalViewModel) {
    val modo = e.ajustes.modo
    val clima = modo == Modo.CLIMA
    val nome = e.ajustes.nomeDela
    var novoFavorito by remember { mutableStateOf(false) }
    var novoEstoque by remember { mutableStateOf(false) }
    var novoEvento by remember { mutableStateOf(false) }

    Tela(if (clima) "Kit e agenda" else "Kit de sobrevivência") { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Secao(if (clima) "Despensa (tem em casa?)" else "Estoque (tem em casa?)")
            Text(if (clima) "Aviso 3 dias antes da frente fria se faltar algo." else "Aviso 3 dias antes da TPM se faltar algo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            e.kit.filter { it.estoque }.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(item.tem, { vm.salvarItem(item.copy(tem = it)) })
                    Text("${emojiDaCategoria(item.categoria)} ${item.nome}", Modifier.weight(1f), color = if (item.tem) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                    IconButton(onClick = { vm.apagarItem(item.id) }) { Icon(Icons.Filled.Close, "Tirar") }
                }
            }
            OutlinedButton(onClick = { novoEstoque = true }) { Text("+ Item no estoque") }

            Secao("Favoritos de $nome")
            Text("Para não precisar perguntar na hora H.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val favoritos = e.kit.filter { !it.estoque }.groupBy { it.categoria }
            if (favoritos.isEmpty()) Text("Nenhum ainda. Qual o chocolate preferido dela? Se você não sabe, já achamos o problema.", modifier = Modifier.padding(vertical = 8.dp))
            CategoriaFavorito.entries.forEach { c ->
                val itens = favoritos[c] ?: return@forEach
                Text("${emojiDaCategoria(c)} ${nomeDaCategoria(c)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                itens.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("   ${item.nome}", Modifier.weight(1f))
                        IconButton(onClick = { vm.apagarItem(item.id) }) { Icon(Icons.Filled.Close, "Tirar") }
                    }
                }
            }
            OutlinedButton(onClick = { novoFavorito = true }) { Text("+ Favorito") }

            Secao("Agenda")
            Text(if (clima) "Marque eventos e veja a previsão do tempo para cada um." else "Marque eventos e veja se caem num dia difícil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val s = e.situacao
            s.dados.eventos.filter { !it.data.isBefore(s.hoje) }.forEach { ev ->
                val dn = s.dia(ev.data)
                val ruim = dn != null && conflita(dn.dia, dn.nivel)
                Cartao(Modifier.padding(vertical = 4.dp), cor = if (ruim) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${emojiDoTipo(ev.tipo)} ${ev.titulo}", fontWeight = FontWeight.SemiBold)
                            Text(ev.data.curta() + (dn?.let { " · ${emojiDoNivel(it.nivel)} ${nomeDoNivel(it.nivel, modo)} · ${nomeDaFase(it.dia.fase, modo)}" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.apagarEvento(ev.id) }) { Icon(Icons.Filled.Close, "Apagar") }
                    }
                }
            }
            OutlinedButton(onClick = { novoEvento = true }) { Text("+ Evento") }
        }
    }

    if (novoEstoque) NovoItem(estoque = true, aoSalvar = { vm.salvarItem(it) }, aoFechar = { novoEstoque = false })
    if (novoFavorito) NovoItem(estoque = false, aoSalvar = { vm.salvarItem(it) }, aoFechar = { novoFavorito = false })
    if (novoEvento) NovoEvento(aoSalvar = { vm.salvarEvento(it) }, aoFechar = { novoEvento = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovoItem(estoque: Boolean, aoSalvar: (ItemKit) -> Unit, aoFechar: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CategoriaFavorito.CHOCOLATE) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (estoque) "Item no estoque" else "Favorito") },
        text = {
            Column {
                OutlinedTextField(nome, { nome = it }, label = { Text(if (estoque) "O quê" else "Qual (ex.: Kit Kat branco)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    CategoriaFavorito.entries.forEach { c ->
                        FilterChip(categoria == c, { categoria = c }, label = { Text("${emojiDaCategoria(c)} ${nomeDaCategoria(c)}") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { aoSalvar(ItemKit(nome = nome.trim(), estoque = estoque, categoria = categoria, tem = !estoque)); aoFechar() }, enabled = nome.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovoEvento(aoSalvar: (Evento) -> Unit, aoFechar: () -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoEvento.JANTAR) }
    var data by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var escolhendo by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Evento") },
        text = {
            Column {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("O quê") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { escolhendo = true }, modifier = Modifier.padding(top = 8.dp)) { Text(data.completa()) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    TipoEvento.entries.forEach { t ->
                        FilterChip(tipo == t, { tipo = t; if (titulo.isBlank() || TipoEvento.entries.any { nomeDoTipo(it) == titulo }) titulo = nomeDoTipo(t) }, label = { Text("${emojiDoTipo(t)} ${nomeDoTipo(t)}") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { aoSalvar(Evento(data = data, titulo = titulo.trim(), tipo = tipo)); aoFechar() }, enabled = titulo.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
    if (escolhendo) EscolherData(data, { data = it }, { escolhendo = false })
}
