package edu.ifsp.marketplace.geofencing

import android.content.Context
import edu.ifsp.marketplace.data.AppDatabase

/**
 * O que acontece quando o entregador entra no perímetro do destino, venha o
 * aviso do geofence ([GeofenceReceiver]) ou da tela de rota aberta.
 */
object ChegadaEntregador {

    /**
     * Marca o pedido, desliga o geofence e, se a conta deste aparelho for a do
     * comprador (a mesma conta demonstrando os dois perfis), já notifica aqui.
     * Nos outros aparelhos o aviso chega pela sincronização com o Firestore.
     *
     * @return true se a chegada foi marcada agora (e precisa ser sincronizada)
     */
    suspend fun registrar(context: Context, db: AppDatabase, pedidoId: Long): Boolean {
        GeofenceEntrega.remover(context, pedidoId)
        val marcou = db.pedidoDao().marcarEntregadorProximo(pedidoId, System.currentTimeMillis()) > 0
        if (marcou) {
            db.pedidoDao().getById(pedidoId)?.let { NotificacaoEntrega.avisarSeForComprador(context, db, it) }
        }
        return marcou
    }
}
