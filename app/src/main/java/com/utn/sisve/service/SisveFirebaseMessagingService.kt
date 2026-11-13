package com.utn.sisve.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.utn.sisve.MainActivity
import com.utn.sisve.R
import com.utn.sisve.SisveApplication
import com.utn.sisve.data.local.AmbulanceStatusStore
import com.utn.sisve.domain.model.AmbulanceStatus
import com.utn.sisve.domain.model.IncomingDispatchStore
import com.utn.sisve.location.ActiveDispatchStore
import com.utn.sisve.repository.ApiRepository
import com.utn.sisve.session.AppForegroundTracker
import com.utn.sisve.session.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SisveFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var apiRepository: ApiRepository
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var incomingDispatchStore: IncomingDispatchStore
    @Inject lateinit var ambulanceStatusStore: AmbulanceStatusStore
    @Inject lateinit var appForegroundTracker: AppForegroundTracker
    @Inject lateinit var activeDispatchStore: ActiveDispatchStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Se recibió un token FCM nuevo.")

        if (sessionManager.getToken() == null) {
            return
        }

        scope.launch {
            try {
                apiRepository.registerFcmToken(token)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo actualizar el token FCM en el servidor.", error)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje FCM recibido de: ${remoteMessage.from}")

        if (ambulanceStatusStore.status.value != AmbulanceStatus.EN_SERVICIO) {
            Log.d(TAG, "Mensaje de emergencia omitido: la ambulancia está fuera de servicio.")
            return
        }

        if (activeDispatchStore.dispatch.value != null) {
            Log.d(TAG, "Mensaje de emergencia omitido: la ambulancia ya tiene una emergencia activa en curso.")
            return
        }

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.dispatch_title)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["description"]
            ?: getString(R.string.dispatch_sample_description)

        val callId = remoteMessage.data["dispatchId"]
            ?: remoteMessage.data["callId"]
            ?: return

        incomingDispatchStore.postDispatch(callId)

        if (!appForegroundTracker.isForeground) {
            showNotification(title, body, callId)
        } else {
            Log.d(TAG, "App en primer plano: se omitió la notificación emergente.")
        }
    }

    private fun showNotification(title: String, body: String, callId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("dispatchId", callId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        ensureChannelExists()

        val notification = NotificationCompat.Builder(this, SisveApplication.CHANNEL_DISPATCHES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(callId.hashCode(), notification)
    }

    private fun ensureChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(SisveApplication.CHANNEL_DISPATCHES) == null) {
                val channel = NotificationChannel(
                    SisveApplication.CHANNEL_DISPATCHES,
                    "Solicitudes de Emergencia",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de emergencia entrantes"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val TAG = "SisveFCM"
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
