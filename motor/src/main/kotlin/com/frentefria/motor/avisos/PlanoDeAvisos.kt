package com.frentefria.motor.avisos

import com.frentefria.motor.Situacao
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.dias
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.emojiDoNivel
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDoNivel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class Horario { MANHA, NOITE }

enum class TipoAviso { RESUMO, TPM_CHEGANDO, REPOR_KIT, PREVISTA_HOJE, ATRASADA, EVENTO, REGISTRAR }

/** Uma notificação pronta, em texto final. `comecou` põe o botão "Começou" nela. */
data class Aviso(val tipo: TipoAviso, val titulo: String, val texto: String, val comecou: Boolean = false, val chave: String = tipo.name)

data class OpcoesAvisos(
    val modo: Modo,
    /** Resumo todo dia, e não só nos dias com alerta ou troca de fase. */
    val resumoTodoDia: Boolean = false,
    val lembreteNoite: Boolean = true,
)

private val PT = Locale("pt", "BR")
private fun diaDaSemana(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.FULL, PT).substringBefore("-")

/**
 * O que publicar num horário. Tudo é recalculado a partir dos dados: um aviso
 * perdido não deixa nada inconsistente. `faltandoNoKit` são os nomes do
 * estoque marcados como "não tem".
 */
fun avisosDe(situacao: Situacao, horario: Horario, opcoes: OpcoesAvisos, faltandoNoKit: List<String>): List<Aviso> {
    val agora = situacao.agora ?: return emptyList()
    val d = agora.dia
    val clima = opcoes.modo == Modo.CLIMA
    val hoje = situacao.hoje

    if (horario == Horario.NOITE) {
        val emTpm = d.fase == Fase.TPM || d.fase == Fase.ATRASADA
        if (!opcoes.lembreteNoite || !emTpm || situacao.registroDeHoje != null) return emptyList()
        return listOf(
            Aviso(
                TipoAviso.REGISTRAR,
                if (clima) "Como foi o tempo hoje?" else "Como ela estava hoje?",
                if (clima) "Registre as condições de hoje: a previsão aprende com isso." else "Registre o dia: é assim que o app aprende a TPM dela.",
            ),
        )
    }

    val avisos = mutableListOf<Aviso>()
    val ontem = situacao.dia(hoje.minusDays(1))
    val mudouFase = ontem != null && ontem.dia.fase != d.fase
    if (opcoes.resumoTodoDia || agora.nivel >= 2 || mudouFase) {
        val titulo = "${emojiDoNivel(agora.nivel)} ${nomeDoNivel(agora.nivel, opcoes.modo)} · ${nomeDaFase(d.fase, opcoes.modo)}"
        val texto = when {
            mudouFase && clima -> "Mudança de estação: ${nomeDaFase(d.fase, opcoes.modo).lowercase()} a partir de hoje."
            mudouFase -> "Começou hoje: ${nomeDaFase(d.fase, opcoes.modo)}."
            agora.nivel >= 3 && clima -> "Condições severas. Evite exposição desnecessária."
            agora.nivel >= 3 -> "Dia de pisar em ovos. Abra o app antes de abrir a boca."
            clima -> "Instabilidade ao longo do dia."
            else -> "Dia de atenção. Paciência extra recomendada."
        }
        avisos += Aviso(TipoAviso.RESUMO, titulo, texto)
    }

    val ateTpm = situacao.diasAteTpm
    if (ateTpm == 2) {
        val inicio = situacao.proximaTpm!!
        avisos += Aviso(
            TipoAviso.TPM_CHEGANDO,
            if (clima) "Frente fria chega ${diaDaSemana(inicio)}" else "TPM começa ${diaDaSemana(inicio)}",
            if (clima) "Massa de ar polar se aproxima. Reforce a paciência e evite assuntos polêmicos." else "Faltam 2 dias. Evite marcar DR, compras grandes e visita da sua mãe.",
        )
    }
    if (ateTpm == 3 && faltandoNoKit.isNotEmpty()) {
        avisos += Aviso(
            TipoAviso.REPOR_KIT,
            if (clima) "Abasteça a despensa" else "Reponha o kit de sobrevivência",
            "Falta: ${faltandoNoKit.joinToString(", ")}.",
        )
    }

    if (d.fase == Fase.TPM && d.diasAteProximo == 0) {
        avisos += Aviso(
            TipoAviso.PREVISTA_HOJE,
            if (clima) "Chuva prevista para hoje" else "Menstruação prevista para hoje",
            if (clima) "Se começar a chover, toque em Começou." else "Se descer, toque em Começou. Não pergunte para ela, observe.",
            comecou = true,
        )
    }
    if (d.fase == Fase.ATRASADA) {
        val n = d.diasAtraso
        avisos += Aviso(
            TipoAviso.ATRASADA,
            if (clima) "Chuva atrasada há $n ${if (n == 1) "dia" else "dias"}" else "Atrasou $n ${if (n == 1) "dia" else "dias"}",
            if (clima) "Frente fria estacionada. Quando chover, toque em Começou." else "Não comente. Quando descer, toque em Começou.",
            comecou = true,
        )
    }

    situacao.conflitos(opcoes.modo, ate = 3).filter { dias(hoje, it.evento.data) == 3 }.forEach { c ->
        avisos += Aviso(
            TipoAviso.EVENTO,
            "${c.evento.titulo} cai num dia de ${nomeDoNivel(c.nivel, opcoes.modo).lowercase()}",
            c.dica,
            chave = "EVENTO:${c.evento.id}",
        )
    }
    return avisos
}
