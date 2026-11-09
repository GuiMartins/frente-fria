package com.frentefria.motor.ciclo

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Uma menstruação registrada: o dia em que começou e, se ele marcou, o último dia. */
@Serializable
data class Menstruacao(
    @Serializable(with = DataSerializer::class) val inicio: LocalDate,
    @Serializable(with = DataSerializer::class) val fim: LocalDate? = null,
)

/** O que ele informou no onboarding e nas configurações. */
data class ConfigCiclo(
    val duracaoCicloPadrao: Int = 28,
    val duracaoMenstruacaoPadrao: Int = 5,
    /** Duração da TPM fixada à mão; null deixa o app aprender. */
    val tpmFixa: Int? = null,
)

enum class Fase { MENSTRUACAO, FOLICULAR, OVULACAO, LUTEA, TPM, ATRASADA }

/** A previsão do próximo ciclo, a partir do histórico. */
data class Previsao(
    val ultimoInicio: LocalDate,
    /** Média ponderada da duração do ciclo, em dias. */
    val duracaoMedia: Double,
    val desvio: Double,
    val duracaoMenstruacao: Int,
    val diasTpm: Int,
    /** Quantos ciclos completos entraram na média (0 = só o padrão informado). */
    val ciclosUsados: Int,
) {
    val duracaoArredondada: Int get() = duracaoMedia.roundToInt()
    val proximoInicio: LocalDate get() = ultimoInicio.plusDays(duracaoArredondada.toLong())

    /** Meia largura da janela de incerteza, em dias: pelo menos 1, no máximo 7. */
    val margem: Int get() = desvio.roundToInt().coerceIn(1, 7)
    val janelaInicio: LocalDate get() = proximoInicio.minusDays(margem.toLong())
    val janelaFim: LocalDate get() = proximoInicio.plusDays(margem.toLong())
}

/** Tudo o que o app precisa saber sobre um dia. */
data class Dia(
    val data: LocalDate,
    val fase: Fase,
    /** 1 = primeiro dia da menstruação. */
    val diaDoCiclo: Int,
    /** Dias até o próximo início (0 = hoje; negativo = atrasada). */
    val diasAteProximo: Int,
    /** O ciclo deste dia é projeção (ainda não aconteceu). */
    val projetado: Boolean,
    val diasAtraso: Int,
    /** Janela fértil estimada (só informação; não serve como anticoncepcional). */
    val fertil: Boolean,
    val inicioDoCiclo: LocalDate,
    val proximoInicio: LocalDate,
) {
    /** Dia relativo ao próximo início: -1 é a véspera. */
    val diaRelativo: Int get() = -diasAteProximo
}

const val CICLO_MINIMO = 18
const val CICLO_MAXIMO = 50
private const val CICLOS_NA_MEDIA = 6

fun dias(de: LocalDate, ate: LocalDate): Int = ChronoUnit.DAYS.between(de, ate).toInt()

/** Durações dos ciclos completos, do mais antigo ao mais recente, sem os fora da faixa plausível. */
fun duracoesValidas(menstruacoes: List<Menstruacao>): List<Int> =
    menstruacoes.map { it.inicio }.sorted().distinct().zipWithNext { a, b -> dias(a, b) }
        .filter { it in CICLO_MINIMO..CICLO_MAXIMO }

/**
 * Média ponderada dos últimos 6 ciclos: o mais recente pesa 6, o mais antigo 1.
 * Com menos de 3 ciclos, o padrão informado entra como mais um ciclo (peso 1),
 * para um ciclo esquisito não estragar a previsão logo no começo.
 */
fun prever(menstruacoes: List<Menstruacao>, config: ConfigCiclo, diasTpm: Int = config.tpmFixa ?: TPM_PADRAO): Previsao? {
    val ordenadas = menstruacoes.sortedBy { it.inicio }
    val ultimo = ordenadas.lastOrNull() ?: return null
    val duracoes = duracoesValidas(ordenadas).takeLast(CICLOS_NA_MEDIA)
    val amostra = buildList {
        if (duracoes.size < 3) add(config.duracaoCicloPadrao.toDouble() to 1.0)
        duracoes.forEachIndexed { i, d -> add(d.toDouble() to (i + 1).toDouble()) }
    }
    val pesoTotal = amostra.sumOf { it.second }
    val media = amostra.sumOf { it.first * it.second } / pesoTotal
    val desvio = if (duracoes.size < 2) 2.0 else sqrt(amostra.sumOf { (d, p) -> p * (d - media) * (d - media) } / pesoTotal)
    val sangramentos = ordenadas.takeLast(CICLOS_NA_MEDIA).mapNotNull { m -> m.fim?.let { dias(m.inicio, it) + 1 } }
        .filter { it in 1..12 }
    val menstruacao = if (sangramentos.isEmpty()) config.duracaoMenstruacaoPadrao else sangramentos.average().roundToInt()
    return Previsao(ultimo.inicio, media, desvio, menstruacao, diasTpm, duracoes.size)
}

