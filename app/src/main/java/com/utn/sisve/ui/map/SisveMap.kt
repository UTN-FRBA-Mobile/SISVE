package com.utn.sisve.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.utn.sisve.BuildConfig
import com.utn.sisve.R
import com.utn.sisve.location.GeoCoord
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

/**
 * Único archivo que conoce la librería de mapas (osmdroid / OpenStreetMap).
 * Para cambiar de proveedor (Google Maps, MapBox) alcanza con reescribir este composable.
 *
 * Con [follow] activo la cámara se mantiene centrada en la ambulancia mientras se mueve.
 * Si el usuario arrastra el mapa se llama a [onUserPan] para que quien lo use desactive el seguimiento.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun SisveMap(
    current: GeoCoord?,
    destination: GeoCoord?,
    path: List<GeoCoord>,
    pathFollowsStreets: Boolean,
    currentLabel: String,
    destinationLabel: String,
    follow: Boolean,
    onUserPan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    val camera = remember { CameraState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setOnTouchListener { _, event ->
                    // Solo un arrastre real del usuario corta el seguimiento (no los movimientos programáticos).
                    if (event.actionMasked == MotionEvent.ACTION_MOVE) onUserPan()
                    false
                }
            }
        },
        update = { map ->
            map.overlays.clear()

            val currentPoint = current?.toGeoPoint()
            val destinationPoint = destination?.toGeoPoint()

            if (path.size >= 2) {
                map.overlays.add(route(path.map { it.toGeoPoint() }, dashed = !pathFollowsStreets))
            }
            destinationPoint?.let {
                map.overlays.add(marker(map, it, destinationLabel, R.drawable.ic_marker_emergency, Marker.ANCHOR_BOTTOM))
            }
            currentPoint?.let {
                map.overlays.add(marker(map, it, currentLabel, R.drawable.ic_marker_ambulance, Marker.ANCHOR_CENTER))
            }

            if (follow && currentPoint != null) {
                if (!camera.zoomed) {
                    map.controller.setZoom(17.0)
                    camera.zoomed = true
                }
                if (camera.lastCentered != currentPoint) {
                    map.controller.animateTo(currentPoint)
                    camera.lastCentered = currentPoint
                }
            }
            map.invalidate()
        }
    )
}

private class CameraState {
    var zoomed = false
    var lastCentered: GeoPoint? = null
}

private fun GeoCoord.toGeoPoint() = GeoPoint(latitude, longitude)

private fun marker(map: MapView, point: GeoPoint, title: String, icon: Int, anchorY: Float) =
    Marker(map).apply {
        position = point
        this.title = title
        this.icon = ContextCompat.getDrawable(map.context, icon)
        setAnchor(Marker.ANCHOR_CENTER, anchorY)
    }

// Por calles: línea continua. Sin recorrido disponible: línea recta punteada.
private fun route(points: List<GeoPoint>, dashed: Boolean) = Polyline().apply {
    setPoints(points)
    outlinePaint.color = Color.parseColor("#1565C0")
    outlinePaint.strokeWidth = 14f
    outlinePaint.strokeCap = Paint.Cap.ROUND
    outlinePaint.strokeJoin = Paint.Join.ROUND
    if (dashed) outlinePaint.pathEffect = DashPathEffect(floatArrayOf(30f, 20f), 0f)
}

// Estilo claro con calles y nombres, sin íconos de comercios ni puntos de interés.
// Datos de OpenStreetMap; los tiles los sirve CARTO y exigen key (atribución obligatoria, ver MapScreen).
// Sin key configurada se usa el estilo estándar de OSM para que el mapa nunca quede inutilizable.
private val MAP_TILES = if (BuildConfig.CARTO_API_KEY.isBlank()) {
    TileSourceFactory.MAPNIK
} else {
    object : XYTileSource(
        // El nombre identifica el caché en disco: cambiarlo descarta tiles viejos (p. ej. los de "API KEY REQUIRED").
        "CartoLightKeyed", 0, 20, 256, ".png",
        arrayOf("https://basemaps.cartocdn.com/rastertiles/light_all/"),
        "© OpenStreetMap contributors © CARTO"
    ) {
        // La key va solo en la URL; no se guarda en el nombre de los archivos del caché.
        override fun getTileURLString(pMapTileIndex: Long): String =
            "https://basemaps.cartocdn.com/rastertiles/light_all/" +
                "${MapTileIndex.getZoom(pMapTileIndex)}/${MapTileIndex.getX(pMapTileIndex)}/" +
                "${MapTileIndex.getY(pMapTileIndex)}.png?key=${BuildConfig.CARTO_API_KEY}"
    }
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        // OSM exige identificar la app; guardamos el caché en el almacenamiento privado.
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(MAP_TILES)
        setMultiTouchControls(true)
        // Tiles escalados a la densidad de pantalla: calles y textos más legibles.
        isTilesScaledToDpi = true
        minZoomLevel = 3.0
        maxZoomLevel = 19.0
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        controller.setZoom(15.0)
    }
}
