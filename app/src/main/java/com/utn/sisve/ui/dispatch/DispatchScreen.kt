package com.utn.sisve.ui.dispatch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.utn.sisve.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    callId: String,
    onAccepted: () -> Unit,
    onRejected: () -> Unit,
    viewModel: DispatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(callId) { viewModel.loadCall(callId) }
    LaunchedEffect(uiState.isAccepted) { if (uiState.isAccepted) onAccepted() }
    LaunchedEffect(uiState.isRejected) { if (uiState.isRejected) onRejected() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dispatch_title)) },
                navigationIcon = {
                    IconButton(onClick = onRejected) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFC62828),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val call = uiState.call

        if (call == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(uiState.error ?: stringResource(R.string.dispatch_load_error))
                TextButton(onClick = onRejected) {
                    Text(stringResource(R.string.navigate_back))
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DetailRow(label = stringResource(R.string.dispatch_type), value = call.emergencyType)
                    HorizontalDivider()
                    DetailRow(label = stringResource(R.string.dispatch_location), value = call.address)
                    HorizontalDivider()
                    DetailRow(label = stringResource(R.string.dispatch_description), value = call.description)
                    HorizontalDivider()
                    DetailRow(label = stringResource(R.string.dispatch_priority), value = call.priority)
                }
            }

            Spacer(Modifier.height(16.dp))

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.rejectDispatch() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = !uiState.isResponding,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (uiState.isResponding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.dispatch_reject), fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.acceptDispatch() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = !uiState.isResponding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    if (uiState.isResponding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.dispatch_accept), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
