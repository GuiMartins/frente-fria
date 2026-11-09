package com.frentefria.motor

import com.frentefria.motor.agenda.Conflito
import com.frentefria.motor.agenda.Evento
import com.frentefria.motor.agenda.conflita
import com.frentefria.motor.agenda.dicaDeConflito
import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.ciclo.Dia
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.Menstruacao
import com.frentefria.motor.ciclo.Previsao
import com.frentefria.motor.ciclo.diaEm
import com.frentefria.motor.ciclo.dias
import com.frentefria.motor.ciclo.historicoDeIntensidade
import com.frentefria.motor.ciclo.menstruacaoEmCurso
import com.frentefria.motor.ciclo.nivel
import com.frentefria.motor.ciclo.preverComRegistros
import com.frentefria.motor.registro.RegistroDia
import com.frentefria.motor.texto.Modo
import java.time.LocalDate

/** Os dados crus do usuário, do jeito que saem do banco. */
data class Dados(
    val menstruacoes: List<Menstruacao>,
    val registros: Map<LocalDate, RegistroDia>,
    val eventos: List<Evento>,
    val config: ConfigCiclo,
)

/** Um dia já com o nível calculado (calendário, faixa de 7 dias). */
data class DiaComNivel(val dia: Dia, val nivel: Int)

/**
 * Tudo o que as telas e os avisos precisam, calculado de uma vez a partir dos
 * dados. Sem nenhuma menstruação registrada, só `vazio` é verdadeiro.
 */
class Situacao(val dados: Dados, val hoje: LocalDate) {
    val previsao: Previsao? = preverComRegistros(dados.menstruacoes, dados.registros, dados.config)
    val vazio: Boolean get() = previsao == null

    private val historico: Map<String, Double> =
        previsao?.let { historicoDeIntensidade(dados.menstruacoes, dados.registros, it, hoje) } ?: emptyMap()

    fun dia(data: LocalDate): DiaComNivel? {
        val p = previsao ?: return null
        val d = diaEm(data, hoje, dados.menstruacoes, p) ?: return null
        return DiaComNivel(d, nivel(d, historico))
    }

    val agora: DiaComNivel? = dia(hoje)

    fun proximosDias(n: Int = 7): List<DiaComNivel> = (0 until n).mapNotNull { dia(hoje.plusDays(it.toLong())) }

    /** Próximo início previsto (o atual, se estiver atrasada). */
    val proximoInicio: LocalDate? get() = agora?.dia?.proximoInicio

    /** Primeiro dia da próxima TPM, a partir de amanhã; null se já está nela. */
    val proximaTpm: LocalDate?
        get() {
            val a = agora?.dia ?: return null
            val p = previsao ?: return null
            if (a.fase == Fase.TPM || a.fase == Fase.ATRASADA) return null
            val inicioTpm = a.proximoInicio.minusDays(p.diasTpm.toLong())
            return if (inicioTpm.isAfter(hoje)) inicioTpm else null
        }

    val diasAteTpm: Int? get() = proximaTpm?.let { dias(hoje, it) }

    val menstruacaoEmCurso: Menstruacao? = previsao?.let { menstruacaoEmCurso(hoje, dados.menstruacoes, it) }

    fun conflitos(modo: Modo, ate: Int = 60): List<Conflito> = dados.eventos
        .filter { !it.data.isBefore(hoje) && dias(hoje, it.data) <= ate }
        .sortedBy { it.data }
        .mapNotNull { e ->
            val d = dia(e.data) ?: return@mapNotNull null
            if (conflita(d.dia, d.nivel)) Conflito(e, d.dia, d.nivel, dicaDeConflito(e.tipo, d.dia, modo)) else null
        }

    val registroDeHoje: RegistroDia? get() = dados.registros[hoje]?.takeIf { !it.vazio }
}
