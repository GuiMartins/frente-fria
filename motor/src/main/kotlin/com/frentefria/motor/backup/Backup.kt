package com.frentefria.motor.backup

import com.frentefria.motor.agenda.Evento
import com.frentefria.motor.ciclo.Menstruacao
import com.frentefria.motor.kit.ItemKit
import com.frentefria.motor.registro.RegistroDia
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** As preferências que vão no backup (o PIN não vai: quem restaura cria outro). */
@Serializable
data class PreferenciasBackup(
    val modo: String,
    val apelido: String,
    val duracaoCicloPadrao: Int,
    val duracaoMenstruacaoPadrao: Int,
    val tpmFixa: Int? = null,
)

@Serializable
data class Backup(
    val versao: Int = 1,
    val geradoEm: String,
    val menstruacoes: List<Menstruacao>,
    val registros: List<RegistroDia>,
    val eventos: List<Evento>,
    val kit: List<ItemKit>,
    val preferencias: PreferenciasBackup,
) {
    fun paraJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
        fun deJson(texto: String): Backup = json.decodeFromString(serializer(), texto)
    }
}
