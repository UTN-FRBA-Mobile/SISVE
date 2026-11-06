package com.utn.sisve

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.utn.sisve.location.LocationServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SisveApplication : Application() {

    @Inject lateinit var locationServiceController: LocationServiceController

    override fun onCreate() {
        super.onCreate()
        createTrackingChannel()
        locationServiceController.observe()
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
    }
}
