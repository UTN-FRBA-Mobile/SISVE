package com.utn.sisve.ui.camera

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.SessionState
import com.utn.sisve.repository.LoginPhotoRepository
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class CameraCaptureError {
    CAPTURE,
    STORAGE,
    CONNECTION,
    UPLOAD
}

data class CameraUiState(
    val isCapturing: Boolean = false,
    val isUploading: Boolean = false,
    val uploadSucceeded: Boolean = false,
    val error: CameraCaptureError? = null
) {
    val isBusy: Boolean
        get() = isCapturing || isUploading
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val photoRepository: LoginPhotoRepository,
    private val sessionManager: SessionManager,
    private val ambulanceStatusStore: AmbulanceStatusStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun capture(imageCapture: ImageCapture) {
        if (_uiState.value.isBusy || _uiState.value.uploadSucceeded) {
            return
        }

        val activeSession = sessionManager.state.value as? SessionState.Active ?: return
        _uiState.update {
            it.copy(isCapturing = true, isUploading = false, error = null)
        }

        viewModelScope.launch {
            try {
                // Cada intento crea otro acceso; si falla el envío, conservar foto y fila local.
                val access = photoRepository.captureAndSave(imageCapture, activeSession.session)

                if (sessionManager.state.value !== activeSession) {
                    return@launch
                }

                _uiState.update { it.copy(isCapturing = false, isUploading = true) }
                photoRepository.upload(access)

                if (sessionManager.state.value !== activeSession) {
                    return@launch
                }

                _uiState.update { it.copy(uploadSucceeded = true) }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(error = CameraCaptureError.CAPTURE) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: ImageCaptureException) {
                val captureError = if (error.imageCaptureError == ImageCapture.ERROR_FILE_IO) {
                    CameraCaptureError.STORAGE
                } else {
                    CameraCaptureError.CAPTURE
                }

                _uiState.update { it.copy(error = captureError) }
            } catch (error: HttpException) {
                if (error.code() == 401 || error.code() == 403) {
                    // El mock pierde sus tokens al reiniciarse: pedir autenticación nuevamente.
                    sessionManager.lockSession(ambulanceStatusStore.status.value)
                } else {
                    _uiState.update { it.copy(error = CameraCaptureError.UPLOAD) }
                }
            } catch (_: LoginPhotoRepository.UploadException) {
                _uiState.update { it.copy(error = CameraCaptureError.CONNECTION) }
            } catch (_: IOException) {
                _uiState.update { it.copy(error = CameraCaptureError.STORAGE) }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = CameraCaptureError.STORAGE) }
            } finally {
                _uiState.update { it.copy(isCapturing = false, isUploading = false) }
            }
        }
    }
}
