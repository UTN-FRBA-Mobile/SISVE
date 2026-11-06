package com.utn.sisve.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utn.sisve.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    onArrived: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val warning = when {
                !state.gpsEnabled -> stringResource(R.string.map_gps_off)
                state.destinationNotFound -> stringResource(R.string.map_destination_not_found)
                else -> null
            }
            warning?.let {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                SisveMap(
                    current = state.current,
                    destination = state.destination,
                    path = state.path,
                    pathFollowsStreets = state.pathFollowsStreets,
                    bearing = state.bearing,
                    currentLabel = stringResource(R.string.map_current_position),
                    destinationLabel = state.destinationAddress ?: stringResource(R.string.map_destination),
                    follow = state.follow,
                    onUserPan = viewModel::onUserPan,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.current == null) {
                        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
                            Text(
                                stringResource(R.string.map_waiting_location),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    state.routeStatus?.let { RouteStatusChip(it) }
                }
                // Atribución exigida por las licencias de OpenStreetMap y CARTO.
                Text(
                    text = stringResource(R.string.map_attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                )
                // Resaltado mientras el mapa sigue a la ambulancia; al arrastrar el mapa se apaga.
                SmallFloatingActionButton(
                    onClick = viewModel::recenter,
                    containerColor = if (state.follow) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (state.follow) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.map_recenter))
                }
            }

            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.destinationAddress?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                    state.distanceMeters?.let {
                        Text(
                            stringResource(R.string.map_distance, formatDistance(it)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    // Botón temporal para probar el flujo post-arribo
                    OutlinedButton(onClick = onArrived, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.map_simulate_arrival))
                    }
                }
            }
        }
    }
}

private fun formatDistance(meters: Float): String =
    if (meters >= 1000f) String.format(java.util.Locale.getDefault(), "%.1f km", meters / 1000f)
    else "${meters.toInt()} m"

// Cartel chico que indica si la ambulancia sigue la ruta o si se está calculando una nueva.
@Composable
private fun RouteStatusChip(status: RouteStatus) {
    val (label, container, content) = when (status) {
        RouteStatus.FOLLOWING -> Triple(
            R.string.map_route_following,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        RouteStatus.CALCULATING -> Triple(
            R.string.map_route_calculating,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        RouteStatus.RECALCULATING -> Triple(
            R.string.map_route_recalculating,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        RouteStatus.UNAVAILABLE -> Triple(
            R.string.map_route_unavailable,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (status == RouteStatus.CALCULATING || status == RouteStatus.RECALCULATING) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = content)
            }
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}
