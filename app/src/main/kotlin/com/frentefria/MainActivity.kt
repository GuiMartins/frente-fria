package com.frentefria

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frentefria.ui.FrenteFriaTema
import com.frentefria.ui.Navegacao
import com.frentefria.ui.PrincipalViewModel
import com.frentefria.ui.onboarding.Onboarding
import com.frentefria.ui.trava.TravaTela

/** FragmentActivity porque o BiometricPrompt precisa dela. */
class MainActivity : FragmentActivity() {

    private val viewModel: PrincipalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FrenteFriaApp
        setContent {
            FrenteFriaTema {
                val estado by viewModel.estado.collectAsStateWithLifecycle()
                val travado by app.trava.travado.collectAsStateWithLifecycle()
                val e = estado ?: return@FrenteFriaTema
                // Com trava, o conteúdo some da tela de recentes e não sai em print.
                LaunchedEffect(e.ajustes.travaLigada) {
                    if (e.ajustes.travaLigada) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                when {
                    !e.ajustes.onboardingFeito -> Onboarding()
                    travado && e.ajustes.travaLigada -> TravaTela(e.ajustes.biometria)
                    else -> Navegacao(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.atualizarHoje()
    }
}
