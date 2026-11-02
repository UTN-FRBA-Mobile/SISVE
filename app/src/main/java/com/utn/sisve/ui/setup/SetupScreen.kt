package com.utn.sisve.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
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

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showError by remember { mutableStateOf(false) }

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
                onClick = { /* TODO Miembro 4: implementar escaneo de QR */ },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.setup_scan_qr))
            }
        }
    }
}
