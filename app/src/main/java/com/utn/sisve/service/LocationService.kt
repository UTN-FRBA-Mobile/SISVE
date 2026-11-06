package com.utn.sisve.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.utn.sisve.MainActivity
import com.utn.sisve.R
import com.utn.sisve.SisveApplication
import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.AmbulanceStatus
import com.utn.sisve.location.LocationStore
import com.utn.sisve.location.LocationTracker
import com.utn.sisve.repository.ApiRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var tracker: LocationTracker
    @Inject lateinit var store: LocationStore
    @Inject lateinit var statusStore: AmbulanceStatusStore
    @Inject lateinit var preferences: AmbulancePreferences
    @Inject lateinit var repository: ApiRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android exige startForeground poco después de startForegroundService.
        startInForeground(buildNotification(gpsEnabled = true))

        if (intent?.action == ACTION_LEAVE_SERVICE) {
            // Botón de la notificación: cambia el estado sin abrir la app.
            statusStore.setStatus(AmbulanceStatus.FUERA_DE_SERVICIO)
            // TODO Miembro 6: notificar al servidor el cambio de estado.
        }

        if (statusStore.status.value != AmbulanceStatus.EN_SERVICIO) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!running) {
            running = true
            tracker.start(preferences.getGpsIntervalSeconds().coerceAtLeast(1L) * 1000L)
            observeStatus()
            observeGpsState()
            sendFixes()
        }
        return START_NOT_STICKY
    }

    private fun observeStatus() = scope.launch {
        statusStore.status.collect { status ->
            if (status != AmbulanceStatus.EN_SERVICIO) stopSelf()
        }
    }

    private fun observeGpsState() = scope.launch {
        store.gpsEnabled.collect { enabled ->
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(enabled))
        }
    }

    private fun sendFixes() = scope.launch {
        val sendIntervalMillis = preferences.getGpsIntervalSeconds().coerceAtLeast(1L) * 1000L
        var lastSentAt = 0L
        store.lastFix.filterNotNull().collect { fix ->
            // Con el mapa abierto el GPS lee cada segundo, pero al servidor se envía según la configuración.
            if (fix.timestamp - lastSentAt < sendIntervalMillis) return@collect
            lastSentAt = fix.timestamp
            // sendLocation guarda en Room si no hay red; luego se reintentan los pendientes.
            repository.sendLocation(fix.coord.latitude, fix.coord.longitude)
            repository.sendPendingLocations()
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(gpsEnabled: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val leave = PendingIntent.getService(
            this, 1,
            Intent(this, LocationService::class.java).setAction(ACTION_LEAVE_SERVICE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, SisveApplication.CHANNEL_TRACKING)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText(
                getString(
                    if (gpsEnabled) R.string.notification_tracking_text
                    else R.string.notification_gps_off
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.notification_action_leave), leave)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        tracker.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_LEAVE_SERVICE = "com.utn.sisve.action.LEAVE_SERVICE"
        private const val NOTIFICATION_ID = 1001
    }
}
