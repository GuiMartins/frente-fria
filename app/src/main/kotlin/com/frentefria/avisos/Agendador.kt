package com.frentefria.avisos

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.frentefria.dados.Preferencias
import com.frentefria.motor.avisos.Horario
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Igual ao Jardim: cada aviso é um OneTimeWork único que, ao rodar, agenda o
 * do dia seguinte (APPEND_OR_REPLACE). PeriodicWork escorrega o horário.
 */
class Agendador(private val context: Context, private val preferencias: Preferencias) {

    private val workManager get() = WorkManager.getInstance(context)

    suspend fun garantirAgendado() = Horario.entries.forEach { agendar(it, ExistingWorkPolicy.KEEP) }

    suspend fun reagendar(h: Horario) = agendar(h, ExistingWorkPolicy.REPLACE)

    suspend fun agendarProximo(h: Horario) = agendar(h, ExistingWorkPolicy.APPEND_OR_REPLACE)

    /** "Testar agora": publica o que sairia hoje de manhã, mesmo num dia calmo. */
    fun testarAgora() {
        val pedido = OneTimeWorkRequestBuilder<AvisoWorker>()
            .setInputData(workDataOf(AvisoWorker.HORARIO to Horario.MANHA.name, AvisoWorker.AVULSO to true))
            .build()
        workManager.enqueueUniqueWork("aviso-teste", ExistingWorkPolicy.REPLACE, pedido)
    }

    fun cancelarTudo() = workManager.cancelAllWork()

    private suspend fun agendar(h: Horario, politica: ExistingWorkPolicy) {
        val agora = LocalDateTime.now()
        val hojeNoHorario = agora.toLocalDate().atTime(preferencias.horario(h))
        val proxima = if (hojeNoHorario.isAfter(agora)) hojeNoHorario else hojeNoHorario.plusDays(1)
        val pedido = OneTimeWorkRequestBuilder<AvisoWorker>()
            .setInitialDelay(Duration.between(agora, proxima).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(AvisoWorker.HORARIO to h.name))
            .build()
        workManager.enqueueUniqueWork("aviso-${h.name.lowercase()}", politica, pedido)
    }
}
