package edu.ifsp.marketplace.data.sync

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.PedidoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PedidoSyncRepository(
    private val dao: PedidoDao,
    firestore: FirebaseFirestore,
    private val scope: CoroutineScope
) {
    private val collection = firestore.collection("pedidos")
    private var listener: ListenerRegistration? = null

    fun startListening() {
        if (listener != null) return
        listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            for (change in snapshot.documentChanges) {
                if (change.document.metadata.hasPendingWrites()) continue
                scope.launch { applyRemoteChange(change) }
            }
        }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }

    private suspend fun applyRemoteChange(change: DocumentChange) {
        val doc = change.document
        when (change.type) {
            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                val remote = doc.toObject(Pedido::class.java).copy(firestoreId = doc.id)
                val local = dao.getByFirestoreId(doc.id)
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    dao.upsertFromRemote(
                        remote.copy(
                            id = local?.id ?: 0,
                            // A foto da entrega vive só no dispositivo que a tirou
                            fotoEntregaPath = local?.fotoEntregaPath,
                            pendingSync = false,
                            pendingDelete = false
                        )
                    )
                }
            }

            DocumentChange.Type.REMOVED -> dao.hardDeleteByFirestoreId(doc.id)
        }
    }

    suspend fun pushPendingChanges() {
        for (pedido in dao.getPendingDelete()) {
            pedido.firestoreId?.let { collection.document(it).delete().await() }
            dao.hardDeleteById(pedido.id)
        }
        for (pedido in dao.getPendingSync()) {
            val dados = mapOf(
                "produtoId" to pedido.produtoId,
                "produtoNome" to pedido.produtoNome,
                "compradorId" to pedido.compradorId,
                "compradorNome" to pedido.compradorNome,
                "quantidade" to pedido.quantidade,
                "valorTotal" to pedido.valorTotal,
                "status" to pedido.status,
                "enderecoEntrega" to pedido.enderecoEntrega,
                "latitudeEntrega" to pedido.latitudeEntrega,
                "longitudeEntrega" to pedido.longitudeEntrega,
                "latitudeEntregue" to pedido.latitudeEntregue,
                "longitudeEntregue" to pedido.longitudeEntregue,
                "enderecoEntregue" to pedido.enderecoEntregue,
                "entregueEm" to pedido.entregueEm,
                "updatedAt" to pedido.updatedAt
            )
            val firestoreId = pedido.firestoreId
            if (firestoreId == null) {
                val ref = collection.add(dados).await()
                dao.update(pedido.copy(firestoreId = ref.id, pendingSync = false))
            } else {
                collection.document(firestoreId).set(dados, SetOptions.merge()).await()
                dao.update(pedido.copy(pendingSync = false))
            }
        }
    }
}
