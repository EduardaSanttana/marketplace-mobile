package edu.ifsp.marketplace.data.sync

import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.data.NegocianteDao
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NegocianteSyncRepository(
    private val dao: NegocianteDao,
    firestore: FirebaseFirestore,
    private val scope: CoroutineScope
) {
    private val collection = firestore.collection("negociantes")
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
                val remote = doc.toObject(Negociante::class.java).copy(firestoreId = doc.id)
                val local = dao.getByFirestoreId(doc.id)
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    dao.upsertFromRemote(
                        remote.copy(id = local?.id ?: 0, pendingSync = false, pendingDelete = false)
                    )
                }
            }
            DocumentChange.Type.REMOVED -> dao.hardDeleteByFirestoreId(doc.id)
        }
    }

    suspend fun pushPendingChanges() {
        for (negociante in dao.getPendingDelete()) {
            negociante.firestoreId?.let { collection.document(it).delete().await() }
            dao.hardDeleteById(negociante.id)
        }
        for (negociante in dao.getPendingSync()) {
            val dados = mapOf(
                "nome" to negociante.nome,
                "email" to negociante.email,
                "telefone" to negociante.telefone,
                "updatedAt" to negociante.updatedAt
            )
            val firestoreId = negociante.firestoreId
            if (firestoreId == null) {
                val ref = collection.add(dados).await()
                dao.update(negociante.copy(firestoreId = ref.id, pendingSync = false))
            } else {
                collection.document(firestoreId).set(dados, SetOptions.merge()).await()
                dao.update(negociante.copy(pendingSync = false))
            }
        }
    }
}
