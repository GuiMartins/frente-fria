package com.frentefria.motor.texto

import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.registro.MotivoDr
import com.frentefria.motor.registro.Sintoma
import com.frentefria.motor.registro.Tatica

/**
 * CLIMA: disfarçado de app de previsão do tempo; nada visível fala de TPM.
 * DIRETO: tudo com todas as letras.
 */
enum class Modo { CLIMA, DIRETO }

/**
 * Palavras que entregam o jogo. No modo CLIMA, nenhum texto mostrado pode ter
 * uma delas (o teste do conteúdo confere).
 */
val PALAVRAS_PROIBIDAS_NO_CLIMA = listOf("tpm", "menstrua", "absorvente", "ovula", "cólica", "colica", "ciclo", "hormôn", "hormon", "período", "fértil", "fertil", "sangra", "anticoncep")

fun entrega(texto: String): String? = PALAVRAS_PROIBIDAS_NO_CLIMA.firstOrNull { texto.lowercase().contains(it) }

fun nomeDaFase(fase: Fase, modo: Modo): String = when (modo) {
    Modo.CLIMA -> when (fase) {
        Fase.MENSTRUACAO -> "Temporada de chuvas"
        Fase.FOLICULAR -> "Primavera"
        Fase.OVULACAO -> "Onda de calor"
        Fase.LUTEA -> "Outono"
        Fase.TPM -> "Frente fria"
        Fase.ATRASADA -> "Frente fria estacionada"
    }
    Modo.DIRETO -> when (fase) {
        Fase.MENSTRUACAO -> "Menstruação"
        Fase.FOLICULAR -> "Fase folicular"
        Fase.OVULACAO -> "Ovulação"
        Fase.LUTEA -> "Fase lútea"
        Fase.TPM -> "TPM"
        Fase.ATRASADA -> "Menstruação atrasada"
    }
}

/** Uma linha que explica a fase em português de gente. */
fun resumoDaFase(fase: Fase, modo: Modo): String = when (modo) {
    Modo.CLIMA -> when (fase) {
        Fase.MENSTRUACAO -> "Chuva forte nos primeiros dias, melhorando aos poucos."
        Fase.FOLICULAR -> "Tempo firme, sol e boa visibilidade. Aproveite."
        Fase.OVULACAO -> "Calor intenso. Clima propício para atividades a dois."
        Fase.LUTEA -> "Temperatura caindo devagar. Leve um casaco."
        Fase.TPM -> "Massa de ar polar sobre a região. Instabilidade alta."
        Fase.ATRASADA -> "A frente fria parou sobre a região e não tem previsão de saída."
    }
    Modo.DIRETO -> when (fase) {
        Fase.MENSTRUACAO -> "Os primeiros dias costumam ser os piores: cólica, cansaço, pouca paciência."
        Fase.FOLICULAR -> "Hormônios subindo: energia e bom humor voltando. A melhor fase."
        Fase.OVULACAO -> "Pico de estrogênio: disposição, autoestima e libido em alta."
        Fase.LUTEA -> "Progesterona no comando: mais cansaço, mais fome, menos paciência."
        Fase.TPM -> "A queda dos hormônios antes da menstruação. Instabilidade garantida."
        Fase.ATRASADA -> "Passou do dia previsto. Pode ser só um ciclo mais longo."
    }
}

fun nomeDoNivel(nivel: Int, modo: Modo): String = when (modo) {
    Modo.CLIMA -> listOf("Céu limpo", "Nublado", "Chuva", "Tempestade", "Furacão")
    Modo.DIRETO -> listOf("Tranquilo", "Atenção", "Alerta", "Perigo", "Fuja")
}[nivel.coerceIn(0, 4)]

fun emojiDoNivel(nivel: Int): String = listOf("☀️", "⛅", "🌧️", "⛈️", "🌪️")[nivel.coerceIn(0, 4)]

fun nomeDoSintoma(s: Sintoma, modo: Modo): String = when (s) {
    Sintoma.IRRITACAO -> "Irritada"
    Sintoma.CHORO -> "Chorou"
    Sintoma.SENSIVEL -> "Sensível"
    Sintoma.ANSIOSA -> "Ansiosa"
    Sintoma.CANSACO -> "Cansada"
    Sintoma.COLICA -> if (modo == Modo.CLIMA) "Dor na barriga" else "Cólica"
    Sintoma.DOR_DE_CABECA -> "Dor de cabeça"
    Sintoma.FOME -> "Fome / desejos"
    Sintoma.INCHACO -> "Inchada"
    Sintoma.SONO_RUIM -> "Dormiu mal"
    Sintoma.LIBIDO_ALTA -> "Libido alta"
    Sintoma.LIBIDO_BAIXA -> "Libido baixa"
    Sintoma.CARINHOSA -> "Carinhosa"
    Sintoma.ENERGIA_ALTA -> "Elétrica"
}

