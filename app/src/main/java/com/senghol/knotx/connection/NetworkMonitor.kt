package com.senghol.knotx.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.senghol.knotx.utils.objects.DLog

import android.net.NetworkRequest


class NetworkMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            val caps = cm.getNetworkCapabilities(network)
            val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (hasInternet && validated) {
                DLog.i("🌐 Network available (validated) → ensureConnected")
                SocketManager.ensureConnected()
            } else {
                DLog.i("🌐 Network available but NOT yet validated — waiting")
            }
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            // Catches the moment validation completes on existing networks
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            if (hasInternet && validated) {
                DLog.i("🌐 Network validated → ensureConnected")
                SocketManager.ensureConnected()
            }
        }

        override fun onLost(network: Network) {
            DLog.i("🌐 Network lost — socket will self-reconnect when network returns")
            // ✅ Do NOT disconnect manually; OkHttp will fire onFailure,
            //    which triggers scheduleReconnect() automatically.
        }
    }

    fun register() {
        try {
            // registerDefaultNetworkCallback misses onCapabilitiesChanged on some OEMs;
            // using a NetworkRequest is more reliable.
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            DLog.e("🌐 Failed to register network callback", e)
        }
    }

    fun unregister() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {}
    }
}