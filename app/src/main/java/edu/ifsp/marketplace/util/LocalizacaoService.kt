package edu.ifsp.marketplace.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
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
}
