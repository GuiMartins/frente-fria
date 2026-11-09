package com.frentefria.motor

import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.TPM_PADRAO
import com.frentefria.motor.ciclo.diaEm
import com.frentefria.motor.ciclo.duracaoTpm
import com.frentefria.motor.ciclo.inicioDaTpmPorCiclo
import com.frentefria.motor.ciclo.nivel
import com.frentefria.motor.ciclo.nivelBase
import com.frentefria.motor.ciclo.preverComRegistros
import com.frentefria.motor.ciclo.historicoDeIntensidade
import com.frentefria.motor.registro.Humor
import com.frentefria.motor.registro.RegistroDia
import com.frentefria.motor.registro.Sintoma
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Registros de um ciclo: `tpm` dias ruins antes de cada início e dias bons
 * antes disso, de 14 dias antes até a véspera.
 */
fun registrosComTpm(inicios: List<LocalDate>, tpm: Int): Map<LocalDate, RegistroDia> =
    inicios.drop(1).flatMap { inicio ->
        (1..14).map { k ->
            val data = inicio.minusDays(k.toLong())
            data to if (k <= tpm) RegistroDia(data, Humor.RUIM, setOf(Sintoma.IRRITACAO)) else RegistroDia(data, Humor.BOM)
        }
    }.toMap()

class AprendizadoTest {
    private val config = ConfigCiclo()

    @Test
    fun `aprende uma TPM de 5 dias`() {
        val ms = ciclos("2026-06-01", 28, 28, 28)
        val regs = registrosComTpm(ms.map { it.inicio }, 5)
        assertEquals(listOf(5, 5, 5), inicioDaTpmPorCiclo(ms, regs))
        assertEquals(5, duracaoTpm(ms, regs, config))
    }

    @Test
    fun `um dia ruim isolado longe do início não estende a TPM`() {
        val ms = ciclos("2026-06-01", 28, 28)
        val regs = registrosComTpm(ms.map { it.inicio }, 4).toMutableMap()
        val longe = ms[1].inicio.minusDays(12)
        regs[longe] = RegistroDia(longe, Humor.PESSIMO, dr = 2)
        assertEquals(4, inicioDaTpmPorCiclo(ms, regs).first())
    }

    @Test
    fun `com menos de 2 ciclos aprendidos vale o padrão`() {
        val ms = ciclos("2026-06-01", 28)
        val regs = registrosComTpm(ms.map { it.inicio }, 4)
        assertEquals(TPM_PADRAO, duracaoTpm(ms, regs, config))
    }

    @Test
    fun `TPM fixada à mão vence a aprendida`() {
        val ms = ciclos("2026-06-01", 28, 28, 28)
        val regs = registrosComTpm(ms.map { it.inicio }, 5)
        assertEquals(9, duracaoTpm(ms, regs, ConfigCiclo(tpmFixa = 9)))
    }

    @Test
    fun `poucos registros no ciclo não contam`() {
        val ms = ciclos("2026-06-01", 28, 28)
        val inicio = ms[1].inicio
        val regs = mapOf(inicio.minusDays(2) to RegistroDia(inicio.minusDays(2), Humor.PESSIMO))
        assertTrue(inicioDaTpmPorCiclo(ms, regs).isEmpty())
    }

    @Test
    fun `a TPM aprendida muda a fase na previsão`() {
        val ms = ciclos("2026-06-01", 28, 28, 28)
        val regs = registrosComTpm(ms.map { it.inicio }, 4)
        val p = preverComRegistros(ms, regs, config)!!
        assertEquals(4, p.diasTpm)
        val hoje = ms.last().inicio.plusDays(22) // 6 dias antes do próximo
        assertEquals(Fase.LUTEA, diaEm(hoje, hoje, ms, p)!!.fase)
    }

    @Test
    fun `nível base sobe na véspera e no atraso`() {
        val ms = ciclos("2026-09-01", 28, 28)
        val p = preverComRegistros(ms, emptyMap(), config)!!
        val proximo = d("2026-11-24")
        assertEquals(2, nivelBase(diaEm(proximo.minusDays(5), proximo, ms, p)!!))
        assertEquals(3, nivelBase(diaEm(proximo.minusDays(1), proximo, ms, p)!!))
        assertEquals(4, nivelBase(diaEm(proximo.plusDays(4), proximo.plusDays(4), ms, p)!!))
        assertEquals(0, nivelBase(diaEm(d("2026-11-03"), d("2026-11-03"), ms, p)!!))
    }

    @Test
    fun `o histórico puxa o nível para o que ele observou`() {
        // Ela é ótima na TPM (humor ótimo nos 7 dias antes, em 3 ciclos).
        val ms = ciclos("2026-06-01", 28, 28, 28)
        val regs = ms.drop(1).flatMap { m ->
            (1..7).map { k -> m.inicio.minusDays(k.toLong()).let { it to RegistroDia(it, Humor.OTIMO) } }
        }.toMap()
        val p = preverComRegistros(ms, regs, config)!!
        val hoje = ms.last().inicio.plusDays(27) // véspera
        val dia = diaEm(hoje, hoje, ms, p)!!
        val hist = historicoDeIntensidade(ms, regs, p, hoje)
        assertEquals(3, nivelBase(dia))
        assertEquals(2, nivel(dia, hist)) // (3 + 0) / 2 = 1,5 → 2
    }
}
