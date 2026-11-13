package com.utn.sisve.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.IncomingDispatchStore
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.repository.ApiRepository
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NavigationViewModel @Inject constructor(
    sessionManager: SessionManager,
    val incomingDispatchStore: IncomingDispatchStore,
    val ambulanceStatusStore: AmbulanceStatusStore,
    private val activeDispatchStore: ActiveDispatchStore,
    private val apiRepository: ApiRepository
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.state

    fun finalizeEmergency() {
        viewModelScope.launch {
            activeDispatchStore.clear()
            apiRepository.finalizeDispatch()
        }
    }
}
