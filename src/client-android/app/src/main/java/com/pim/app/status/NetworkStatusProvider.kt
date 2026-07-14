package com.pim.app.status

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkAvailability { Unavailable, Restricted, Validated }

@Singleton
class NetworkStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val availability: Flow<NetworkAvailability> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            trySend(NetworkAvailability.Unavailable)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(safeNetworkRead {
                    val caps = connectivityManager.getNetworkCapabilities(network)
                    availabilityFor(hasNetwork = true, capabilities = caps)
                })
            }

            override fun onLost(network: Network) {
                trySend(safeNetworkRead {
                    val active = connectivityManager.activeNetwork
                    availabilityFor(
                        hasNetwork = active != null,
                        capabilities = connectivityManager.getNetworkCapabilities(active)
                    )
                })
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(availabilityFor(hasNetwork = true, capabilities = capabilities))
            }
        }

        val initial = safeNetworkRead {
            val activeNetwork = connectivityManager.activeNetwork
            availabilityFor(
                hasNetwork = activeNetwork != null,
                capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            )
        }
        trySend(initial)

        var registered = false
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
            registered = true
        } catch (_: SecurityException) {
            trySend(NetworkAvailability.Unavailable)
        }

        awaitClose {
            if (registered) {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: SecurityException) {
                    // The flow is already closing; fail closed without crashing the collector.
                }
            }
        }
    }.distinctUntilChanged()

    companion object {
        internal fun availabilityFor(
            hasNetwork: Boolean,
            capabilities: NetworkCapabilities?
        ): NetworkAvailability {
            if (!hasNetwork) return NetworkAvailability.Unavailable
            if (capabilities == null) return NetworkAvailability.Restricted
            val hasInternet = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            val hasValidated = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
            return if (hasInternet && hasValidated) NetworkAvailability.Validated
            else NetworkAvailability.Restricted
        }

        internal inline fun safeNetworkRead(
            block: () -> NetworkAvailability
        ): NetworkAvailability = try {
            block()
        } catch (_: SecurityException) {
            NetworkAvailability.Unavailable
        }
    }
}
