package com.frentefria.dados

import com.frentefria.motor.agenda.Evento
import com.frentefria.motor.agenda.TipoEvento
import com.frentefria.motor.backup.Backup
import com.frentefria.motor.ciclo.Menstruacao
import com.frentefria.motor.kit.CategoriaFavorito
import com.frentefria.motor.kit.ItemKit
import com.frentefria.motor.registro.Humor
import com.frentefria.motor.registro.MotivoDr
import com.frentefria.motor.registro.RegistroDia
import com.frentefria.motor.registro.Sintoma
import com.frentefria.motor.registro.Tatica
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Traduz entre o banco (texto) e os modelos do motor. */
class Repositorio(private val dao: FrenteFriaDao) {

    val menstruacoes: Flow<List<Menstruacao>> = dao.menstruacoes().map { l -> l.map { it.modelo() } }
    val registros: Flow<Map<LocalDate, RegistroDia>> = dao.registros().map { l -> l.map { it.modelo() }.associateBy { it.data } }
    val eventos: Flow<List<Evento>> = dao.eventos().map { l -> l.map { it.modelo() } }
    val kit: Flow<List<ItemKit>> = dao.kit().map { l -> l.map { it.modelo() } }

    /** "Menstruou hoje": se já existe um início perto (3 dias), corrige a data em vez de duplicar. */
    suspend fun registrarInicio(data: LocalDate) {
        val atuais = menstruacoes.first()
        atuais.filter { kotlin.math.abs(it.inicio.toEpochDay() - data.toEpochDay()) in 1..3 }
            .forEach { dao.apagarMenstruacao(it.inicio.toString()) }
        val existente = atuais.firstOrNull { it.inicio == data }
        dao.salvarMenstruacao(MenstruacaoEntity(data.toString(), existente?.fim?.toString()))
    }

    /** "Parou hoje": marca o fim na menstruação mais recente que começou antes. */
    suspend fun registrarFim(data: LocalDate) {
        val m = menstruacoes.first().lastOrNull { !it.inicio.isAfter(data) } ?: return
        dao.salvarMenstruacao(MenstruacaoEntity(m.inicio.toString(), data.toString()))
    }

    suspend fun apagarFim(inicio: LocalDate) = dao.salvarMenstruacao(MenstruacaoEntity(inicio.toString(), null))

    suspend fun apagarMenstruacao(inicio: LocalDate) = dao.apagarMenstruacao(inicio.toString())

    suspend fun registro(data: LocalDate): RegistroDia? = dao.registro(data.toString())?.modelo()

    suspend fun salvarRegistro(r: RegistroDia) {
        if (r.vazio) dao.apagarRegistro(r.data.toString()) else dao.salvarRegistro(r.entidade())
    }

    suspend fun apagarRegistro(data: LocalDate) = dao.apagarRegistro(data.toString())

    suspend fun salvarEvento(e: Evento) = dao.salvarEvento(EventoEntity(e.id, e.data.toString(), e.titulo, e.tipo.name))
    suspend fun apagarEvento(id: Long) = dao.apagarEvento(id)

    suspend fun salvarItem(i: ItemKit) = dao.salvarItem(ItemKitEntity(i.id, i.nome, i.estoque, i.categoria.name, i.tem))
    suspend fun apagarItem(id: Long) = dao.apagarItem(id)

    /** Só na primeira vez: o kit nasce com o estoque sugerido. */
    suspend fun semearKit(itens: List<ItemKit>) {
        if (dao.contarKit() == 0) itens.forEach { salvarItem(it) }
    }

    suspend fun limparTudo() = dao.limparTudo()

    suspend fun restaurar(b: Backup) = dao.substituirTudo(
        b.menstruacoes.map { MenstruacaoEntity(it.inicio.toString(), it.fim?.toString()) },
        b.registros.map { it.entidade() },
        b.eventos.map { EventoEntity(0, it.data.toString(), it.titulo, it.tipo.name) },
        b.kit.map { ItemKitEntity(0, it.nome, it.estoque, it.categoria.name, it.tem) },
    )
}

private fun MenstruacaoEntity.modelo() = Menstruacao(LocalDate.parse(inicio), fim?.let(LocalDate::parse))

private inline fun <reified E : Enum<E>> nomes(texto: String): Set<E> =
    texto.split(',').filter { it.isNotBlank() }.mapNotNull { n -> enumValues<E>().firstOrNull { it.name == n } }.toSet()

private fun RegistroEntity.modelo() = RegistroDia(
    data = LocalDate.parse(data),
    humor = humor?.let { h -> Humor.entries.firstOrNull { it.name == h } },
    sintomas = nomes<Sintoma>(sintomas),
    dr = dr,
    motivos = nomes<MotivoDr>(motivos),
    taticas = taticas.split(',').filter { it.isNotBlank() }.mapNotNull { par ->
        val (nome, valor) = par.split(':').let { it[0] to it.getOrElse(1) { "" } }
        Tatica.entries.firstOrNull { it.name == nome }?.let { t -> t to when (valor) { "1" -> true; "0" -> false; else -> null } }
    }.toMap(),
    nota = nota,
)

private fun RegistroDia.entidade() = RegistroEntity(
    data = data.toString(),
    humor = humor?.name,
    sintomas = sintomas.joinToString(",") { it.name },
    dr = dr,
    motivos = motivos.joinToString(",") { it.name },
    taticas = taticas.entries.joinToString(",") { (t, v) -> "${t.name}:${when (v) { true -> "1"; false -> "0"; null -> "" }}" },
    nota = nota,
)

private fun EventoEntity.modelo() = Evento(id, LocalDate.parse(data), titulo, TipoEvento.entries.firstOrNull { it.name == tipo } ?: TipoEvento.OUTRO)

private fun ItemKitEntity.modelo() = ItemKit(id, nome, estoque, CategoriaFavorito.entries.firstOrNull { it.name == categoria } ?: CategoriaFavorito.OUTRO, tem)
