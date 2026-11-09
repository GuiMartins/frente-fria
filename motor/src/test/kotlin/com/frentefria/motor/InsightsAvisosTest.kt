package com.frentefria.motor

import com.frentefria.motor.agenda.Evento
import com.frentefria.motor.agenda.TipoEvento
import com.frentefria.motor.avisos.Horario
import com.frentefria.motor.avisos.OpcoesAvisos
import com.frentefria.motor.avisos.TipoAviso
import com.frentefria.motor.avisos.avisosDe
import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.prever
import com.frentefria.motor.insights.acuracia
import com.frentefria.motor.insights.calcularInsights
import com.frentefria.motor.registro.Humor
import com.frentefria.motor.registro.RegistroDia
import com.frentefria.motor.registro.Tatica
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.entrega
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsAvisosTest {
    private val config = ConfigCiclo()

    @Test
    fun `DR concentrada na TPM dá multiplicador alto`() {
        val ms = ciclos("2026-06-01", 28, 28, 28)
        val regs = ms.drop(1).flatMap { m ->
            (1..20).map { k ->
                val data = m.inicio.minusDays(k.toLong())
                data to if (k <= 5) RegistroDia(data, Humor.RUIM, dr = if (k % 2 == 0) 2 else 0) else RegistroDia(data, Humor.BOM, dr = if (k == 15) 1 else 0)
            }
        }.toMap()
        val hoje = ms.last().inicio.plusDays(3)
        val i = calcularInsights(ms, regs, prever(ms, config), config, hoje)
        val mult = assertNotNull(i.multiplicadorDrTpm)
        assertTrue(mult > 3, "multiplicador $mult")
        assertTrue(i.porFase.getValue(Fase.TPM).humorMedio!! < i.porFase.getValue(Fase.FOLICULAR).humorMedio!!)
        assertEquals(3, i.regularidade!!.ciclos)
        assertEquals("regular", i.regularidade!!.classificacao)
    }

    @Test
    fun `ranking de táticas ordena pela taxa de sucesso`() {
        val ms = ciclos("2026-10-01", 28)
        val regs = (1..6).associate { k ->
            val data = d("2026-10-01").plusDays(k.toLong())
            data to RegistroDia(data, taticas = mapOf(Tatica.CHOCOLATE to true, Tatica.CONCORDAR to (k % 3 == 0)))
        }
        val i = calcularInsights(ms, regs, prever(ms, config), config, d("2026-11-09"))
        assertEquals(Tatica.CHOCOLATE, i.taticas.first().tatica)
        assertEquals(1.0, i.taticas.first().taxaSucesso)
    }

    @Test
    fun `acurácia precisa de 2 previsões`() {
        assertNull(acuracia(ciclos("2026-09-01", 28), config))
        val a = acuracia(ciclos("2026-01-01", 28, 28, 28, 28), config)!!
        assertEquals(0.0, a.erroMedio)
    }

    @Test
    fun `curva de humor alinha pelo início`() {
        val ms = ciclos("2026-09-01", 28, 28)
        val datas = listOf(ms[1].inicio.minusDays(1), ms[2].inicio.minusDays(1), ms[2].inicio)
        val regs = datas.associateWith { RegistroDia(it, Humor.PESSIMO) }
        val curva = calcularInsights(ms, regs, prever(ms, config), config, d("2026-11-09")).curvaHumor
        assertEquals(2, curva.single { it.diaRelativo == -1 }.amostras)
        assertEquals(1, curva.single { it.diaRelativo == 0 }.amostras)
    }

    private fun situacao(hoje: String, eventos: List<Evento> = emptyList()) =
        Situacao(Dados(ciclos("2026-09-01", 28, 28), emptyMap(), eventos, config), d(hoje)) // próximo: 2026-11-24

    @Test
    fun `aviso de TPM chegando 2 dias antes e kit 3 dias antes`() {
        // TPM de 7 dias começa em 2026-11-17.
        val doisAntes = avisosDe(situacao("2026-11-15"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), listOf("Chocolate"))
        assertTrue(doisAntes.any { it.tipo == TipoAviso.TPM_CHEGANDO })
        val tresAntes = avisosDe(situacao("2026-11-14"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), listOf("Chocolate"))
        assertTrue(tresAntes.any { it.tipo == TipoAviso.REPOR_KIT })
        assertTrue(avisosDe(situacao("2026-11-14"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), emptyList()).none { it.tipo == TipoAviso.REPOR_KIT })
    }

    @Test
    fun `prevista hoje e atrasada têm botão Começou`() {
        val hoje = avisosDe(situacao("2026-11-24"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), emptyList())
        assertTrue(hoje.single { it.tipo == TipoAviso.PREVISTA_HOJE }.comecou)
        val atrasada = avisosDe(situacao("2026-11-26"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), emptyList())
        assertEquals("Atrasou 2 dias", atrasada.single { it.tipo == TipoAviso.ATRASADA }.titulo)
    }

    @Test
    fun `dia calmo sem mudança de fase não manda resumo`() {
        assertTrue(avisosDe(situacao("2026-11-04"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), emptyList()).none { it.tipo == TipoAviso.RESUMO })
        assertTrue(avisosDe(situacao("2026-11-04"), Horario.MANHA, OpcoesAvisos(Modo.DIRETO, resumoTodoDia = true), emptyList()).any { it.tipo == TipoAviso.RESUMO })
    }

    @Test
    fun `evento em conflito avisa 3 dias antes`() {
        val e = Evento(7, d("2026-11-20"), "Jantar com os sogros", TipoEvento.FAMILIA)
        val avisos = avisosDe(situacao("2026-11-17", listOf(e)), Horario.MANHA, OpcoesAvisos(Modo.DIRETO), emptyList())
        assertEquals("EVENTO:7", avisos.single { it.tipo == TipoAviso.EVENTO }.chave)
    }

    @Test
    fun `no modo clima nenhum aviso entrega o jogo`() {
        val e = Evento(1, d("2026-11-26"), "Viagem", TipoEvento.VIAGEM)
        val datas = (0..40).map { d("2026-11-01").plusDays(it.toLong()).toString() }
        datas.forEach { data ->
            listOf(Horario.MANHA, Horario.NOITE).forEach { h ->
                avisosDe(situacao(data, listOf(e)), h, OpcoesAvisos(Modo.CLIMA, resumoTodoDia = true), listOf("Chocolate")).forEach { a ->
                    assertNull(entrega(a.titulo + " " + a.texto), "$data: ${a.titulo} / ${a.texto}")
                }
            }
        }
    }
}
