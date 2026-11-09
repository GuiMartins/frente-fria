package com.frentefria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frentefria.FrenteFriaApp
import com.frentefria.dados.Ajustes
import com.frentefria.motor.Dados
import com.frentefria.motor.Situacao
import com.frentefria.motor.agenda.Evento
import com.frentefria.motor.kit.ItemKit
import com.frentefria.motor.registro.RegistroDia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Tudo o que as telas mostram, recalculado a cada mudança no banco ou nas preferências. */
data class Estado(
    val ajustes: Ajustes,
    val situacao: Situacao,
    val kit: List<ItemKit>,
)

/**
 * Um ViewModel só para as telas principais: todas leem a mesma situação, e o
 * motor recalcula tudo em milissegundos.
 */
class PrincipalViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FrenteFriaApp
    private val repo = app.repositorio

    private val hoje = MutableStateFlow(LocalDate.now())

    /** Chamado ao voltar para o app: vira o dia sem precisar reabrir. */
    fun atualizarHoje() { hoje.value = LocalDate.now() }

    val conteudo get() = app.conteudo

    val estado: StateFlow<Estado?> = combine(
        combine(app.preferencias.ajustes, repo.menstruacoes, repo.registros, repo.eventos, repo.kit) { a, m, r, e, k -> listOf(a, m, r, e, k) },
        hoje,
    ) { l, dia ->
        @Suppress("UNCHECKED_CAST")
        val ajustes = l[0] as Ajustes
        @Suppress("UNCHECKED_CAST")
        Estado(
            ajustes,
            Situacao(Dados(l[1] as List<com.frentefria.motor.ciclo.Menstruacao>, l[2] as Map<LocalDate, RegistroDia>, l[3] as List<Evento>, ajustes.config), dia),
            l[4] as List<ItemKit>,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun menstruou(data: LocalDate) = viewModelScope.launch { repo.registrarInicio(data) }
    fun parou(data: LocalDate) = viewModelScope.launch { repo.registrarFim(data) }
    fun apagarFim(inicio: LocalDate) = viewModelScope.launch { repo.apagarFim(inicio) }
    fun apagarMenstruacao(inicio: LocalDate) = viewModelScope.launch { repo.apagarMenstruacao(inicio) }
    fun salvarRegistro(r: RegistroDia) = viewModelScope.launch { repo.salvarRegistro(r) }
    fun apagarRegistro(data: LocalDate) = viewModelScope.launch { repo.apagarRegistro(data) }
    fun salvarEvento(e: Evento) = viewModelScope.launch { repo.salvarEvento(e) }
    fun apagarEvento(id: Long) = viewModelScope.launch { repo.apagarEvento(id) }
    fun salvarItem(i: ItemKit) = viewModelScope.launch { repo.salvarItem(i) }
    fun apagarItem(id: Long) = viewModelScope.launch { repo.apagarItem(id) }
}
