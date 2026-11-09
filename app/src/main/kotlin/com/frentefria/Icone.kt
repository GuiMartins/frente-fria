package com.frentefria

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.frentefria.motor.texto.Modo

/**
 * O ícone do launcher troca com o modo: dois activity-alias (nuvem e direto),
 * só um ligado por vez. Desligar o alias que abriu o app fecha a tarefa dele,
 * então liga o novo, reabre o app por ele e só depois desliga o antigo.
 */
object Icone {
    private fun alias(context: Context, modo: Modo) =
        ComponentName(context, "com.frentefria." + if (modo == Modo.CLIMA) "IconeClima" else "IconeDireto")

    fun aplicar(context: Context, modo: Modo, reabrir: Activity? = null) {
        val pm = context.packageManager
        val ligar = alias(context, modo)
        val desligar = alias(context, if (modo == Modo.CLIMA) Modo.DIRETO else Modo.CLIMA)
        val jaEstava = pm.getComponentEnabledSetting(desligar) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
            pm.getComponentEnabledSetting(ligar) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (jaEstava) return
        pm.setComponentEnabledSetting(ligar, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        if (reabrir != null) {
            // Fecha a tarefa atual antes: com singleTop, o pedido cairia nela mesma.
            reabrir.finishAndRemoveTask()
            context.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ligar)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        val desligarAntigo = {
            pm.setComponentEnabledSetting(desligar, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        // Desligar na hora fecharia também a tarefa nova, que ainda está subindo.
        if (reabrir != null) Handler(Looper.getMainLooper()).postDelayed(desligarAntigo, 1500) else desligarAntigo()
    }
}
