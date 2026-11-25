package edu.ifsp.marketplace.data.sync

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Última posição conhecida do entregador num pedido em transporte. */
data class PosicaoEntregador(val posicao: LatLng, val atualizadoEm: Long)

/**
 * Posição ao vivo do entregador, para o comprador acompanhar no mapa.
 *
 * Fica fora do Room e da coleção "pedidos" de propósito: muda a cada poucos
 * segundos e não faz parte do histórico do pedido. Cada pedido tem um documento
 * em "rastreio" com o mesmo id do pedido no Firestore; o aparelho do entregador
 * grava e o do comprador escuta.
 */
object RastreioEntregador {

    private val colecao get() = FirebaseFirestore.getInstance().collection("rastreio")

    /** Publica a posição atual. Sem conexão, o Firestore guarda e envia depois. */
    fun publicar(pedidoFirestoreId: String, posicao: LatLng) {
        colecao.document(pedidoFirestoreId).set(
            mapOf(
                "latitude" to posicao.latitude,
                "longitude" to posicao.longitude,
                "atualizadoEm" to System.currentTimeMillis()
            )
        )
    }

    /** Some com a posição quando a entrega termina: o comprador não precisa mais dela. */
    fun encerrar(pedidoFirestoreId: String) {
        colecao.document(pedidoFirestoreId).delete()
    }

    /** Emite a posição do entregador a cada atualização, ou null se ainda não há nenhuma. */
    fun acompanhar(pedidoFirestoreId: String): Flow<PosicaoEntregador?> = callbackFlow {
        val registro = colecao.document(pedidoFirestoreId).addSnapshotListener { doc, erro ->
            if (erro != null) return@addSnapshotListener
            val lat = doc?.getDouble("latitude")
            val lng = doc?.getDouble("longitude")
            trySend(
                if (lat != null && lng != null) {
                    PosicaoEntregador(LatLng(lat, lng), doc.getLong("atualizadoEm") ?: 0L)
                } else null
            )
        }
        awaitClose { registro.remove() }
    }
}
