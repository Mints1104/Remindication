package com.mints.mobilehealthapplication.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class InternetConnectionChecker(private val context: Context) {

    private val TAG = "InternetConnectionChecker"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _connectionState = MutableLiveData<Boolean>()
    val connectionState: LiveData<Boolean> = _connectionState

    var isConnected: Boolean = false
        private set

    init {
        checkInternetConnection()
        registerNetworkCallback()
    }


    fun checkInternetConnection(): Boolean {
        val network: Network? = connectivityManager.activeNetwork
        if (network == null) {
            Log.d(TAG, "No active network detected")
            updateConnectionState(false)
            return false
        }

        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        if (networkCapabilities == null) {
            Log.d(TAG, "No network capabilities detected")
            updateConnectionState(false)
            return false
        }

        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        updateConnectionState(hasInternet)

        if (hasInternet) {
            Log.d(TAG, "Internet connection available")

            val connectionType = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Unknown"
            }

            Log.d(TAG, "Connection type: $connectionType")
        } else {
            Log.d(TAG, "No internet connection")
        }

        return hasInternet
    }


    private fun updateConnectionState(connected: Boolean) {
        isConnected = connected
        _connectionState.postValue(connected)
    }


    fun registerNetworkCallback() {
        if (networkCallback != null) return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network available")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                updateConnectionState(false)
                Log.d(TAG, "Network lost")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                updateConnectionState(hasInternet)
                Log.d(TAG, "Network capabilities changed, internet available: $hasInternet")
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }


    fun unregisterNetworkCallback() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
}