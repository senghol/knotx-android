package com.senghol.knotx.calling.domain.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class CallNetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE
}

data class CallNetworkState(
    val isAvailable: Boolean = false,
    val type: CallNetworkType = CallNetworkType.NONE,
    val isMetered: Boolean = false
)

@Singleton
class CallNetworkManager @Inject constructor(
    context: Context
) {
    private val connectivityManager =
        context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    private val _networkState =
        MutableStateFlow(CallNetworkState())

    val networkState: StateFlow<CallNetworkState> =
        _networkState.asStateFlow()

    private val _networkChanges =
        MutableSharedFlow<CallNetworkState>(
            extraBufferCapacity = 8
        )

    val networkChanges: SharedFlow<CallNetworkState> =
        _networkChanges.asSharedFlow()
    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                updateNetworkState()
            }

            override fun onLost(network: Network) {
                updateNetworkState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                updateNetworkState()
            }
        }

    private var isStarted = false
    fun start() {
        if (isStarted) return
        isStarted = true
        updateNetworkState()

        try {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    .build(),
                networkCallback
            )
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!isStarted) return

        try {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        } catch (_: Exception) {
        }
        isStarted = false
    }

    private fun updateNetworkState() {
        val network = connectivityManager.activeNetwork

        val newState = if (network == null) {
            CallNetworkState(
                isAvailable = false,
                type = CallNetworkType.NONE
            )
        } else {
            val capabilities =
                connectivityManager.getNetworkCapabilities(network)

            if (capabilities == null) {
                CallNetworkState(
                    isAvailable = false,
                    type = CallNetworkType.NONE
                )
            } else {
                val type = when {
                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    ) -> CallNetworkType.WIFI

                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                    ) -> CallNetworkType.CELLULAR

                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_ETHERNET
                    ) -> CallNetworkType.ETHERNET

                    else -> CallNetworkType.OTHER
                }

                CallNetworkState(
                    isAvailable = true,
                    type = type,
                    isMetered =
                        !capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                        )
                )
            }
        }

        _networkState.value = newState
        _networkChanges.tryEmit(newState)
    }
}