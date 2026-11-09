package edu.ifsp.marketplace.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun observarConectividade(context: Context): Flow<Boolean> = callbackFlow {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Wi-Fi e dados móveis são redes separadas: a queda de uma não quer dizer que
    // o aparelho ficou sem conexão. Conectado é ter ao menos uma rede disponível
    val disponiveis = mutableSetOf<Network>()
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(disponiveis) { disponiveis.add(network) }
            trySend(true)
        }

        override fun onLost(network: Network) {
            val restantes = synchronized(disponiveis) {
                disponiveis.remove(network)
                disponiveis.size
            }
            trySend(restantes > 0)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(request, callback)
    trySend(temConexaoAtiva(connectivityManager))

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

private fun temConexaoAtiva(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
