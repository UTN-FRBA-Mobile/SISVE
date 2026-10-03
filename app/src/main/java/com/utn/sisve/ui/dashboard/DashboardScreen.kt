package com.utn.sisve.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utn.sisve.domain.model.AmbulanceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onDispatchReceived: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadAmbulanceData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SISVE", fontWeight = FontWeight.Bold)
                        Text(
                            text = uiState.ambulanceId,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(status = uiState.status, isLoading = uiState.isLoading)

            Text(
                text = "Cambiar estado",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AmbulanceStatus.entries.forEach { status ->
                    StatusButton(
                        status = status,
                        isSelected = uiState.status == status,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateStatus(status) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Botón temporal para probar el flujo de despacho — sacar antes de entregar
            OutlinedButton(
                onClick = { onDispatchReceived("TEST-001") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simular llamado entrante")
            }
        }
    }
}

@Composable
private fun StatusCard(status: AmbulanceStatus, isLoading: Boolean) {
    val (color, containerColor) = when (status) {
        AmbulanceStatus.LIBRE -> Color(0xFF2E7D32) to Color(0xFFC8E6C9)
        AmbulanceStatus.OCUPADO -> Color(0xFFC62828) to Color(0xFFFFCDD2)
        AmbulanceStatus.EN_CAMINO -> Color(0xFFE65100) to Color(0xFFFFE0B2)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = color)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Estado actual", fontSize = 12.sp, color = color.copy(alpha = 0.7f))
                    Text(
                        text = status.displayName.uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusButton(
    status: AmbulanceStatus,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = when (status) {
        AmbulanceStatus.LIBRE -> Color(0xFF2E7D32)
        AmbulanceStatus.OCUPADO -> Color(0xFFC62828)
        AmbulanceStatus.EN_CAMINO -> Color(0xFFE65100)
    }
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) { Text(status.displayName, fontSize = 11.sp) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(status.displayName, fontSize = 11.sp, color = color)
        }
    }
}
