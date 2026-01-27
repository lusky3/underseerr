package app.lusk.underseerr.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Manages network connectivity monitoring.
 * Feature: underseerr
 * Validates: Requirements 2.6, 7.1
 */
class NetworkManager(
    private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Network connectivity state.
     */
    data class NetworkState(
        val isConnected: Boolean,
        val isMetered: Boolean = false,
        val networkType: NetworkType = NetworkType.UNKNOWN
    )
    
    /**
     * Types of network connections.
     */
    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN,
        UNKNOWN
    }
    
    /**
     * Connectivity status enum for simplified state tracking.
     */
    enum class ConnectivityStatus {
        CONNECTED,
        DISCONNECTED
    }
    
    /**
     * Observe network connectivity changes as a Flow.
     * 
     * @return Flow emitting network state changes
     */
    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()
            
            override fun onAvailable(network: Network) {
                networks.add(network)
                trySend(getCurrentNetworkState())
            }
            
            override fun onLost(network: Network) {
                networks.remove(network)
                trySend(getCurrentNetworkState())
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentNetworkState())
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Emit initial state
        trySend(getCurrentNetworkState())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Observe connectivity status as a simplified Flow.
     * 
     * @return Flow emitting connectivity status changes
     */
    val connectivityStatus: Flow<ConnectivityStatus> = observeNetworkState()
        .map { state ->
            if (state.isConnected) ConnectivityStatus.CONNECTED
            else ConnectivityStatus.DISCONNECTED
        }
        .distinctUntilChanged()
    
    /**
     * Get current network state synchronously.
     * 
     * @return Current network state
     */
    fun getCurrentNetworkState(): NetworkState {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { 
            connectivityManager.getNetworkCapabilities(it) 
        }
        
        if (capabilities == null) {
            return NetworkState(
                isConnected = false,
                isMetered = false,
                networkType = NetworkType.UNKNOWN
            )
        }
        
        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        
        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }
        
        return NetworkState(
            isConnected = isConnected,
            isMetered = isMetered,
            networkType = networkType
        )
    }
    
    /**
     * Check if device is currently connected to the internet.
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return getCurrentNetworkState().isConnected
    }
    
    /**
     * Check if current connection is metered (e.g., cellular data).
     * 
     * @return true if metered, false otherwise
     */
    fun isMetered(): Boolean {
        return getCurrentNetworkState().isMetered
    }
    
    /**
     * Get current network type.
     * 
     * @return Current network type
     */
    fun getNetworkType(): NetworkType {
        return getCurrentNetworkState().networkType
    }
    
    /**
     * Check if device is connected to WiFi.
     * 
     * @return true if connected to WiFi, false otherwise
     */
    fun isWifiConnected(): Boolean {
        return getNetworkType() == NetworkType.WIFI
    }
    
    /**
     * Check if device is connected to cellular network.
     * 
     * @return true if connected to cellular, false otherwise
     */
    fun isCellularConnected(): Boolean {
        return getNetworkType() == NetworkType.CELLULAR
    }
}
