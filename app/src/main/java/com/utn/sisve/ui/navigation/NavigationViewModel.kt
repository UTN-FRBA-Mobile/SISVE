package com.utn.sisve.ui.navigation

import androidx.lifecycle.ViewModel
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.IncomingDispatchStore
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class NavigationViewModel @Inject constructor(
    sessionManager: SessionManager,
    val incomingDispatchStore: IncomingDispatchStore,
    val ambulanceStatusStore: AmbulanceStatusStore
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.state
}
