package com.frentefria.motor.agenda

import com.frentefria.motor.ciclo.DataSerializer
import com.frentefria.motor.ciclo.Dia
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.texto.Modo
import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class TipoEvento { ANIVERSARIO_DELA, VIAGEM, FAMILIA, JANTAR, FESTA, PRAIA, OUTRO }

@Serializable
data class Evento(
    val id: Long = 0,
    @Serializable(with = DataSerializer::class) val data: LocalDate,
    val titulo: String,
    val tipo: TipoEvento = TipoEvento.OUTRO,
)

fun nomeDoTipo(t: TipoEvento): String = when (t) {
    TipoEvento.ANIVERSARIO_DELA -> "Aniversário dela"
    TipoEvento.VIAGEM -> "Viagem"
    TipoEvento.FAMILIA -> "Família"
    TipoEvento.JANTAR -> "Jantar / encontro"
    TipoEvento.FESTA -> "Festa"
    TipoEvento.PRAIA -> "Praia / piscina"
    TipoEvento.OUTRO -> "Outro"
}

fun emojiDoTipo(t: TipoEvento): String = when (t) {
    TipoEvento.ANIVERSARIO_DELA -> "🎂"
    TipoEvento.VIAGEM -> "✈️"
    TipoEvento.FAMILIA -> "👨‍👩‍👧"
    TipoEvento.JANTAR -> "🍷"
    TipoEvento.FESTA -> "🎉"
    TipoEvento.PRAIA -> "🏖️"
    TipoEvento.OUTRO -> "📌"
}

data class Conflito(val evento: Evento, val dia: Dia, val nivel: Int, val dica: String)

/** Um evento conflita se cai num dia de nível 3 ou mais, na TPM ou na menstruação. */
fun conflita(dia: Dia, nivel: Int): Boolean =
    nivel >= 3 || dia.fase == Fase.TPM || dia.fase == Fase.ATRASADA ||
        (dia.fase == Fase.MENSTRUACAO && dia.diaDoCiclo <= 3)

fun dicaDeConflito(tipo: TipoEvento, dia: Dia, modo: Modo): String {
    val chuva = dia.fase == Fase.MENSTRUACAO
    val clima = modo == Modo.CLIMA
    return when (tipo) {
        TipoEvento.ANIVERSARIO_DELA -> if (clima) "Aniversário sob mau tempo: capriche no presente e não marque nada cansativo." else "Aniversário na TPM: capriche no presente, deixe ela escolher tudo e nada de programa cansativo."
        TipoEvento.VIAGEM -> when {
            chuva && clima -> "Viagem com chuva: leve o kit (remédio, bolsa térmica) e planeje paradas."
            chuva -> "Viagem na menstruação: leve remédio, bolsa térmica, absorvente extra e planeje paradas."
            clima -> "Viagem com frente fria: roteiro leve, comida boa e nada de horário apertado."
            else -> "Viagem na TPM: roteiro leve, comida boa garantida e nenhum horário apertado."
        }
        TipoEvento.FAMILIA -> "Combine antes um sinal discreto de \"vamos embora\" e respeite quando ela usar."
        TipoEvento.JANTAR -> "Reserve num lugar que ela ama. Não é dia de experimentar restaurante novo."
        TipoEvento.FESTA -> "Vá de carro próprio para poder sair cedo sem depender de ninguém."
        TipoEvento.PRAIA -> if (chuva && !clima) "Praia na menstruação pode ser um problema: pergunte com jeito se ela quer remarcar." else "Pode bater o inchaço: nada de comentário sobre corpo, foto ou biquíni."
        TipoEvento.OUTRO -> if (clima) "Previsão de instabilidade nesse dia. Tenha um plano B." else "Cai num dia difícil. Tenha um plano B e paciência extra."
    }
}
