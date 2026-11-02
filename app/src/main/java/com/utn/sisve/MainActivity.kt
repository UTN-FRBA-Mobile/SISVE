package com.utn.sisve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.utn.sisve.data.local.AmbulanceSession
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.session.SessionManager
import com.utn.sisve.ui.navigation.AppNavGraph
import com.utn.sisve.ui.navigation.AppRoute
import com.utn.sisve.ui.theme.SisveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ambulanceSession: AmbulanceSession

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var ambulanceStatusStore: AmbulanceStatusStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeSessionExpiration()

        setContent {
            SisveTheme {
                val startDestination = if (ambulanceSession.isConfigured()) {
                    AppRoute.Login.route
                } else {
                    AppRoute.Setup.route
                }

                AppNavGraph(startDestination = startDestination)
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        // Android llama a este método ante interacción real, no por GPS ni notificaciones.
        sessionManager.recordInteraction(ambulanceStatusStore.status.value)
    }

    private fun observeSessionExpiration() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Al regresar del segundo plano se comprueba inmediatamente el tiempo transcurrido.
                sessionManager.state.collectLatest { state ->
                    if (state is SessionState.Active) {
                        while (true) {
                            sessionManager.checkExpiration(ambulanceStatusStore.status.value)

                            val remaining = sessionManager.remainingInactivityMillis() ?: break

                            // Si hubo interacción durante la espera, al despertar se calcula un nuevo plazo.
                            delay(remaining.coerceAtLeast(1L))
                        }
                    }
                }
            }
        }
    }
}