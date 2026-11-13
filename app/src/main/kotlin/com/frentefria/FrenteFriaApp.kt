package com.frentefria

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.frentefria.atualizacao.Atualizador
import com.frentefria.avisos.Agendador
import com.frentefria.avisos.Avisos
import com.frentefria.dados.FrenteFriaDatabase
import com.frentefria.dados.Preferencias
import com.frentefria.dados.Repositorio
import com.frentefria.motor.Dados
import com.frentefria.motor.Situacao
import com.frentefria.motor.conteudo.Conteudo
import com.frentefria.trava.Trava
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Injeção de dependência manual: as poucas peças do app moram aqui. */
class FrenteFriaApp : Application() {
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferencias by lazy { Preferencias(this) }
    val repositorio by lazy { Repositorio(FrenteFriaDatabase.criar(this).dao()) }
    val avisos by lazy { Avisos(this) }
    val agendador by lazy { Agendador(this, preferencias) }
    val trava by lazy { Trava(preferencias) }
    val atualizador by lazy { Atualizador(this, preferencias) }
    val conteudo: Conteudo by lazy {
        Conteudo.ler(assets.open("conteudo/conteudo.json").bufferedReader().use { it.readText() })
    }

    /** A situação de agora, lida direto do banco (avisos e receivers). */
    suspend fun situacaoAgora(hoje: LocalDate = LocalDate.now()): Situacao {
        val ajustes = preferencias.atual()
        return Situacao(
            Dados(repositorio.menstruacoes.first(), repositorio.registros.first(), repositorio.eventos.first(), ajustes.config),
            hoje,
        )
    }

    override fun onCreate() {
        super.onCreate()
        avisos.criarCanais()
        escopo.launch { trava.iniciar() }
        escopo.launch { if (preferencias.atual().onboardingFeito) agendador.garantirAgendado() }
        escopo.launch { atualizador.verificar() }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = trava.foiParaFundo()
            override fun onStart(owner: LifecycleOwner) { escopo.launch { trava.voltou() } }
        })
    }
}
