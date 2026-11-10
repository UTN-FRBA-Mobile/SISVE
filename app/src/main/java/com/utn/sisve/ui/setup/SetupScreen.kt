package com.utn.sisve.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.utn.sisve.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showError by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isAlreadyConfigured) {
        if (uiState.isAlreadyConfigured) onSetupComplete()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            OutlinedTextField(
                value = uiState.ambulanceId,
                onValueChange = {
                    showError = false
                    viewModel.onAmbulanceIdChange(it)
                },
                label = { Text(stringResource(R.string.setup_ambulance_id)) },
                placeholder = { Text(stringResource(R.string.setup_ambulance_id_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                isError = showError && uiState.ambulanceId.isBlank()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.licensePlate,
                onValueChange = {
                    showError = false
                    viewModel.onLicensePlateChange(it)
                },
                label = { Text(stringResource(R.string.setup_license_plate)) },
                placeholder = { Text(stringResource(R.string.setup_license_plate_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                isError = showError && uiState.licensePlate.isBlank()
            )

            if (showError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setup_required_fields),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            uiState.qrError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (viewModel.saveAndContinue()) onSetupComplete()
                    else showError = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.setup_continue), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showQrSheet = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.setup_scan_qr))
            }
        }
    }

    if (showQrSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQrSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Escanear QR de Unidad (Simulado)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Seleccioná un código QR para simular la lectura de la etiqueta en el vehículo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QrPresetCard(
                    title = "Móvil 01 (CABA Central)",
                    details = "AMB-001 • AB 123 CD • Servidor Local",
                    onClick = {
                        val json = """
                            {
                              "ambulanceId": "AMB-001",
                              "licensePlate": "AB 123 CD",
                              "serverUrl": "http://127.0.0.1:8585/api/v1/",
                              "gpsIntervalSeconds": 10
                            }
                        """.trimIndent()
                        viewModel.applyQrCode(json)
                        showQrSheet = false
                    }
                )

                QrPresetCard(
                    title = "Móvil 05 (Unidad de Terapia Intensiva)",
                    details = "AMB-005 • AE 845 ZX • Intervalo GPS: 5s",
                    onClick = {
                        val json = """
                            {
                              "ambulanceId": "AMB-005",
                              "licensePlate": "AE 845 ZX",
                              "serverUrl": "http://127.0.0.1:8585/api/v1/",
                              "gpsIntervalSeconds": 5
                            }
                        """.trimIndent()
                        viewModel.applyQrCode(json)
                        showQrSheet = false
                    }
                )

                QrPresetCard(
                    title = "QR Inválido o Dañado (Prueba de error)",
                    details = "Formato corrupto para validar excepciones",
                    isErrorCase = true,
                    onClick = {
                        viewModel.applyQrCode("{ \"corrupted\": true }")
                        showQrSheet = false
                    }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QrPresetCard(
    title: String,
    details: String,
    isErrorCase: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isErrorCase) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = if (isErrorCase) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isErrorCase) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}