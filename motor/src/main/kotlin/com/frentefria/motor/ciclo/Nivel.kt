package com.frentefria.motor.ciclo

import com.frentefria.motor.registro.RegistroDia
import java.time.LocalDate
import kotlin.math.roundToInt

const val NIVEL_MAXIMO = 4

/**
 * Nível de alerta pela fase, sem histórico: 0 céu limpo … 4 furacão.
 * Menstruação pesa nos dois primeiros dias; a TPM sobe conforme o início chega.
 */
fun nivelBase(dia: Dia): Int = when (dia.fase) {
    Fase.MENSTRUACAO -> when (dia.diaDoCiclo) { 1, 2 -> 3; 3 -> 2; else -> 1 }
    Fase.FOLICULAR -> 0
    Fase.OVULACAO -> 0
    Fase.LUTEA -> 1
    Fase.TPM -> if (dia.diasAteProximo <= 2) 3 else 2
    Fase.ATRASADA -> if (dia.diasAtraso >= 3) 4 else 3
}

/**
 * Chave para comparar dias de ciclos diferentes: na segunda metade do ciclo,
 * o que importa é a distância até o próximo início ("3 dias antes"); na
 * primeira, o dia do ciclo ("dia 2"). Dia atrasado conta como véspera.
 */
fun chaveDoDia(dia: Dia): String = when {
    dia.fase == Fase.ATRASADA -> "antes:1"
    dia.diaDoCiclo <= 10 -> "dia:${dia.diaDoCiclo}"
    else -> "antes:${dia.diasAteProximo.coerceAtLeast(1)}"
}

/**
 * Intensidade média observada por chave de dia, em ciclos já registrados.
 * Só entram chaves com 2 observações ou mais.
 */
fun historicoDeIntensidade(
    menstruacoes: List<Menstruacao>,
    registros: Map<LocalDate, RegistroDia>,
    previsao: Previsao,
    hoje: LocalDate,
): Map<String, Double> =
    registros.values.asSequence()
        .filter { !it.data.isAfter(hoje) }
        .mapNotNull { r -> r.intensidade?.let { i -> diaEm(r.data, hoje, menstruacoes, previsao)?.let { chaveDoDia(it) to i } } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size >= 2 }
        .mapValues { (_, v) -> v.average() }

/**
 * O nível do dia: a base da fase, misturada meio a meio com o que ele já
 * observou nesse mesmo dia de outros ciclos. Atraso nunca fica abaixo de 3.
 */
fun nivel(dia: Dia, historico: Map<String, Double>): Int {
    val base = nivelBase(dia)
    val observado = historico[chaveDoDia(dia)] ?: return base
    val misturado = ((base + observado) / 2.0).roundToInt().coerceIn(0, NIVEL_MAXIMO)
    return if (dia.fase == Fase.ATRASADA) maxOf(misturado, 3) else misturado
}
