package edu.ifsp.marketplace.data.sync

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import edu.ifsp.marketplace.data.NegocianteDao
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.PedidoDao
import edu.ifsp.marketplace.data.ProdutoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PedidoSyncRepository(
    private val dao: PedidoDao,
    private val produtoDao: ProdutoDao,
    private val negocianteDao: NegocianteDao,
    firestore: FirebaseFirestore,
    private val scope: CoroutineScope,
    private val religar: suspend () -> Unit
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

    private suspend fun idDoNegociante(firestoreId: String?): Long? =
        firestoreId?.let { negocianteDao.getByFirestoreId(it)?.id }

    private suspend fun applyRemoteChange(change: DocumentChange) {
        val doc = change.document
        when (change.type) {
            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                val remote = doc.toObject(Pedido::class.java).copy(firestoreId = doc.id)
                val local = dao.getByFirestoreId(doc.id)
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    val produto = remote.produtoFirestoreId ?: local?.produtoFirestoreId
                    val comprador = remote.compradorFirestoreId ?: local?.compradorFirestoreId
                    val vendedor = remote.vendedorFirestoreId ?: local?.vendedorFirestoreId
                    dao.upsertFromRemote(
                        remote.copy(
                            id = local?.id ?: 0,
                            produtoId = idLocalDoVinculo(
                                produto,
                                produto?.let { produtoDao.getByFirestoreId(it)?.id },
                                local?.produtoId,
                                remote.produtoId
                            ),
                            compradorId = idLocalDoVinculo(
                                comprador, idDoNegociante(comprador), local?.compradorId, remote.compradorId
                            ),
                            vendedorId = idLocalDoVinculo(
                                vendedor, idDoNegociante(vendedor), local?.vendedorId, remote.vendedorId
                            ),
                            produtoFirestoreId = produto,
                            compradorFirestoreId = comprador,
                            vendedorFirestoreId = vendedor,
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
        religar()
    }

    suspend fun pushPendingChanges() {
        for (pedido in dao.getPendingDelete()) {
            pedido.firestoreId?.let { collection.document(it).delete().await() }
            dao.hardDeleteById(pedido.id)
        }
        for (pedido in dao.getPendingSync()) {
            // Só um pedido criado aqui confia nos ids locais; os que vieram de outro
            // aparelho sem os vínculos ficam sem eles, em vez de ganhar vínculos errados
            val criadoAqui = pedido.firestoreId == null
            val produto = pedido.produtoFirestoreId
                ?: if (criadoAqui) produtoDao.getById(pedido.produtoId)?.firestoreId else null
            val comprador = pedido.compradorFirestoreId
                ?: if (criadoAqui) negocianteDao.getById(pedido.compradorId)?.firestoreId else null
            val vendedor = pedido.vendedorFirestoreId
                ?: if (criadoAqui) negocianteDao.getById(pedido.vendedorId)?.firestoreId else null
            if (produto != pedido.produtoFirestoreId || comprador != pedido.compradorFirestoreId ||
                vendedor != pedido.vendedorFirestoreId
            ) {
                dao.definirVinculos(pedido.id, produto, comprador, vendedor)
            }

            val dados = mutableMapOf<String, Any?>(
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
                "vendedorId" to pedido.vendedorId,
                "vendedorNome" to pedido.vendedorNome,
                "enderecoRetirada" to pedido.enderecoRetirada,
                "latitudeRetirada" to pedido.latitudeRetirada,
                "longitudeRetirada" to pedido.longitudeRetirada,
                "latitudeEntregue" to pedido.latitudeEntregue,
                "longitudeEntregue" to pedido.longitudeEntregue,
                "enderecoEntregue" to pedido.enderecoEntregue,
                "entregueEm" to pedido.entregueEm,
                "updatedAt" to pedido.updatedAt
            )
            // Ausentes, e não nulos: o merge não apaga o que outro aparelho já gravou
            pedido.criadoEm?.let { dados["criadoEm"] = it }
            pedido.retiradoEm?.let { dados["retiradoEm"] = it }
            produto?.let { dados["produtoFirestoreId"] = it }
            comprador?.let { dados["compradorFirestoreId"] = it }
            vendedor?.let { dados["vendedorFirestoreId"] = it }

            val firestoreId = pedido.firestoreId
            if (firestoreId == null) {
                val ref = collection.add(dados).await()
                dao.marcarSincronizado(pedido.id, ref.id, pedido.updatedAt)
            } else {
                collection.document(firestoreId).set(dados, SetOptions.merge()).await()
                dao.marcarSincronizado(pedido.id, firestoreId, pedido.updatedAt)
            }
        }
    }
}
