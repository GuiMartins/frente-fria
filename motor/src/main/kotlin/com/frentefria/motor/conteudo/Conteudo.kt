package com.frentefria.motor.conteudo

import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.texto.Modo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.random.Random

/**
 * Um texto do conteúdo. `clima` substitui `texto` no modo disfarçado, só
 * quando o texto entrega o jogo. Listas vazias valem para qualquer fase/nível.
 */
@Serializable
data class Item(
    val texto: String,
    val clima: String? = null,
    val fases: List<Fase> = emptyList(),
    val niveis: List<Int> = emptyList(),
    /** Explicação curta (frases-bomba: por que não dizer; dizer: por que funciona). */
    val porque: String? = null,
    @SerialName("porque_clima") val porqueClima: String? = null,
) {
    fun em(modo: Modo): String = if (modo == Modo.CLIMA) clima ?: texto else texto
    fun porqueEm(modo: Modo): String? = if (modo == Modo.CLIMA) porqueClima ?: porque else porque
    /** Dia atrasado aceita também o que foi escrito para a TPM. */
    fun serve(fase: Fase, nivel: Int? = null): Boolean =
        (fases.isEmpty() || fase in fases || (fase == Fase.ATRASADA && Fase.TPM in fases)) &&
            (nivel == null || niveis.isEmpty() || nivel in niveis)
}

@Serializable
data class TextoFase(val texto: String, val clima: String? = null) {
    fun em(modo: Modo): String = if (modo == Modo.CLIMA) clima ?: texto else texto
}

@Serializable
data class Conteudo(
    /** O que acontece com ela em cada fase (Manual). */
    @SerialName("o_que_acontece") val oQueAcontece: Map<Fase, TextoFase> = emptyMap(),
    /** A frase grande do cartão de previsão. */
    val manchetes: List<Item> = emptyList(),
    val faca: List<Item> = emptyList(),
    @SerialName("nao_faca") val naoFaca: List<Item> = emptyList(),
    val dizer: List<Item> = emptyList(),
    @SerialName("nunca_dizer") val nuncaDizer: List<Item> = emptyList(),
    val agrados: List<Item> = emptyList(),
    /** Fatos e curiosidades para o Manual e para o rodapé do Hoje. */
    val fatos: List<Item> = emptyList(),
) {
    fun todos(): List<Pair<String, Item>> =
        manchetes.map { "manchetes" to it } + faca.map { "faca" to it } + naoFaca.map { "nao_faca" to it } +
            dizer.map { "dizer" to it } + nuncaDizer.map { "nunca_dizer" to it } + agrados.map { "agrados" to it } +
            fatos.map { "fatos" to it }

    companion object {
        private val json = Json { ignoreUnknownKeys = false }
        fun ler(texto: String): Conteudo = json.decodeFromString(serializer(), texto)
    }
}

/** O que o Hoje mostra: a mesma escolha o dia inteiro, outra amanhã. */
data class ConteudoDoDia(val manchete: Item?, val faca: List<Item>, val naoFaca: List<Item>, val fato: Item?)

fun conteudoDoDia(conteudo: Conteudo, data: LocalDate, fase: Fase, nivel: Int, quantidade: Int = 3): ConteudoDoDia {
    val semente = data.toEpochDay()
    fun <T> List<T>.sorteio(salto: Int): List<T> = shuffled(Random(semente * 31 + salto))
    // Manchete: primeiro as do nível exato; sem nenhuma, qualquer uma da fase.
    val manchetes = conteudo.manchetes.filter { it.serve(fase, nivel) && it.niveis.isNotEmpty() }
        .ifEmpty { conteudo.manchetes.filter { it.serve(fase) } }
    return ConteudoDoDia(
        manchete = manchetes.sorteio(1).firstOrNull(),
        faca = conteudo.faca.filter { it.serve(fase, nivel) }.sorteio(2).take(quantidade),
        naoFaca = conteudo.naoFaca.filter { it.serve(fase, nivel) }.sorteio(3).take(quantidade),
        fato = conteudo.fatos.filter { it.serve(fase) }.sorteio(4).firstOrNull(),
    )
}
