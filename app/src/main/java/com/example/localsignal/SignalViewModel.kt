package com.example.localsignal

import android.util.Log
import android.util.Log.e
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset

@HiltViewModel
class SignalViewModel @Inject constructor(
    private val connectionsClient: ConnectionsClient
) : ViewModel() {

    private val tag = "NearbyConnections"
    private val serviceId = "com.example.localsignal.test"
    private val strategy = Strategy.P2P_CLUSTER

    private val _message = MutableStateFlow("Waiting for a signal...")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _connectedDeviceCount = MutableStateFlow(0)
    val connectedDeviceCount: StateFlow<Int> = _connectedDeviceCount.asStateFlow()

    private val connectedEndpoints = mutableSetOf<String>()

    //Add view model functions here

    private val payloadCallback = object: PayloadCallback() {
        override fun onPayloadReceived(
            p0: String,
            p1: Payload
        ) {
            if(p1.type == Payload.Type.BYTES) {
                val receivedString = String(p1.asBytes()!!, Charsets.UTF_8)
                _message.value = "Message: $receivedString"
            }
        }

        override fun onPayloadTransferUpdate(
            p0: String,
            p1: PayloadTransferUpdate
        ) {
            TODO("Not yet implemented")
        }

    }

    fun startNetworking() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startAdvertising("MyDevice", serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnFailureListener { e ->
                _message.value = "Faied to start advertising: ${e.message}"
                Log.e(tag, "Advertising Failed", e)
            }
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnFailureListener { e ->
                _message.value = "Failed to start discovery: ${e.message}"
                Log.e(tag, "Discovery failed", e)
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(
            p0: String,
            p1: ConnectionInfo
        ) {
            connectionsClient.acceptConnection(p0, payloadCallback)
        }

        override fun onConnectionResult(
            p0: String,
            p1: ConnectionResolution
        ) {
            if (p1.status.statusCode == ConnectionsStatusCodes.STATUS_OK)
            {
                connectedEndpoints.add(p0)
                _connectedDeviceCount.value = connectedEndpoints.size
            }
        }

        override fun onDisconnected(p0: String) {
            connectedEndpoints.remove(p0)
            _connectedDeviceCount.value = connectedEndpoints.size
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(
            p0: String,
            p1: DiscoveredEndpointInfo
        ) {
            connectionsClient.requestConnection("MyDevice", p0, connectionLifecycleCallback)
        }

        override fun onEndpointLost(p0: String) {  }
    }

    fun sendSignal(textInput: String) {
        if(connectedEndpoints.isEmpty())
        {
            _message.value = "No devices connected yet"
            return
        }

        val payload = Payload.fromBytes(textInput.toByteArray(Charsets.UTF_8))

        connectionsClient.sendPayload(connectedEndpoints.toList(), payload)
        _message.value = "Signal Sent"
    }

    override fun onCleared() {
        super.onCleared()
        connectionsClient.stopAllEndpoints()
    }
}

















