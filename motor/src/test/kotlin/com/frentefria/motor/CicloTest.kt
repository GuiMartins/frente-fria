package com.frentefria.motor

import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.Menstruacao
import com.frentefria.motor.ciclo.diaEm
import com.frentefria.motor.ciclo.menstruacaoEmCurso
import com.frentefria.motor.ciclo.prever
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun d(s: String): LocalDate = LocalDate.parse(s)

/** Inícios a partir de uma data e uma lista de durações. */
fun ciclos(primeiro: String, vararg duracoes: Int): List<Menstruacao> {
    var data = d(primeiro)
    val lista = mutableListOf(Menstruacao(data))
    duracoes.forEach { data = data.plusDays(it.toLong()); lista += Menstruacao(data) }
    return lista
}

class CicloTest {
    private val config = ConfigCiclo()

    @Test
    fun `sem nenhum registro não há previsão`() {
        assertNull(prever(emptyList(), config))
    }

    @Test
    fun `só um início usa o padrão informado`() {
        val p = prever(listOf(Menstruacao(d("2026-11-01"))), ConfigCiclo(duracaoCicloPadrao = 30))!!
        assertEquals(30, p.duracaoArredondada)
        assertEquals(d("2026-12-01"), p.proximoInicio)
        assertEquals(0, p.ciclosUsados)
        assertEquals(2, p.margem)
    }

    @Test
    fun `ciclos regulares de 30 dias preveem 30 com margem mínima`() {
        val p = prever(ciclos("2026-05-01", 30, 30, 30, 30, 30), config)!!
        assertEquals(30, p.duracaoArredondada)
        assertEquals(1, p.margem)
        assertEquals(5, p.ciclosUsados)
    }

    @Test
    fun `os ciclos recentes pesam mais`() {
        // 26, 26, 26, 32, 32, 32: média simples 29, ponderada puxa para 32.
        val p = prever(ciclos("2026-03-01", 26, 26, 26, 32, 32, 32), config)!!
        assertTrue(p.duracaoMedia > 29.5, "média ${p.duracaoMedia}")
    }

    @Test
    fun `ciclo absurdo fica fora da média`() {
        // Um "ciclo" de 60 dias é um registro esquecido, não um ciclo.
        val p = prever(ciclos("2026-01-01", 28, 28, 60, 28, 28), config)!!
        assertEquals(28, p.duracaoArredondada)
        assertEquals(4, p.ciclosUsados)
    }

    @Test
    fun `irregular aumenta a janela`() {
        val p = prever(ciclos("2026-01-01", 24, 34, 25, 33, 26), config)!!
        assertTrue(p.margem >= 3, "margem ${p.margem}")
    }

    @Test
    fun `duração da menstruação vem dos fins marcados`() {
        val ms = listOf(
            Menstruacao(d("2026-09-01"), d("2026-09-04")),
            Menstruacao(d("2026-09-29"), d("2026-10-02")),
        )
        assertEquals(4, prever(ms, config)!!.duracaoMenstruacao)
    }

    @Test
    fun `fases de um ciclo de 28 dias`() {
        val ms = ciclos("2026-10-01", 28, 28, 28)
        val p = prever(ms, config)!!
        val inicio = ms.last().inicio // 2026-11-24
        val hoje = inicio
        fun fase(offset: Int) = diaEm(inicio.plusDays(offset.toLong()), hoje, ms, p)!!.fase
        assertEquals(Fase.MENSTRUACAO, fase(0))
        assertEquals(Fase.MENSTRUACAO, fase(4))
        assertEquals(Fase.FOLICULAR, fase(5))
        assertEquals(Fase.FOLICULAR, fase(12))
        assertEquals(Fase.OVULACAO, fase(13))
        assertEquals(Fase.OVULACAO, fase(14)) // próximo - 14
        assertEquals(Fase.OVULACAO, fase(15))
        assertEquals(Fase.LUTEA, fase(16))
        assertEquals(Fase.TPM, fase(21)) // 7 dias antes do próximo
        assertEquals(Fase.TPM, fase(27))
        assertEquals(Fase.MENSTRUACAO, fase(28)) // ciclo projetado
    }

    @Test
    fun `passou do dia previsto sem registro é atrasada`() {
        val ms = ciclos("2026-09-01", 28, 28) // último 2026-10-27, próximo 2026-11-24
        val p = prever(ms, config)!!
        val previsto = d("2026-11-24")
        val noDia = diaEm(previsto, previsto, ms, p)!!
        assertEquals(Fase.TPM, noDia.fase)
        assertEquals(0, noDia.diasAteProximo)

        val tresDepois = previsto.plusDays(3)
        val atrasada = diaEm(tresDepois, tresDepois, ms, p)!!
        assertEquals(Fase.ATRASADA, atrasada.fase)
        assertEquals(3, atrasada.diasAtraso)
        assertEquals(-3, atrasada.diasAteProximo)
    }

    @Test
    fun `atrasada projeta o próximo ciclo a partir de amanhã`() {
        val ms = ciclos("2026-09-01", 28, 28)
        val p = prever(ms, config)!!
        val hoje = d("2026-11-27") // 3 dias de atraso
        assertEquals(Fase.MENSTRUACAO, diaEm(hoje.plusDays(1), hoje, ms, p)!!.fase)
        assertTrue(diaEm(hoje.plusDays(1), hoje, ms, p)!!.projetado)
    }

    @Test
    fun `dia passado usa o próximo início real`() {
        // Ciclo real de 32 dias: dia 26 (6 antes do próximo real) é TPM, mesmo com média 28.
        val ms = listOf(Menstruacao(d("2026-09-01")), Menstruacao(d("2026-10-03")))
        val p = prever(ms, config)!!
        val dia = diaEm(d("2026-09-27"), d("2026-11-01"), ms, p)!!
        assertEquals(Fase.TPM, dia.fase)
        assertEquals(6, dia.diasAteProximo)
    }

    @Test
    fun `antes do primeiro registro não há dia`() {
        val ms = listOf(Menstruacao(d("2026-10-01")))
        assertNull(diaEm(d("2026-09-01"), d("2026-10-05"), ms, prever(ms, config)!!))
    }

    @Test
    fun `menstruação em curso some depois de marcar o fim`() {
        val ms = listOf(Menstruacao(d("2026-11-01")))
        val p = prever(ms, config)!!
        assertEquals(d("2026-11-01"), menstruacaoEmCurso(d("2026-11-03"), ms, p)?.inicio)
        assertNull(menstruacaoEmCurso(d("2026-11-03"), listOf(Menstruacao(d("2026-11-01"), d("2026-11-03"))), p))
        assertNull(menstruacaoEmCurso(d("2026-11-20"), ms, p))
    }

    @Test
    fun `fim marcado encurta a menstruação daquele ciclo`() {
        val ms = listOf(Menstruacao(d("2026-11-01"), d("2026-11-03")))
        val p = prever(ms, config)!!
        assertEquals(Fase.FOLICULAR, diaEm(d("2026-11-04"), d("2026-11-04"), ms, p)!!.fase)
    }
}