fun emojiDoSintoma(s: Sintoma): String = when (s) {
    Sintoma.IRRITACAO -> "😤"
    Sintoma.CHORO -> "😭"
    Sintoma.SENSIVEL -> "🥺"
    Sintoma.ANSIOSA -> "😰"
    Sintoma.CANSACO -> "🥱"
    Sintoma.COLICA -> "🤕"
    Sintoma.DOR_DE_CABECA -> "🤯"
    Sintoma.FOME -> "🍫"
    Sintoma.INCHACO -> "🎈"
    Sintoma.SONO_RUIM -> "😵"
    Sintoma.LIBIDO_ALTA -> "🔥"
    Sintoma.LIBIDO_BAIXA -> "🧊"
    Sintoma.CARINHOSA -> "🥰"
    Sintoma.ENERGIA_ALTA -> "⚡"
}

fun nomeDoMotivo(m: MotivoDr): String = when (m) {
    MotivoDr.NADA_APARENTE -> "Nada aparente"
    MotivoDr.TAREFAS -> "Tarefas de casa"
    MotivoDr.LOUCA -> "Louça"
    MotivoDr.DINHEIRO -> "Dinheiro"
    MotivoDr.FAMILIA -> "Família"
    MotivoDr.CELULAR -> "Celular"
    MotivoDr.COMIDA -> "Comida"
    MotivoDr.TOM_DE_VOZ -> "Tom de voz"
    MotivoDr.ESQUECIMENTO -> "Esqueci algo"
    MotivoDr.CIUME -> "Ciúme"
    MotivoDr.BAGUNCA -> "Bagunça"
    MotivoDr.OUTRO -> "Outro"
}

fun nomeDaTatica(t: Tatica, modo: Modo): String = when (t) {
    Tatica.CHOCOLATE -> "Chocolate"
    Tatica.COMIDA_FAVORITA -> "Comida favorita"
    Tatica.MASSAGEM -> "Massagem"
    Tatica.DAR_ESPACO -> "Dar espaço"
    Tatica.OUVIR_SEM_OPINAR -> "Ouvir sem opinar"
    Tatica.BOLSA_TERMICA -> "Bolsa térmica"
    Tatica.ASSUMIR_TAREFAS -> "Assumir as tarefas"
    Tatica.FILME_NO_SOFA -> "Filme no sofá"
    Tatica.ELOGIO -> "Elogio"
    Tatica.PEDIR_DESCULPA -> "Pedir desculpa"
    Tatica.FLORES -> "Flores"
    Tatica.CHA -> "Chá"
    Tatica.PASSEIO -> "Passeio"
    Tatica.CONCORDAR -> "Concordar com tudo"
    Tatica.ABRACO -> "Abraço"
    Tatica.DELIVERY -> "Delivery"
}

fun emojiDaTatica(t: Tatica): String = when (t) {
    Tatica.CHOCOLATE -> "🍫"
    Tatica.COMIDA_FAVORITA -> "🍝"
    Tatica.MASSAGEM -> "💆"
    Tatica.DAR_ESPACO -> "🚪"
    Tatica.OUVIR_SEM_OPINAR -> "🤐"
    Tatica.BOLSA_TERMICA -> "♨️"
    Tatica.ASSUMIR_TAREFAS -> "🧽"
    Tatica.FILME_NO_SOFA -> "🎬"
    Tatica.ELOGIO -> "💬"
    Tatica.PEDIR_DESCULPA -> "🙏"
    Tatica.FLORES -> "💐"
    Tatica.CHA -> "🍵"
    Tatica.PASSEIO -> "🚶"
    Tatica.CONCORDAR -> "👍"
    Tatica.ABRACO -> "🤗"
    Tatica.DELIVERY -> "🛵"
}

fun nomeDaDr(intensidade: Int): String = listOf("Paz", "Faísca", "Discussão", "Guerra")[intensidade.coerceIn(0, 3)]

/** Os nomes das partes do app que mudam com o modo. */
data class Termos(
    val tpm: String,
    val menstruacao: String,
    val menstruouHoje: String,
    val parouHoje: String,
    val dr: String,
    val ciclo: String,
    /** "da estação" / "do ciclo" */
    val doCiclo: String,
)

fun termos(modo: Modo): Termos = when (modo) {
    Modo.CLIMA -> Termos(
        tpm = "frente fria",
        menstruacao = "temporada de chuvas",
        menstruouHoje = "Começou a chover",
        parouHoje = "Parou de chover",
        dr = "Deu trovoada",
        ciclo = "estação",
        doCiclo = "da estação",
    )
    Modo.DIRETO -> Termos(
        tpm = "TPM",
        menstruacao = "menstruação",
        menstruouHoje = "Menstruou hoje",
        parouHoje = "Parou hoje",
        dr = "Deu ruim (DR)",
        ciclo = "ciclo",
        doCiclo = "do ciclo",
    )
}
