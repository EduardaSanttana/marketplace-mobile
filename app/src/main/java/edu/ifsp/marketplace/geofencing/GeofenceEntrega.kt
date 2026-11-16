package edu.ifsp.marketplace.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import edu.ifsp.marketplace.data.Pedido
import kotlinx.coroutines.tasks.await

/**
 * Perímetro (geofence) em volta do ponto de entrega de um pedido em transporte.
 *
 * Veio da PoC `pocs/e7-geofencing/aluno_bruno`. Quem vigia o perímetro é o
 * Google Play Services, mesmo com o app fechado: ao entrar nele, o sistema
 * chama [GeofenceReceiver], que marca o pedido e avisa o comprador.
 *
 * A partir do Android 10 o geofence só é aceito com a localização em segundo
 * plano liberada. Sem ela, [registrar] devolve false e a tela de rota faz a
 * mesma conta enquanto está aberta (ver `RotaEntregaScreen`).
 */
object GeofenceEntrega {

    /** O geofence some sozinho depois disso, caso a entrega nunca seja registrada. */
    private const val DURACAO_MS = 12 * 60 * 60 * 1000L

    private const val PREFIXO = "pedido-"

    fun idDoGeofence(pedidoId: Long) = "$PREFIXO$pedidoId"

    fun pedidoDoGeofence(requestId: String): Long? =
        requestId.removePrefix(PREFIXO).toLongOrNull()

    fun temPermissaoSegundoPlano(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun intentPendente(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        // O Play Services preenche o intent com o evento: precisa ser mutável
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Cria (ou substitui) o perímetro do pedido. Se o entregador já estiver dentro
     * ao registrar, o disparo é imediato.
     *
     * @return true se o sistema aceitou o geofence
     */
    @SuppressLint("MissingPermission")
    suspend fun registrar(context: Context, pedido: Pedido): Boolean {
        val latitude = pedido.latitudeEntrega ?: return false
        val longitude = pedido.longitudeEntrega ?: return false
        if (!temPermissaoSegundoPlano(context)) return false

        val geofence = Geofence.Builder()
            .setRequestId(idDoGeofence(pedido.id))
            .setCircularRegion(latitude, longitude, Pedido.RAIO_ENTREGADOR_PROXIMO_METROS)
            .setExpirationDuration(DURACAO_MS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return try {
            LocationServices.getGeofencingClient(context)
                .addGeofences(request, intentPendente(context))
                .await()
            true
        } catch (e: Exception) {
            // GEOFENCE_NOT_AVAILABLE (localização desligada) ou falta de permissão
            false
        }
    }

    fun remover(context: Context, pedidoId: Long) {
        LocationServices.getGeofencingClient(context)
            .removeGeofences(listOf(idDoGeofence(pedidoId)))
    }
}
