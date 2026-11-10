package com.utn.sisve.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.utn.sisve.data.local.AmbulancePreferences
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    private val ambulancePreferences: AmbulancePreferences,
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

    fun resetAmbulanceConfiguration() {
        ambulancePreferences.clearAmbulance()
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

                registerFcmToken()
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

    private suspend fun registerFcmToken() {
        try {
            val registered = withTimeoutOrNull(10_000) {
                val token = suspendCancellableCoroutine { continuation ->
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!continuation.isActive) {
                            return@addOnCompleteListener
                        }

                        if (task.isSuccessful && !task.result.isNullOrBlank()) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resumeWithException(
                                task.exception ?: IOException("Firebase no devolvió un token FCM.")
                            )
                        }
                    }
                }

                apiRepository.registerFcmToken(token)
                true
            } ?: false

            if (!registered) {
                Log.e(TAG, "Se agotó el tiempo para registrar el dispositivo FCM.")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo registrar el dispositivo para recibir notificaciones.", error)
        }
    }

    companion object {
        private const val TAG = "SISVE-Login"
    }
}