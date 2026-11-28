package com.utn.sisve

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.utn.sisve.location.LocationServiceController
import com.utn.sisve.repository.ApiRepository
import com.utn.sisve.session.AppForegroundTracker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch


@Singleton
class TokenPreferences @Inject constructor(
    private val prefs: SharedPreferences
) {

    fun saveFcmToken(tokenFcm: String) {
        prefs.edit()
            .putString(KEY_TOKEN_FCM, tokenFcm.trim())
            .apply()
    }

    fun getFcmToken(): String = prefs.getString(KEY_TOKEN_FCM, "") ?: ""

    companion object {
        private const val KEY_TOKEN_FCM = "token_fcm"
    }
}




@HiltAndroidApp
class SisveApplication : Application() {

    @Inject lateinit var locationServiceController: LocationServiceController
    @Inject lateinit var appForegroundTracker: AppForegroundTracker
    @Inject lateinit var apiRepository: ApiRepository
    @Inject lateinit var tokenPreferences: TokenPreferences
    @Inject lateinit var ambulancePreferences: com.utn.sisve.data.local.AmbulancePreferences

    override fun onCreate() {
        super.onCreate()
        createTrackingChannel()
        locationServiceController.observe()
        CoroutineScope(Dispatchers.IO).launch {
            registerFcmToken()
        }
    }


    private fun createTrackingChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TRACKING,
                getString(R.string.notification_channel_tracking),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_TRACKING = "tracking"
        const val CHANNEL_DISPATCHES = "dispatches"
        private const val TAG = "SisveApp"


    }


    private suspend fun registerFcmToken() {
        try {
            var token = tokenPreferences.getFcmToken()

            if (token.isBlank()) {
                val fetchedToken = withTimeoutOrNull(10_000) {
                    suspendCancellableCoroutine { continuation ->
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (!continuation.isActive) return@addOnCompleteListener
                            if (task.isSuccessful && !task.result.isNullOrBlank()) {
                                continuation.resume(task.result)
                            } else {
                                continuation.resumeWithException(
                                    task.exception ?: IOException("Firebase no devolvió un token FCM.")
                                )
                            }
                        }
                    }
                }
                if (!fetchedToken.isNullOrBlank()) {
                    token = fetchedToken
                    tokenPreferences.saveFcmToken(token)
                }
            }

            if (token.isNotBlank()) {
                apiRepository.registerDeviceToken(token)
                Log.d(TAG, "Dispositivo FCM registrado en el servidor.")

                val ambulanceId = ambulancePreferences.getAmbulanceId()
                if (ambulanceId.isNotBlank()) {
                    try {
                        apiRepository.linkAmbulanceToken(ambulanceId, token)
                        Log.d(TAG, "Ambulancia $ambulanceId vinculada al token FCM en el servidor.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al vincular ambulancia con token FCM", e)
                    }
                }
            } else {
                Log.e(TAG, "No se pudo obtener el token FCM.")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo registrar el dispositivo para recibir notificaciones.", error)
        }
    }


}
