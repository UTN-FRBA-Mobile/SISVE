package com.utn.sisve.domain.model

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class IncomingDispatchStore @Inject constructor() {

    private val _incomingDispatch = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val incomingDispatch: SharedFlow<String> = _incomingDispatch.asSharedFlow()

    fun postDispatch(dispatchId: String) {
        _incomingDispatch.tryEmit(dispatchId)
    }
}
