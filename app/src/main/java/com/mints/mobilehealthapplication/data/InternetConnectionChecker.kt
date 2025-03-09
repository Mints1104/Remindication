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

    // LiveData for observing connection state changes
    private val _connectionState = MutableLiveData<Boolean>()
    val connectionState: LiveData<Boolean> = _connectionState

    // Variable to store the connection status (still useful for immediate checks)
    var isConnected: Boolean = false
        private set

    init {
        // Initialize connection state
        checkInternetConnection()
        // Register network callback in initialization
        registerNetworkCallback()
    }

    /**
     * Checks if the device has an active internet connection
     * @return Boolean indicating if internet is available
     */
    fun checkInternetConnection(): Boolean {
        // For Android 6.0+ (API 23+)
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

        // Check if the device has internet capability
        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        updateConnectionState(hasInternet)

        if (hasInternet) {
            Log.d(TAG, "Internet connection available")

            // Determine connection type for more detailed logging
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

    /**
     * Updates the connection state and LiveData
     */
    private fun updateConnectionState(connected: Boolean) {
        isConnected = connected
        _connectionState.postValue(connected)
    }

    /**
     * Sets up a network callback to monitor network changes
     * This should be called only once, preferably when the app starts
     */
    fun registerNetworkCallback() {
        // Avoid registering multiple callbacks
        if (networkCallback != null) return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network available")
                // Don't immediately update state here - wait for capabilities check
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

    /**
     * Unregisters the network callback to prevent memory leaks
     * This should be called when the app is being destroyed
     */
    fun unregisterNetworkCallback() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
}