package com.utn.sisve.location

import android.content.Context
import android.location.Geocoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

// Dirección -> coordenadas. Interfaz propia para poder cambiar de proveedor.
interface AddressGeocoder {
    suspend fun locate(address: String): GeoCoord?
}

class AndroidAddressGeocoder @Inject constructor(
    @ApplicationContext private val context: Context
) : AddressGeocoder {

    @Suppress("DEPRECATION")
    override suspend fun locate(address: String): GeoCoord? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            Geocoder(context, Locale("es", "AR")).getFromLocationName(address, 1)
                ?.firstOrNull()
                ?.let { GeoCoord(it.latitude, it.longitude) }
        } catch (e: Exception) {
            // Sin red o dirección no resoluble.
            null
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    abstract fun bindGeocoder(impl: AndroidAddressGeocoder): AddressGeocoder
}
