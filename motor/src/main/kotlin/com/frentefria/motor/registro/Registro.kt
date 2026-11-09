package com.frentefria.motor.registro

import com.frentefria.motor.ciclo.DataSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Humor dela, do jeito que ele percebeu. */
enum class Humor(val valor: Int) { PESSIMO(1), RUIM(2), NEUTRO(3), BOM(4), OTIMO(5) }

enum class Sintoma(val deTpm: Boolean) {
    IRRITACAO(true), CHORO(true), SENSIVEL(true), ANSIOSA(true), CANSACO(true), COLICA(false),
    DOR_DE_CABECA(false), FOME(true), INCHACO(true), SONO_RUIM(false), LIBIDO_ALTA(false),
    LIBIDO_BAIXA(false), CARINHOSA(false), ENERGIA_ALTA(false),
}

enum class MotivoDr { NADA_APARENTE, TAREFAS, LOUCA, DINHEIRO, FAMILIA, CELULAR, COMIDA, TOM_DE_VOZ, ESQUECIMENTO, CIUME, BAGUNCA, OUTRO }

enum class Tatica {
    CHOCOLATE, COMIDA_FAVORITA, MASSAGEM, DAR_ESPACO, OUVIR_SEM_OPINAR, BOLSA_TERMICA, ASSUMIR_TAREFAS,
    FILME_NO_SOFA, ELOGIO, PEDIR_DESCULPA, FLORES, CHA, PASSEIO, CONCORDAR, ABRACO, DELIVERY,
}

/** Intensidade da DR: 0 nenhuma, 1 faísca, 2 discussão, 3 guerra. */
const val DR_MAXIMA = 3

@Serializable
data class RegistroDia(
    @Serializable(with = DataSerializer::class) val data: LocalDate,
    val humor: Humor? = null,
    val sintomas: Set<Sintoma> = emptySet(),
    val dr: Int = 0,
    val motivos: Set<MotivoDr> = emptySet(),
    /** Tática usada → funcionou (true), não funcionou (false) ou sem avaliação (null). */
    val taticas: Map<Tatica, Boolean?> = emptyMap(),
    val nota: String = "",
) {
    val vazio: Boolean get() = humor == null && sintomas.isEmpty() && dr == 0 && taticas.isEmpty() && nota.isBlank()

    /** O dia teve cara de TPM: humor ruim, DR ou sintomas típicos. */
    val sinalDeTpm: Boolean
        get() = (humor != null && humor.valor <= Humor.RUIM.valor) || dr > 0 || sintomas.count { it.deTpm } >= 2

    /**
     * O quanto o dia foi pesado, de 0 a 4, na mesma régua do nível de alerta:
     * o pior entre o humor (péssimo = 4) e a DR (guerra = 4).
     */
    val intensidade: Int?
        get() {
            val doHumor = humor?.let { 5 - it.valor }
            val daDr = if (dr > 0) dr + 1 else null
            val sintomasTpm = sintomas.count { it.deTpm }
            val dosSintomas = if (sintomasTpm > 0) minOf(sintomasTpm, 3) else null
            return listOfNotNull(doHumor, daDr, dosSintomas).maxOrNull()
        }
}
