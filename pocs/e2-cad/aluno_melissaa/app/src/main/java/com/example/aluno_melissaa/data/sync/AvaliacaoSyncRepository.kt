package com.example.aluno_melissaa.data.sync

import com.example.aluno_melissaa.data.Avaliacao
import com.example.aluno_melissaa.data.AvaliacaoDao
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AvaliacaoSyncRepository(
    private val dao: AvaliacaoDao,
    firestore: FirebaseFirestore,
    private val scope: CoroutineScope
) {
    private val collection = firestore.collection("avaliacoes")
    private var listener: ListenerRegistration? = null

    fun startListening() {
        if (listener != null) return
        listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            for (change in snapshot.documentChanges) {
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
                    dao.upsertFromRemote(
                        remote.copy(id = local?.id ?: 0, pendingSync = false, pendingDelete = false)
                    )
                }
            }
            DocumentChange.Type.REMOVED -> dao.hardDeleteByFirestoreId(doc.id)
        }
    }

    suspend fun pushPendingChanges() {
        for (avaliacao in dao.getPendingDelete()) {
            avaliacao.firestoreId?.let { collection.document(it).delete().await() }
            dao.hardDeleteById(avaliacao.id)
        }
        for (avaliacao in dao.getPendingSync()) {
            val dados = mapOf(
                "produtoId" to avaliacao.produtoId,
                "nota" to avaliacao.nota,
                "comentario" to avaliacao.comentario,
                "updatedAt" to avaliacao.updatedAt
            )
            val firestoreId = avaliacao.firestoreId
            if (firestoreId == null) {
                val ref = collection.add(dados).await()
                dao.update(avaliacao.copy(firestoreId = ref.id, pendingSync = false))
            } else {
                collection.document(firestoreId).set(dados, SetOptions.merge()).await()
                dao.update(avaliacao.copy(pendingSync = false))
            }
        }
    }
}
