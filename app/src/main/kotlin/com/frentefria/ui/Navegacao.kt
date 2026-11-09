package com.frentefria.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.frentefria.motor.texto.Modo
import com.frentefria.ui.calendario.CalendarioTela
import com.frentefria.ui.config.CiclosTela
import com.frentefria.ui.config.ConfigTela
import com.frentefria.ui.hoje.HojeTela
import com.frentefria.ui.insights.InsightsTela
import com.frentefria.ui.kit.KitTela
import com.frentefria.ui.manual.ManualTela
import com.frentefria.ui.registro.RegistroTela
import java.time.LocalDate

private data class Aba(val rota: String, val emoji: String, val clima: String, val direto: String)

private val ABAS = listOf(
    Aba("hoje", "🌤️", "Hoje", "Hoje"),
    Aba("calendario", "📅", "Previsão", "Calendário"),
    Aba("insights", "📊", "Dados", "Insights"),
    Aba("manual", "📖", "Almanaque", "Manual"),
    Aba("kit", "🎒", "Kit", "Kit"),
)

@Composable
fun Navegacao(viewModel: PrincipalViewModel) {
    val nav = rememberNavController()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val e = estado ?: return
    val atual by nav.currentBackStackEntryAsState()
    val rota = atual?.destination?.route
    val naAba = ABAS.any { it.rota == rota }

    fun registrar(data: LocalDate, dr: Boolean = false) = nav.navigate("registro/$data?dr=$dr")

    Scaffold(
        bottomBar = {
            if (naAba) NavigationBar {
                ABAS.forEach { aba ->
                    NavigationBarItem(
                        selected = rota == aba.rota,
                        onClick = {
                            nav.navigate(aba.rota) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(aba.emoji, fontSize = 20.sp) },
                        label = { Text(if (e.ajustes.modo == Modo.CLIMA) aba.clima else aba.direto) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "hoje", modifier = Modifier.padding(bottom = if (naAba) padding.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f))) {
            composable("hoje") {
                HojeTela(e, viewModel, aoRegistrar = { registrar(it) }, aoDr = { registrar(LocalDate.now(), dr = true) }, aoConfigurar = { nav.navigate("config") })
            }
            composable("calendario") { CalendarioTela(e, viewModel, aoRegistrar = { registrar(it) }) }
            composable("insights") { InsightsTela(e) }
            composable("manual") { ManualTela(e, viewModel.conteudo) }
            composable("kit") { KitTela(e, viewModel) }
            composable(
                "registro/{data}?dr={dr}",
                arguments = listOf(navArgument("data") { type = NavType.StringType }, navArgument("dr") { type = NavType.BoolType; defaultValue = false }),
            ) { entrada ->
                RegistroTela(
                    e,
                    viewModel,
                    data = LocalDate.parse(entrada.arguments!!.getString("data")),
                    comDr = entrada.arguments!!.getBoolean("dr"),
                    aoVoltar = { nav.popBackStack() },
                )
            }
            composable("config") { ConfigTela(e, aoVoltar = { nav.popBackStack() }, aoAbrirCiclos = { nav.navigate("ciclos") }) }
            composable("ciclos") { CiclosTela(e, viewModel, aoVoltar = { nav.popBackStack() }) }
        }
    }
}
