package com.frentefria.motor.insights

import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.Menstruacao
import com.frentefria.motor.ciclo.Previsao
import com.frentefria.motor.ciclo.diaEm
import com.frentefria.motor.ciclo.dias
import com.frentefria.motor.ciclo.duracoesValidas
import com.frentefria.motor.ciclo.inicioDaTpmPorCiclo
import com.frentefria.motor.ciclo.prever
import com.frentefria.motor.registro.MotivoDr
import com.frentefria.motor.registro.RegistroDia
import com.frentefria.motor.registro.Sintoma
import com.frentefria.motor.registro.Tatica
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt

data class Regularidade(val ciclos: Int, val media: Double, val desvio: Double, val menor: Int, val maior: Int) {
    /** Até 3 dias de desvio é regular; até 6, mais ou menos; acima, irregular. */
    val classificacao: String get() = when {
        desvio <= 3 -> "regular"
        desvio <= 6 -> "mais ou menos regular"
        else -> "irregular"
    }
}

data class Acuracia(val previsoes: Int, val erroMedio: Double, val acertosEmUmDia: Int)

data class EstatisticaFase(
    val fase: Fase,
    val diasRegistrados: Int,
    val humorMedio: Double?,
    val drs: Int,
    /** DRs por dia registrado nessa fase. */
    val taxaDr: Double,
    val sintomas: List<Pair<Sintoma, Double>>,
)

data class EstatisticaTatica(val tatica: Tatica, val usos: Int, val funcionou: Int, val falhou: Int, val melhorFase: Fase?) {
    val avaliados: Int get() = funcionou + falhou
    val taxaSucesso: Double? get() = if (avaliados == 0) null else funcionou.toDouble() / avaliados
}

/** Um ponto do gráfico: dia relativo ao início da menstruação (-1 = véspera, 0 = primeiro dia). */
data class PontoHumor(val diaRelativo: Int, val humorMedio: Double, val amostras: Int)

data class Insights(
    val diasRegistrados: Int,
    val regularidade: Regularidade?,
    val acuracia: Acuracia?,
    val tpmPorCiclo: List<Int>,
    val porFase: Map<Fase, EstatisticaFase>,
    /** Quantas vezes mais DR na TPM do que no resto do ciclo; null sem dados. */
    val multiplicadorDrTpm: Double?,
    val taticas: List<EstatisticaTatica>,
    val motivos: List<Pair<MotivoDr, Int>>,
    val curvaHumor: List<PontoHumor>,
    val drsTotal: Int,
)

const val CURVA_ANTES = 14
const val CURVA_DEPOIS = 7

