package com.mints.mobilehealthapplication.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log

class InternetConnectionChecker(private val context: Context) {

    private val TAG = "InternetConnectionChecker"

    // Variable to store the connection status
    var isConnected: Boolean = false
        private set

    /**
     * Checks if the device has an active internet connection
     * @return Boolean indicating if internet is available
     */
    fun checkInternetConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For Android 6.0+ (API 23+)
        val network: Network? = connectivityManager.activeNetwork
        if (network == null) {
            Log.d(TAG, "No active network detected")
            isConnected = false
            return false
        }

        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        if (networkCapabilities == null) {
            Log.d(TAG, "No network capabilities detected")
            isConnected = false
            return false
        }

        // Check if the device has internet capability
        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        isConnected = hasInternet

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
     * Sets up a network callback to monitor network changes
     */
    fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isConnected = true
                Log.d(TAG, "Network available")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isConnected = false
                Log.d(TAG, "Network lost")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                isConnected = hasInternet
                Log.d(TAG, "Network capabilities changed, internet available: $hasInternet")
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }
}