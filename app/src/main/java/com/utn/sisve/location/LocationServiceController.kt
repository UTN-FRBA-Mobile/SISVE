package com.utn.sisve.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.AmbulanceStatus
import com.utn.sisve.service.LocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Arranca el servicio de GPS al entrar en servicio. La detención la hace el propio servicio.
@Singleton
class LocationServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statusStore: AmbulanceStatusStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun observe() {
        scope.launch {
            statusStore.status.collect { status ->
                if (status == AmbulanceStatus.EN_SERVICIO && hasFineLocation()) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, LocationService::class.java)
                    )
                }
            }
        }
    }

    private fun hasFineLocation() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
