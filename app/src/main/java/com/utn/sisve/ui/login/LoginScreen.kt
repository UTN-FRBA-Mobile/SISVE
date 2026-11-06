package com.utn.sisve.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utn.sisve.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loginSucceeded) {
        if (uiState.loginSucceeded) {
            onLoginSuccess()
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = stringResource(R.string.dashboard_unit, uiState.ambulanceId),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(R.string.login_pin_hint),
                    style = MaterialTheme.typography.bodyMedium
                )

                PinIndicator(length = uiState.pin.length)

                uiState.error?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                LoginError.INVALID_PIN -> R.string.login_error_invalid_pin
                                LoginError.INVALID_CREDENTIALS -> R.string.login_error_credentials
                                LoginError.CONNECTION -> R.string.login_error_connection
                                LoginError.SERVER -> R.string.login_error_server
                                LoginError.INVALID_RESPONSE -> R.string.login_error_response
                            }
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                PinKeypad(
                    enabled = !uiState.isLoading && !uiState.loginSucceeded,
                    onDigit = viewModel::appendDigit,
                    onDelete = viewModel::deleteLastDigit,
                    onClear = viewModel::clearPin
                )

                Button(
                    onClick = viewModel::login,
                    enabled = uiState.canSubmit,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = stringResource(
                            if (uiState.isLoading) R.string.login_loading else R.string.login_submit
                        ),
                        fontSize = 18.sp
                    )
                }

            }
        }
    }
}

@Composable
private fun PinIndicator(length: Int) {
    val description = stringResource(R.string.login_pin_length, length)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(length) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (digits in (1..9).chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (digit in digits) {
                    OutlinedButton(
                        onClick = { onDigit(digit) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Text(digit.toString(), fontSize = 28.sp)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onClear,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text(stringResource(R.string.login_clear_pin))
            }

            OutlinedButton(
                onClick = { onDigit(0) },
                enabled = enabled,
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text(0.toString(), fontSize = 28.sp)
            }

            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.login_delete_digit)
                )
            }
        }
    }
}
