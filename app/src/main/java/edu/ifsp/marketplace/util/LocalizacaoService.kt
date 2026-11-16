package edu.ifsp.marketplace.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Posição atual do aparelho, usada tanto na rota quanto no registro da entrega. */
object LocalizacaoService {

    // A partir do Android 12 o sistema ignora um pedido só de FINE: as duas vão juntas
    val PERMISSOES = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun temPermissao(context: Context): Boolean = PERMISSOES.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** @return a posição atual, a última conhecida, ou null se o GPS não responder */
    @SuppressLint("MissingPermission")
    suspend fun posicaoAtual(context: Context): LatLng? {
        if (!temPermissao(context)) return null
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val cancelamento = CancellationTokenSource()
        return try {
            val local = withTimeoutOrNull(15_000) {
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelamento.token)
                    .await(cancelamento)
            } ?: cliente.lastLocation.await()
            local?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: ApiException) {
            null
        }
    }

    /**
     * Posições do aparelho enquanto alguém coleta o fluxo. Usado pela tela de
     * rota para conferir a chegada ao destino quando o geofence não está ativo.
     */
    @SuppressLint("MissingPermission")
    fun atualizacoes(context: Context, intervaloMs: Long = 5_000): Flow<LatLng> = callbackFlow {
        if (!temPermissao(context)) {
            close()
            return@callbackFlow
        }
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let { trySend(LatLng(it.latitude, it.longitude)) }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervaloMs).build()
        cliente.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { cliente.removeLocationUpdates(callback) }
    }
}
