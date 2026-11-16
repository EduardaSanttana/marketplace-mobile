package edu.ifsp.marketplace.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import edu.ifsp.marketplace.data.AppDatabase
import edu.ifsp.marketplace.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Chamado pelo Play Services quando o entregador entra no perímetro de um
 * ponto de entrega, mesmo com o app em segundo plano.
 *
 * Grava a chegada no SQLite e tenta enviá-la ao Firestore na hora. Sem conexão,
 * ela fica pendente e segue na próxima sincronização, como qualquer outro dado.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) return
        if (evento.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val pedidos = evento.triggeringGeofences.orEmpty()
            .mapNotNull { GeofenceEntrega.pedidoDoGeofence(it.requestId) }
        if (pedidos.isEmpty()) return

        val app = context.applicationContext
        // O receiver tem cerca de 10 s: goAsync segura o processo enquanto grava e envia
        val pendente = goAsync()
        val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        escopo.launch {
            try {
                val db = AppDatabase.getInstance(app)
                var marcou = false
                for (id in pedidos) {
                    marcou = ChegadaEntregador.registrar(app, db, id) || marcou
                }
                if (marcou) {
                    withTimeoutOrNull(8_000) { SyncManager(db, escopo).syncAgora() }
                }
            } finally {
                pendente.finish()
                escopo.cancel()
            }
        }
    }
}
