package com.utn.sisve.session

import com.utn.sisve.domain.model.AmbulanceStatus
import com.utn.sisve.domain.model.SessionData
import com.utn.sisve.domain.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class SessionManager @Inject constructor(
    private val clock: SessionClock
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    // Provisional: solo en memoria hasta incorporar el almacenamiento protegido.
    // Nunca exponer el token dentro del estado que observa la interfaz.
    @Volatile
    private var token: String? = null

    private var lastInteractionMillis: Long? = null

    // Llamar únicamente después de un login exitoso contra el servidor.
    @Synchronized
    fun startSession(
        token: String,
        ambulanceId: String,
        operatorId: String,
        operatorName: String
    ) {
        require(token.isNotBlank()) { "El token no puede estar vacío." }
        require(ambulanceId.isNotBlank()) { "El ID de ambulancia no puede estar vacío." }
        require(operatorId.isNotBlank()) { "El ID de operador no puede estar vacío." }
        require(operatorName.isNotBlank()) { "El nombre de operador no puede estar vacío." }

        this.token = token
        lastInteractionMillis = clock.nowMillis()

        _state.value = SessionState.Active(
            session = SessionData(
                ambulanceId = ambulanceId,
                operatorId = operatorId,
                operatorName = operatorName
            )
        )
    }

    // También puede existir un token con la interfaz bloqueada para mantener el GPS.
    fun getToken(): String? = token

    @Synchronized
    fun lockSession(ambulanceStatus: AmbulanceStatus) {
        val session = when (val current = _state.value) {
            is SessionState.Active -> current.session
            is SessionState.Locked -> current.session
            SessionState.Unauthenticated -> null
        }

        lastInteractionMillis = null

        if (ambulanceStatus == AmbulanceStatus.FUERA_DE_SERVICIO || session == null) {
            token = null
            _state.value = SessionState.Unauthenticated
        } else {
            _state.value = SessionState.Locked(session)
        }
    }

    // El flujo de cierre debe poner la ambulancia fuera de servicio antes de llamar esto.
    @Synchronized
    fun logout() {
        lockSession(AmbulanceStatus.FUERA_DE_SERVICIO)
    }

    // Llamar al volver a primer plano y periódicamente mientras la app esté visible.
    // Devuelve true solo cuando esta comprobación acaba de bloquear la sesión.
    @Synchronized
    fun checkExpiration(ambulanceStatus: AmbulanceStatus): Boolean {
        val lastInteraction = lastInteractionMillis ?: return false

        if (_state.value !is SessionState.Active) {
            return false
        }

        val elapsed = clock.nowMillis() - lastInteraction

        if (elapsed < INACTIVITY_TIMEOUT_MILLIS) {
            return false
        }

        lockSession(ambulanceStatus)

        return true
    }

    // Solo interacción real del usuario; no notificaciones ni actualizaciones GPS.
    // Comprobar primero evita que el primer toque reactive una sesión ya vencida.
    @Synchronized
    fun recordInteraction(ambulanceStatus: AmbulanceStatus): Boolean {
        if (checkExpiration(ambulanceStatus) || _state.value !is SessionState.Active) {
            return false
        }

        lastInteractionMillis = clock.nowMillis()

        return true
    }

    companion object {
        private val INACTIVITY_TIMEOUT_MILLIS = 8.hours.inWholeMilliseconds
    }
}
