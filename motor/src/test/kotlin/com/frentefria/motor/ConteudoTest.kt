package com.frentefria.motor

import com.frentefria.motor.agenda.TipoEvento
import com.frentefria.motor.agenda.dicaDeConflito
import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.ciclo.Fase
import com.frentefria.motor.ciclo.diaEm
import com.frentefria.motor.ciclo.prever
import com.frentefria.motor.conteudo.Conteudo
import com.frentefria.motor.conteudo.conteudoDoDia
import com.frentefria.motor.registro.Sintoma
import com.frentefria.motor.registro.Tatica
import com.frentefria.motor.texto.Modo
import com.frentefria.motor.texto.entrega
import com.frentefria.motor.texto.nomeDaFase
import com.frentefria.motor.texto.nomeDaTatica
import com.frentefria.motor.texto.nomeDoSintoma
import com.frentefria.motor.texto.resumoDaFase
import com.frentefria.motor.texto.termos
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConteudoTest {
    private val conteudo: Conteudo by lazy {
        Conteudo.ler(File(System.getProperty("conteudo.dir"), "conteudo.json").readText())
    }

    @Test
    fun `no modo clima nenhum texto do conteúdo entrega o jogo`() {
        val problemas = conteudo.todos().mapNotNull { (lista, item) ->
            val texto = item.em(Modo.CLIMA) + " " + (item.porqueEm(Modo.CLIMA) ?: "")
            entrega(texto)?.let { "$lista: \"${item.texto.take(50)}\" tem \"$it\" e não tem versão clima" }
        } + conteudo.oQueAcontece.mapNotNull { (f, t) -> entrega(t.em(Modo.CLIMA))?.let { "o_que_acontece $f: $it" } }
        assertTrue(problemas.isEmpty(), problemas.joinToString("\n"))
    }

    @Test
    fun `toda fase tem conteúdo suficiente`() {
        val fases = listOf(Fase.MENSTRUACAO, Fase.FOLICULAR, Fase.OVULACAO, Fase.LUTEA, Fase.TPM, Fase.ATRASADA)
        fases.forEach { f ->
            assertNotNull(conteudo.oQueAcontece[f], "o_que_acontece sem $f")
            assertTrue(conteudo.manchetes.count { it.serve(f) } >= 3, "manchetes de $f")
            assertTrue(conteudo.faca.count { it.serve(f) } >= 4, "faça de $f")
            assertTrue(conteudo.naoFaca.count { it.serve(f) } >= 4, "não faça de $f")
            assertTrue(conteudo.dizer.count { it.serve(f) } >= 2, "dizer de $f")
            assertTrue(conteudo.agrados.count { it.serve(f) } >= 2, "agrados de $f")
        }
        assertTrue(conteudo.nuncaDizer.size >= 10)
    }

    @Test
    fun `todo nível de 0 a 4 tem manchete própria`() {
        (0..4).forEach { n -> assertTrue(conteudo.manchetes.any { n in it.niveis }, "nível $n") }
    }

    @Test
    fun `o conteúdo do dia é o mesmo o dia inteiro e muda entre dias`() {
        val ms = ciclos("2026-09-01", 28, 28)
        val p = prever(ms, ConfigCiclo())!!
        val dia = d("2026-11-20")
        val fase = diaEm(dia, dia, ms, p)!!.fase
        val a = conteudoDoDia(conteudo, dia, fase, 2)
        assertEquals(a, conteudoDoDia(conteudo, dia, fase, 2))
        assertEquals(3, a.faca.size)
        val semana = (0..6).map { conteudoDoDia(conteudo, dia.plusDays(it.toLong()), Fase.TPM, 2).faca }
        assertTrue(semana.toSet().size > 1)
    }

    @Test
    fun `textos fixos do modo clima não entregam o jogo`() {
        Fase.entries.forEach {
            assertNull(entrega(nomeDaFase(it, Modo.CLIMA)))
            assertNull(entrega(resumoDaFase(it, Modo.CLIMA)))
        }
        Sintoma.entries.forEach { assertNull(entrega(nomeDoSintoma(it, Modo.CLIMA))) }
        Tatica.entries.forEach { assertNull(entrega(nomeDaTatica(it, Modo.CLIMA))) }
        with(termos(Modo.CLIMA)) { listOf(tpm, menstruacao, menstruouHoje, parouHoje, dr, ciclo).forEach { assertNull(entrega(it), it) } }
        val ms = ciclos("2026-09-01", 28, 28)
        val p = prever(ms, ConfigCiclo())!!
        (0..40).forEach { k ->
            val dia = diaEm(d("2026-10-27").plusDays(k.toLong()), d("2026-11-01"), ms, p)!!
            TipoEvento.entries.forEach { t -> assertNull(entrega(dicaDeConflito(t, dia, Modo.CLIMA)), "$t ${dia.fase}") }
        }
    }
}
