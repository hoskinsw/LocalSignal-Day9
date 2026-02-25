package com.example.localsignal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SignalScreen()
                }
            }
        }
    }
}

@Composable
fun SignalScreen(viewModel: SignalViewModel = hiltViewModel()) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val connectedCount by viewModel.connectedDeviceCount.collectAsStateWithLifecycle()

    var permissionsGranted by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val requiredPermissions = remember {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissions.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val essentialPermissionsGranted = results.entries.all {
            it.value || it.key == Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (essentialPermissionsGranted) {
            permissionsGranted = true
            viewModel.startNetworking()
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(requiredPermissions)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!permissionsGranted) {
            Text("Permissions required for local networking.")
        } else {
            Text(
                text = "Devices Connected: $connectedCount",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = textInput,
                onValueChange = { textInput = it },
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.sendSignal(textInput) },
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text("Send Signal", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}