const val TPM_PADRAO = 7

/**
 * A fase de um dia qualquer. Dias até hoje usam o que foi registrado: se o
 * próximo início previsto passou sem registro, é ATRASADA. Dias depois de hoje
 * projetam ciclos com a duração média; se hoje está atrasada, o próximo ciclo
 * projetado começa amanhã.
 */
fun diaEm(data: LocalDate, hoje: LocalDate, menstruacoes: List<Menstruacao>, previsao: Previsao): Dia? {
    val ordenadas = menstruacoes.sortedBy { it.inicio }
    val atual = ordenadas.lastOrNull { !it.inicio.isAfter(data) } ?: return null
    val seguinte = ordenadas.firstOrNull { it.inicio.isAfter(data) }
    val duracao = previsao.duracaoArredondada

    if (seguinte != null) return montar(data, atual, seguinte.inicio, false, 0, previsao)

    val proximoPrevisto = atual.inicio.plusDays(duracao.toLong())
    if (!data.isAfter(hoje)) {
        // No dia previsto ainda é TPM ("prevista para hoje"); atrasada é a partir do dia seguinte.
        return if (!data.isAfter(proximoPrevisto)) {
            montar(data, atual, proximoPrevisto, false, 0, previsao)
        } else {
            val atraso = dias(proximoPrevisto, data)
            Dia(data, Fase.ATRASADA, dias(atual.inicio, data) + 1, -atraso, false, atraso, false, atual.inicio, proximoPrevisto)
        }
    }

    // Futuro: o primeiro ciclo projetado começa no previsto, ou amanhã se já atrasou.
    var inicio = if (proximoPrevisto.isAfter(hoje)) proximoPrevisto else hoje.plusDays(1)
    if (data.isBefore(inicio)) return montar(data, atual, inicio, false, 0, previsao)
    while (!data.isBefore(inicio.plusDays(duracao.toLong()))) inicio = inicio.plusDays(duracao.toLong())
    return montar(data, Menstruacao(inicio), inicio.plusDays(duracao.toLong()), true, 0, previsao)
}

private fun montar(data: LocalDate, ciclo: Menstruacao, proximo: LocalDate, projetado: Boolean, atraso: Int, p: Previsao): Dia {
    val diaDoCiclo = dias(ciclo.inicio, data) + 1
    val ateProximo = dias(data, proximo)
    val fimMenstruacao = ciclo.fim?.let { dias(ciclo.inicio, it) + 1 } ?: p.duracaoMenstruacao
    val ovulacao = proximo.minusDays(14)
    val distOvulacao = dias(ovulacao, data)
    val fase = when {
        diaDoCiclo <= fimMenstruacao -> Fase.MENSTRUACAO
        ateProximo <= p.diasTpm -> Fase.TPM
        abs(distOvulacao) <= 1 -> Fase.OVULACAO
        distOvulacao < 0 -> Fase.FOLICULAR
        else -> Fase.LUTEA
    }
    // Janela fértil clássica: 5 dias antes da ovulação até 1 depois.
    val fertil = distOvulacao in -5..1 && fase != Fase.MENSTRUACAO
    return Dia(data, fase, diaDoCiclo, ateProximo, projetado, atraso, fertil, ciclo.inicio, proximo)
}

/** A menstruação que está acontecendo hoje (começou e ainda não terminou), se houver. */
fun menstruacaoEmCurso(hoje: LocalDate, menstruacoes: List<Menstruacao>, previsao: Previsao): Menstruacao? {
    val ultima = menstruacoes.maxByOrNull { it.inicio } ?: return null
    if (ultima.inicio.isAfter(hoje) || ultima.fim != null) return null
    return ultima.takeIf { dias(it.inicio, hoje) < maxOf(previsao.duracaoMenstruacao + 3, 8) }
}
