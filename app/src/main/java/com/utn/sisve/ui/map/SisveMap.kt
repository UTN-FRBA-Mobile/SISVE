package com.utn.sisve.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.graphics.DashPathEffect
import com.utn.sisve.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.utn.sisve.location.GeoCoord
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
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
 * [recenterTick]: cada vez que cambia, la cámara vuelve a centrarse.
 */
@Composable
fun SisveMap(
    current: GeoCoord?,
    destination: GeoCoord?,
    currentLabel: String,
    destinationLabel: String,
    recenterTick: Int,
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
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            val currentPoint = current?.toGeoPoint()
            val destinationPoint = destination?.toGeoPoint()

            if (currentPoint != null && destinationPoint != null) {
                map.overlays.add(route(currentPoint, destinationPoint))
            }
            destinationPoint?.let {
                map.overlays.add(marker(map, it, destinationLabel, R.drawable.ic_marker_emergency, Marker.ANCHOR_BOTTOM))
            }
            currentPoint?.let {
                map.overlays.add(marker(map, it, currentLabel, R.drawable.ic_marker_ambulance, Marker.ANCHOR_CENTER))
            }

            val needsFit = destinationPoint != null && currentPoint != null && !camera.fitted
            when {
                needsFit -> {
                    map.post {
                        map.zoomToBoundingBox(
                            BoundingBox.fromGeoPoints(listOf(currentPoint, destinationPoint)),
                            true,
                            120
                        )
                    }
                    camera.fitted = true
                    camera.lastTick = recenterTick
                }
                currentPoint != null && (!camera.centered || camera.lastTick != recenterTick) -> {
                    map.controller.setZoom(16.0)
                    map.controller.animateTo(currentPoint)
                    camera.centered = true
                    camera.lastTick = recenterTick
                }
            }
            map.invalidate()
        }
    )
}

private class CameraState {
    var centered = false
    var fitted = false
    var lastTick = 0
}

private fun GeoCoord.toGeoPoint() = GeoPoint(latitude, longitude)

private fun marker(map: MapView, point: GeoPoint, title: String, icon: Int, anchorY: Float) =
    Marker(map).apply {
        position = point
        this.title = title
        this.icon = ContextCompat.getDrawable(map.context, icon)
        setAnchor(Marker.ANCHOR_CENTER, anchorY)
    }

private fun route(from: GeoPoint, to: GeoPoint) = Polyline().apply {
    setPoints(listOf(from, to))
    outlinePaint.color = Color.parseColor("#1565C0")
    outlinePaint.strokeWidth = 10f
    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(30f, 20f), 0f)
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        // OSM exige identificar la app; guardamos el caché en el almacenamiento privado.
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        // Tiles escalados a la densidad de pantalla: calles y textos más legibles.
        isTilesScaledToDpi = true
        minZoomLevel = 3.0
        maxZoomLevel = 19.0
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        controller.setZoom(15.0)
    }
}
