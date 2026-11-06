package com.utn.sisve.domain.model

sealed interface SessionState {
    data object Unauthenticated : SessionState

    data class Locked(
        val session: SessionData
    ) : SessionState

    data class Active(
        val session: SessionData
    ) : SessionState
}