fun calcularInsights(
    menstruacoes: List<Menstruacao>,
    registros: Map<LocalDate, RegistroDia>,
    previsao: Previsao?,
    config: ConfigCiclo,
    hoje: LocalDate,
): Insights {
    val reais = registros.values.filter { !it.vazio && !it.data.isAfter(hoje) }
    val duracoes = duracoesValidas(menstruacoes)
    val regularidade = if (duracoes.size < 2) null else {
        val media = duracoes.average()
        Regularidade(duracoes.size, media, sqrt(duracoes.sumOf { (it - media) * (it - media) } / duracoes.size), duracoes.min(), duracoes.max())
    }

    val diasComFase = if (previsao == null) emptyList() else reais.mapNotNull { r -> diaEm(r.data, hoje, menstruacoes, previsao)?.let { r to it.fase } }
    // Atrasada conta como TPM nas estatísticas: é o mesmo clima.
    fun faseEstatistica(f: Fase) = if (f == Fase.ATRASADA) Fase.TPM else f
    val porFase = diasComFase.groupBy({ faseEstatistica(it.second) }, { it.first }).mapValues { (fase, rs) ->
        val humores = rs.mapNotNull { it.humor?.valor }
        val drs = rs.count { it.dr > 0 }
        val sintomas = rs.flatMap { it.sintomas }.groupingBy { it }.eachCount()
            .map { (s, n) -> s to n.toDouble() / rs.size }.sortedByDescending { it.second }.take(4)
        EstatisticaFase(fase, rs.size, humores.takeIf { it.isNotEmpty() }?.average(), drs, drs.toDouble() / rs.size, sintomas)
    }

    val tpm = porFase[Fase.TPM]
    val resto = porFase.filterKeys { it != Fase.TPM }.values
    val diasResto = resto.sumOf { it.diasRegistrados }
    val drsResto = resto.sumOf { it.drs }
    val multiplicador = if (tpm == null || tpm.diasRegistrados < 3 || diasResto < 5 || tpm.drs == 0) null else {
        // Sem DR fora da TPM, divide por meia DR para não dar infinito.
        val taxaResto = maxOf(drsResto.toDouble(), 0.5) / diasResto
        tpm.taxaDr / taxaResto
    }

    val faseDoRegistro = diasComFase.associate { it.first.data to faseEstatistica(it.second) }
    val taticas = Tatica.entries.mapNotNull { t ->
        val usos = reais.filter { t in it.taticas }
        if (usos.isEmpty()) return@mapNotNull null
        val bons = usos.filter { it.taticas[t] == true }
        val melhor = bons.mapNotNull { faseDoRegistro[it.data] }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        EstatisticaTatica(t, usos.size, bons.size, usos.count { it.taticas[t] == false }, melhor)
    }.sortedWith(compareByDescending<EstatisticaTatica> { it.taxaSucesso ?: -1.0 }.thenByDescending { it.usos })

    val motivos = reais.filter { it.dr > 0 }.flatMap { it.motivos }.groupingBy { it }.eachCount()
        .toList().sortedByDescending { it.second }

    return Insights(
        diasRegistrados = reais.size,
        regularidade = regularidade,
        acuracia = acuracia(menstruacoes, config),
        tpmPorCiclo = inicioDaTpmPorCiclo(menstruacoes, registros),
        porFase = porFase,
        multiplicadorDrTpm = multiplicador,
        taticas = taticas,
        motivos = motivos,
        curvaHumor = curvaHumor(menstruacoes, reais),
        drsTotal = reais.count { it.dr > 0 },
    )
}

/**
 * Como o app teria acertado: para cada ciclo a partir do segundo, prevê o
 * início usando só o que havia antes e compara com o que aconteceu.
 */
fun acuracia(menstruacoes: List<Menstruacao>, config: ConfigCiclo): Acuracia? {
    val ordenadas = menstruacoes.sortedBy { it.inicio }.distinctBy { it.inicio }
    val erros = (1 until ordenadas.size).mapNotNull { i ->
        val real = ordenadas[i].inicio
        val anterior = ordenadas[i - 1].inicio
        if (dias(anterior, real) !in 18..50) return@mapNotNull null
        val previsao = prever(ordenadas.subList(0, i), config) ?: return@mapNotNull null
        abs(dias(previsao.proximoInicio, real))
    }
    if (erros.size < 2) return null
    return Acuracia(erros.size, erros.average(), erros.count { it <= 1 })
}

/**
 * Humor médio de 14 dias antes até 7 dias depois de cada início registrado.
 * Os dias relativos usam o início real seguinte (ou o anterior, depois dele).
 */
fun curvaHumor(menstruacoes: List<Menstruacao>, registros: List<RegistroDia>): List<PontoHumor> {
    val inicios = menstruacoes.map { it.inicio }.sorted().distinct()
    if (inicios.isEmpty()) return emptyList()
    val pontos = registros.mapNotNull { r ->
        val humor = r.humor?.valor ?: return@mapNotNull null
        val relativos = inicios.map { dias(it, r.data) }.filter { it in -CURVA_ANTES..CURVA_DEPOIS }
        val relativo = relativos.minByOrNull { abs(it) } ?: return@mapNotNull null
        relativo to humor
    }
    return pontos.groupBy({ it.first }, { it.second }).map { (d, hs) -> PontoHumor(d, hs.average(), hs.size) }.sortedBy { it.diaRelativo }
}
