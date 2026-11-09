package com.frentefria.avisos

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.frentefria.FrenteFriaApp
import com.frentefria.MainActivity
import com.frentefria.R
import com.frentefria.motor.avisos.Aviso
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Um canal só, com nome neutro ("Previsão"): nas configurações do Android
 * também não pode aparecer nada que entregue o jogo.
 */
class Avisos(private val context: Context) {

    private val gerenciador = NotificationManagerCompat.from(context)

    fun criarCanais() {
        gerenciador.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CANAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Previsão")
                    .setDescription("Alertas e resumo do dia")
                    .build(),
            ),
        )
    }

    fun ligadas(): Boolean = gerenciador.areNotificationsEnabled()

    @SuppressLint("MissingPermission") // conferido em ligadas()
    fun publicar(avisos: List<Aviso>, limpar: Boolean) {
        if (!ligadas()) return
        if (limpar) gerenciador.cancelAll()
        avisos.forEach { a ->
            val id = a.chave.hashCode()
            val n = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setColor(AZUL)
                .setContentTitle(a.titulo)
                .setContentText(a.texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(a.texto))
                .setContentIntent(abrirApp())
                .setAutoCancel(true)
                // Na tela de bloqueio, só "Frente Fria" e o título: nada do texto.
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .apply { if (a.comecou) addAction(0, "Começou", comecou(id)) }
                .build()
            gerenciador.notify(id, n)
        }
    }

    fun cancelar(id: Int) = gerenciador.cancel(id)

    private fun abrirApp(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun comecou(id: Int): PendingIntent {
        val intent = Intent(context, ComecouReceiver::class.java)
            .setData(Uri.parse("frentefria://comecou/$id"))
            .putExtra(ComecouReceiver.ID, id)
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private companion object {
        const val CANAL = "previsao"
        const val AZUL = 0xFF3B6EA8.toInt()
    }
}

/** Botão "Começou" da notificação: registra o início hoje, sem abrir o app. */
class ComecouReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as FrenteFriaApp
        val id = intent.getIntExtra(ID, 0)
        val pendente = goAsync()
        app.escopo.launch {
            try {
                app.repositorio.registrarInicio(LocalDate.now())
                app.avisos.cancelar(id)
            } finally {
                pendente.finish()
            }
        }
    }

    companion object {
        const val ID = "id"
    }
}
