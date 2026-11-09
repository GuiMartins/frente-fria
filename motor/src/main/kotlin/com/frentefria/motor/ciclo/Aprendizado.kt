package com.frentefria.motor.ciclo

import com.frentefria.motor.registro.RegistroDia
import java.time.LocalDate
import kotlin.math.roundToInt

const val TPM_MINIMA = 3
const val TPM_MAXIMA = 12
private const val JANELA_BUSCA = 14
private const val DIAS_CALMOS_PARA_PARAR = 3

/**
 * Para cada ciclo completo, quantos dias antes do início a TPM dela começou.
 * Anda para trás a partir da véspera: cada dia com sinal (humor ruim, DR,
 * sintomas típicos) estende a TPM; três dias registrados seguidos sem sinal
 * encerram a busca. Assim um dia ruim isolado duas semanas antes não conta.
 * Ciclos com menos de 4 dias registrados nas duas semanas antes ficam de fora.
 */
fun inicioDaTpmPorCiclo(menstruacoes: List<Menstruacao>, registros: Map<LocalDate, RegistroDia>): List<Int> {
    val inicios = menstruacoes.map { it.inicio }.sorted().distinct()
    return inicios.zipWithNext().mapNotNull { (anterior, proximo) ->
        val limite = minOf(JANELA_BUSCA, dias(anterior, proximo) - 1)
        val dias = (1..limite).map { k -> k to registros[proximo.minusDays(k.toLong())] }
        if (dias.count { it.second?.vazio == false } < 4) return@mapNotNull null
        var maisLonge = 0
        var calmos = 0
        for ((k, r) in dias) {
            when {
                r == null || r.vazio -> Unit
                r.sinalDeTpm -> { maisLonge = k; calmos = 0 }
                else -> { calmos++; if (calmos >= DIAS_CALMOS_PARA_PARAR && maisLonge > 0) break }
            }
        }
        maisLonge.takeIf { it > 0 }
    }
}

/** Duração da TPM: a fixada à mão, a aprendida (2 ciclos ou mais) ou o padrão de 7 dias. */
fun duracaoTpm(menstruacoes: List<Menstruacao>, registros: Map<LocalDate, RegistroDia>, config: ConfigCiclo): Int {
    config.tpmFixa?.let { return it.coerceIn(TPM_MINIMA, TPM_MAXIMA) }
    return tpmAprendida(menstruacoes, registros) ?: TPM_PADRAO
}

fun tpmAprendida(menstruacoes: List<Menstruacao>, registros: Map<LocalDate, RegistroDia>): Int? {
    val porCiclo = inicioDaTpmPorCiclo(menstruacoes, registros).takeLast(6)
    if (porCiclo.size < 2) return null
    return porCiclo.average().roundToInt().coerceIn(TPM_MINIMA, TPM_MAXIMA)
}

/** A previsão completa: duração do ciclo pelo histórico e TPM aprendida pelos registros. */
fun preverComRegistros(menstruacoes: List<Menstruacao>, registros: Map<LocalDate, RegistroDia>, config: ConfigCiclo): Previsao? =
    prever(menstruacoes, config, duracaoTpm(menstruacoes, registros, config))
