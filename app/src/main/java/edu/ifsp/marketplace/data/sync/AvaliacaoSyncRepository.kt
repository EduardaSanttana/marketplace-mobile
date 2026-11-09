package edu.ifsp.marketplace.data.sync

import edu.ifsp.marketplace.data.Avaliacao
import edu.ifsp.marketplace.data.AvaliacaoDao
import edu.ifsp.marketplace.data.ProdutoDao
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AvaliacaoSyncRepository(
    private val dao: AvaliacaoDao,
    private val produtoDao: ProdutoDao,
    firestore: FirebaseFirestore,
    private val scope: CoroutineScope,
    private val religar: suspend () -> Unit
) {
    private val collection = firestore.collection("avaliacoes")
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
                val remote = doc.toObject(Avaliacao::class.java).copy(firestoreId = doc.id)
                val local = dao.getByFirestoreId(doc.id)
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    val produto = remote.produtoFirestoreId ?: local?.produtoFirestoreId
                    dao.upsertFromRemote(
                        remote.copy(
                            id = local?.id ?: 0,
                            produtoId = idLocalDoVinculo(
                                produto,
                                produto?.let { produtoDao.getByFirestoreId(it)?.id },
                                local?.produtoId,
                                remote.produtoId
                            ),
                            produtoFirestoreId = produto,
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
        for (avaliacao in dao.getPendingDelete()) {
            avaliacao.firestoreId?.let { collection.document(it).delete().await() }
            dao.hardDeleteById(avaliacao.id)
        }
        for (avaliacao in dao.getPendingSync()) {
            // Mesmo critério dos produtos: o id local só é confiável no aparelho de origem
            val produto = avaliacao.produtoFirestoreId
                ?: if (avaliacao.firestoreId == null) produtoDao.getById(avaliacao.produtoId)?.firestoreId
                else null
            val dados = mutableMapOf<String, Any?>(
                "produtoId" to avaliacao.produtoId,
                "nota" to avaliacao.nota,
                "comentario" to avaliacao.comentario,
                "updatedAt" to avaliacao.updatedAt
            )
            produto?.let { dados["produtoFirestoreId"] = it }

            val firestoreId = avaliacao.firestoreId
            if (firestoreId == null) {
                val ref = collection.add(dados).await()
                dao.update(avaliacao.copy(firestoreId = ref.id, produtoFirestoreId = produto, pendingSync = false))
            } else {
                collection.document(firestoreId).set(dados, SetOptions.merge()).await()
                dao.update(avaliacao.copy(produtoFirestoreId = produto, pendingSync = false))
            }
        }
    }
}
