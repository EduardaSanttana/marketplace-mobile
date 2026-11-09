package edu.ifsp.marketplace.data.sync

import edu.ifsp.marketplace.data.NegocianteDao
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.data.ProdutoDao
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProdutoSyncRepository(
    private val dao: ProdutoDao,
    private val negocianteDao: NegocianteDao,
    firestore: FirebaseFirestore,
    private val scope: CoroutineScope,
    private val religar: suspend () -> Unit
) {
    private val collection = firestore.collection("produtos")
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
                val remote = doc.toObject(Produto::class.java).copy(firestoreId = doc.id)
                val local = dao.getByFirestoreId(doc.id)
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    val vendedor = remote.negocianteFirestoreId ?: local?.negocianteFirestoreId
                    dao.upsertFromRemote(
                        remote.copy(
                            id = local?.id ?: 0,
                            negocianteId = idLocalDoVinculo(
                                vendedor,
                                vendedor?.let { negocianteDao.getByFirestoreId(it)?.id },
                                local?.negocianteId,
                                remote.negocianteId
                            ),
                            negocianteFirestoreId = vendedor,
                            fotoPath = local?.fotoPath,
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
        for (produto in dao.getPendingDelete()) {
            produto.firestoreId?.let { collection.document(it).delete().await() }
            dao.hardDeleteById(produto.id)
        }
        for (produto in dao.getPendingSync()) {
            // Só um produto criado aqui confia no id local do vendedor. Um que veio de
            // outro aparelho sem o vínculo pode ter o id local errado: fica sem ele
            val vendedor = produto.negocianteFirestoreId
                ?: if (produto.firestoreId == null) negocianteDao.getById(produto.negocianteId)?.firestoreId
                else null
            val dados = mutableMapOf<String, Any?>(
                "nome" to produto.nome,
                "descricao" to produto.descricao,
                "preco" to produto.preco,
                "quantidade" to produto.quantidade,
                "negocianteId" to produto.negocianteId,
                "updatedAt" to produto.updatedAt
            )
            // Ausente, e não nulo: o merge não apaga o vínculo gravado por outro aparelho
            vendedor?.let { dados["negocianteFirestoreId"] = it }

            val firestoreId = produto.firestoreId
            if (firestoreId == null) {
                val ref = collection.add(dados).await()
                dao.update(produto.copy(firestoreId = ref.id, negocianteFirestoreId = vendedor, pendingSync = false))
            } else {
                collection.document(firestoreId).set(dados, SetOptions.merge()).await()
                dao.update(produto.copy(negocianteFirestoreId = vendedor, pendingSync = false))
            }
        }
    }
}
