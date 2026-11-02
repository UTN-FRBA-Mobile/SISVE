package com.utn.sisve.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import com.utn.sisve.data.local.AmbulanceSession
import com.utn.sisve.repository.ApiRepository
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class LoginError {
    INVALID_PIN,
    INVALID_CREDENTIALS,
    CONNECTION,
    SERVER,
    INVALID_RESPONSE
}

data class LoginUiState(
    val ambulanceId: String = "",
    val pin: String = "",
    val isLoading: Boolean = false,
    val loginSucceeded: Boolean = false,
    val error: LoginError? = null
) {
    val canSubmit: Boolean
        get() = ambulanceId.isNotBlank() && pin.length in 4..6 && !isLoading && !loginSucceeded
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val ambulanceSession: AmbulanceSession,
    private val apiRepository: ApiRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    // El PIN solo vive en memoria: no usar SavedStateHandle ni preferencias para guardarlo.
    private val _uiState = MutableStateFlow(
        LoginUiState(ambulanceId = ambulanceSession.getAmbulanceId())
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun appendDigit(digit: Int) {
        if (digit !in 0..9) {
            return
        }

        editPin { if (it.length < 6) it + digit else it }
    }

    fun deleteLastDigit() {
        editPin { it.dropLast(1) }
    }

    fun clearPin() {
        editPin { "" }
    }

    private fun editPin(transform: (String) -> String) {
        _uiState.update {
            if (it.isLoading || it.loginSucceeded) it
            else it.copy(pin = transform(it.pin), error = null)
        }
    }

    fun login() {
        val current = _uiState.value

        if (current.isLoading || current.loginSucceeded) {
            return
        }

        val ambulanceId = ambulanceSession.getAmbulanceId()

        if (ambulanceId.isBlank()) {
            return
        }

        if (!current.pin.matches(Regex("[0-9]{4,6}"))) {
            _uiState.update { it.copy(error = LoginError.INVALID_PIN) }
            return
        }

        _uiState.update { it.copy(ambulanceId = ambulanceId, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val response = apiRepository.login(ambulanceId, current.pin)
                val operator = response.operator

                if (
                    response.token.isBlank() || response.ambulanceId != ambulanceId ||
                    operator.id.isBlank() || operator.name.isBlank()
                ) {
                    _uiState.update { it.copy(error = LoginError.INVALID_RESPONSE) }
                    return@launch
                }

                sessionManager.startSession(
                    token = response.token,
                    ambulanceId = response.ambulanceId,
                    operatorId = operator.id,
                    operatorName = operator.name
                )

                _uiState.update { it.copy(pin = "", loginSucceeded = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: HttpException) {
                val loginError = when (error.code()) {
                    401, 403 -> LoginError.INVALID_CREDENTIALS
                    else -> LoginError.SERVER
                }

                _uiState.update {
                    it.copy(
                        error = loginError,
                        pin = if (loginError == LoginError.INVALID_CREDENTIALS) "" else it.pin
                    )
                }
            } catch (_: IOException) {
                _uiState.update { it.copy(error = LoginError.CONNECTION) }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = LoginError.INVALID_RESPONSE) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
