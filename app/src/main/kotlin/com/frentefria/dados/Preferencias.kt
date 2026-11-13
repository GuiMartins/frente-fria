package com.frentefria.dados

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.frentefria.motor.atualizacao.Atualizacao
import com.frentefria.motor.avisos.Horario
import com.frentefria.motor.ciclo.ConfigCiclo
import com.frentefria.motor.texto.Modo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore("preferencias")

/** O que ele escolhe no onboarding e nas configurações. */
data class Ajustes(
    val onboardingFeito: Boolean,
    val modo: Modo,
    val apelido: String,
    val config: ConfigCiclo,
    val travaLigada: Boolean,
    val biometria: Boolean,
    val manha: LocalTime,
    val noite: LocalTime,
    val resumoTodoDia: Boolean,
    val lembreteNoite: Boolean,
) {
    /** "ela", ou o apelido que ele escolheu. */
    val nomeDela: String get() = apelido.ifBlank { "ela" }
}

class Preferencias(private val context: Context) {

    val ajustes: Flow<Ajustes> = context.dataStore.data.map { it.ajustes() }.distinctUntilChanged()

    suspend fun atual(): Ajustes = ajustes.first()

    private fun Preferences.ajustes() = Ajustes(
        onboardingFeito = this[ONBOARDING] ?: false,
        modo = this[MODO]?.let { m -> Modo.entries.firstOrNull { it.name == m } } ?: Modo.CLIMA,
        apelido = this[APELIDO] ?: "",
        config = ConfigCiclo(
            duracaoCicloPadrao = this[CICLO] ?: 28,
            duracaoMenstruacaoPadrao = this[MENSTRUACAO] ?: 5,
            tpmFixa = this[TPM_FIXA]?.takeIf { it > 0 },
        ),
        travaLigada = this[PIN_HASH] != null,
        biometria = this[BIOMETRIA] ?: false,
        manha = this[MANHA]?.let(LocalTime::parse) ?: LocalTime.of(8, 0),
        noite = this[NOITE]?.let(LocalTime::parse) ?: LocalTime.of(21, 0),
        resumoTodoDia = this[RESUMO_TODO_DIA] ?: false,
        lembreteNoite = this[LEMBRETE_NOITE] ?: true,
    )

    suspend fun horario(h: Horario): LocalTime = atual().let { if (h == Horario.MANHA) it.manha else it.noite }

    suspend fun mudarHorario(h: Horario, t: LocalTime) = context.dataStore.edit { it[if (h == Horario.MANHA) MANHA else NOITE] = t.toString() }
    suspend fun mudarModo(m: Modo) = context.dataStore.edit { it[MODO] = m.name }
    suspend fun mudarApelido(a: String) = context.dataStore.edit { it[APELIDO] = a.trim() }
    suspend fun mudarCiclo(dias: Int) = context.dataStore.edit { it[CICLO] = dias }
    suspend fun mudarMenstruacao(dias: Int) = context.dataStore.edit { it[MENSTRUACAO] = dias }
    /** 0 ou null volta ao automático. */
    suspend fun mudarTpmFixa(dias: Int?) = context.dataStore.edit { it[TPM_FIXA] = dias ?: 0 }
    suspend fun mudarBiometria(b: Boolean) = context.dataStore.edit { it[BIOMETRIA] = b }
    suspend fun mudarResumoTodoDia(b: Boolean) = context.dataStore.edit { it[RESUMO_TODO_DIA] = b }
    suspend fun mudarLembreteNoite(b: Boolean) = context.dataStore.edit { it[LEMBRETE_NOITE] = b }
    suspend fun concluirOnboarding() = context.dataStore.edit { it[ONBOARDING] = true }

    suspend fun pin(): Pair<String, String>? = context.dataStore.data.first().let { p ->
        val h = p[PIN_HASH] ?: return@let null
        h to (p[PIN_SAL] ?: "")
    }

    suspend fun salvarPin(hash: String, sal: String) = context.dataStore.edit { it[PIN_HASH] = hash; it[PIN_SAL] = sal }
    suspend fun removerPin() = context.dataStore.edit { it.remove(PIN_HASH); it.remove(PIN_SAL); it[BIOMETRIA] = false }

    /** A última release consultada no GitHub; null antes da primeira ou se ela não tinha APK. */
    val atualizacao: Flow<Atualizacao?> = context.dataStore.data
        .map { prefs -> prefs[ATUALIZACAO]?.let { Json.decodeFromString(Atualizacao.serializer(), it) } }
        .distinctUntilChanged()

    suspend fun atualizacaoConsultadaEm(): Instant? = context.dataStore.data.first()[ATUALIZACAO_EM]?.let(Instant::parse)

    suspend fun guardarAtualizacao(atualizacao: Atualizacao?, em: Instant) {
        context.dataStore.edit {
            if (atualizacao == null) it.remove(ATUALIZACAO) else it[ATUALIZACAO] = Json.encodeToString(Atualizacao.serializer(), atualizacao)
            it[ATUALIZACAO_EM] = em.toString()
        }
    }

    suspend fun limparTudo() = context.dataStore.edit { it.clear() }

    private companion object {
        val ONBOARDING = booleanPreferencesKey("onboarding")
        val MODO = stringPreferencesKey("modo")
        val APELIDO = stringPreferencesKey("apelido")
        val CICLO = intPreferencesKey("ciclo")
        val MENSTRUACAO = intPreferencesKey("menstruacao")
        val TPM_FIXA = intPreferencesKey("tpm_fixa")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SAL = stringPreferencesKey("pin_sal")
        val BIOMETRIA = booleanPreferencesKey("biometria")
        val MANHA = stringPreferencesKey("manha")
        val NOITE = stringPreferencesKey("noite")
        val RESUMO_TODO_DIA = booleanPreferencesKey("resumo_todo_dia")
        val LEMBRETE_NOITE = booleanPreferencesKey("lembrete_noite")
        val ATUALIZACAO = stringPreferencesKey("atualizacao")
        val ATUALIZACAO_EM = stringPreferencesKey("atualizacao_em")
    }
}
