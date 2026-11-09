package com.frentefria.avisos

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.frentefria.FrenteFriaApp
import com.frentefria.motor.avisos.Horario
import com.frentefria.motor.avisos.OpcoesAvisos
import com.frentefria.motor.avisos.avisosDe
import kotlinx.coroutines.flow.first

/** Lê o banco, calcula a situação e publica. O estado vem sempre do banco. */
class AvisoWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as FrenteFriaApp
        val horario = inputData.getString(HORARIO)?.let(Horario::valueOf) ?: return Result.failure()
        val avulso = inputData.getBoolean(AVULSO, false)
        try {
            val ajustes = app.preferencias.atual()
            if (ajustes.onboardingFeito) {
                val situacao = app.situacaoAgora()
                val faltando = app.repositorio.kit.first().filter { it.estoque && !it.tem }.map { it.nome }
                val opcoes = OpcoesAvisos(ajustes.modo, resumoTodoDia = ajustes.resumoTodoDia || avulso, lembreteNoite = ajustes.lembreteNoite)
                app.avisos.publicar(avisosDe(situacao, horario, opcoes, faltando), limpar = horario == Horario.MANHA)
            }
        } finally {
            if (!avulso) app.agendador.agendarProximo(horario)
        }
        return Result.success()
    }

    companion object {
        const val HORARIO = "horario"
        const val AVULSO = "avulso"
    }
}
