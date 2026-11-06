package com.utn.sisve.location

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.hypot

// Recorrido por calles entre dos puntos. Interfaz propia para poder cambiar de servicio.
interface RouteProvider {
    // Devuelve los puntos del recorrido, o null si no se pudo calcular.
    suspend fun route(from: GeoCoord, to: GeoCoord): List<GeoCoord>?
}

// Servidor público de demostración de OSRM (OpenStreetMap). No requiere key.
class OsrmRouteProvider @Inject constructor() : RouteProvider {

    // Cliente propio y simple: no debe llevar el token de la ambulancia ni el logging de la API de SISVE.
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun route(from: GeoCoord, to: GeoCoord): List<GeoCoord>? = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
            "?overview=full&geometries=geojson"
        try {
            val request = Request.Builder().url(url).header("User-Agent", "SISVE-student-app").build()
            client.newCall(request).execute().use { response ->
                parse(if (response.isSuccessful) response.body?.string() else null)
            }
        } catch (e: Exception) {
            // Sin red o servicio caído: el mapa muestra una línea recta como alternativa.
            null
        }
    }

    private fun parse(body: String?): List<GeoCoord>? {
        if (body == null) return null
        val json = JsonParser.parseString(body).asJsonObject
        if (json["code"]?.asString != "Ok") return null
        return json.getAsJsonArray("routes")
            .firstOrNull()?.asJsonObject
            ?.getAsJsonObject("geometry")
            ?.getAsJsonArray("coordinates")
            ?.map { point ->
                val lonLat = point.asJsonArray
                GeoCoord(latitude = lonLat[1].asDouble, longitude = lonLat[0].asDouble)
            }
            ?.takeIf { it.size >= 2 }
    }
}

// Cálculos sobre el recorrido, con una aproximación plana válida para distancias urbanas.
object RouteMath {

    data class Snap(val segmentIndex: Int, val point: GeoCoord, val distanceMeters: Double)

    // Punto del recorrido más cercano a [position] (proyección sobre cada tramo).
    fun snap(position: GeoCoord, route: List<GeoCoord>): Snap? {
        if (route.size < 2) return null
        val metersPerDegLon = METERS_PER_DEG_LAT * cos(Math.toRadians(position.latitude))
        var best: Snap? = null
        for (i in 0 until route.size - 1) {
            val ax = (route[i].longitude - position.longitude) * metersPerDegLon
            val ay = (route[i].latitude - position.latitude) * METERS_PER_DEG_LAT
            val bx = (route[i + 1].longitude - position.longitude) * metersPerDegLon
            val by = (route[i + 1].latitude - position.latitude) * METERS_PER_DEG_LAT
            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared == 0.0) 0.0 else (-(ax * dx + ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val distance = hypot(ax + dx * t, ay + dy * t)
            if (best == null || distance < best.distanceMeters) {
                val point = GeoCoord(
                    latitude = route[i].latitude + (route[i + 1].latitude - route[i].latitude) * t,
                    longitude = route[i].longitude + (route[i + 1].longitude - route[i].longitude) * t
                )
                best = Snap(i, point, distance)
            }
        }
        return best
    }

    fun length(points: List<GeoCoord>): Double =
        points.zipWithNext().sumOf { (a, b) ->
            val metersPerDegLon = METERS_PER_DEG_LAT * cos(Math.toRadians((a.latitude + b.latitude) / 2))
            hypot(
                (b.longitude - a.longitude) * metersPerDegLon,
                (b.latitude - a.latitude) * METERS_PER_DEG_LAT
            )
        }

    private const val METERS_PER_DEG_LAT = 110_540.0
}
