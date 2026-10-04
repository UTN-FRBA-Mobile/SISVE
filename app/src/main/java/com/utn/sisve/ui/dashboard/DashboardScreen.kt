package com.utn.sisve.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.utn.sisve.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utn.sisve.domain.model.AmbulanceStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onDispatchReceived: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operatorName = uiState.operatorName.ifBlank { stringResource(R.string.operator_default_name) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadAmbulanceData() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = operatorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = uiState.ambulanceId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.history_title)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToHistory()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings_title)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider()

                NavigationDrawerItem(
                    label = {
                        Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToLogin()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.open_menu))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AmbulanceHeader(
                    operatorName = operatorName,
                    ambulanceId = uiState.ambulanceId,
                    licensePlate = uiState.licensePlate
                )

                StatusIndicator(
                    status = uiState.status,
                    isLoading = uiState.isLoading
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleStatus() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (uiState.status) {
                                AmbulanceStatus.FUERA_DE_SERVICIO -> Color(0xFF2E7D32)
                                AmbulanceStatus.EN_SERVICIO -> Color(0xFFC62828)
                            }
                        )
                    ) {
                        Text(
                            text = stringResource(
                                when (uiState.status) {
                                    AmbulanceStatus.FUERA_DE_SERVICIO -> R.string.dashboard_enter_service
                                    AmbulanceStatus.EN_SERVICIO -> R.string.dashboard_leave_service
                                }
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.status == AmbulanceStatus.EN_SERVICIO) {
                        // Botón temporal para simular llamado — sacar antes de entregar
                        OutlinedButton(
                            onClick = { onDispatchReceived("TEST-001") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.dashboard_simulate_dispatch))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbulanceHeader(operatorName: String, ambulanceId: String, licensePlate: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = operatorName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.dashboard_unit, ambulanceId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.dashboard_license_plate, licensePlate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusIndicator(status: AmbulanceStatus, isLoading: Boolean) {
    val (circleColor, textColor, label) = when (status) {
        AmbulanceStatus.FUERA_DE_SERVICIO -> Triple(
            Color(0xFFC62828), Color(0xFFC62828), R.string.status_out_of_service
        )
        AmbulanceStatus.EN_SERVICIO -> Triple(
            Color(0xFF2E7D32), Color(0xFF2E7D32), R.string.status_in_service
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(color = circleColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        Text(
            text = stringResource(label),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
