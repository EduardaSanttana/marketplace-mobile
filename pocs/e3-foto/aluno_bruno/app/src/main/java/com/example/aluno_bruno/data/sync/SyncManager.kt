package com.example.aluno_bruno.data.sync

import com.example.aluno_bruno.data.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncStatus { OCIOSO, SINCRONIZANDO, ERRO }

class SyncManager(
    db: AppDatabase,
    scope: CoroutineScope,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val produtoRepo = ProdutoSyncRepository(db.produtoDao(), firestore, scope)
    private val negocianteRepo = NegocianteSyncRepository(db.negocianteDao(), firestore, scope)
    private val avaliacaoRepo = AvaliacaoSyncRepository(db.avaliacaoDao(), firestore, scope)

    private val _status = MutableStateFlow(SyncStatus.OCIOSO)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _ultimaSincronizacao = MutableStateFlow<Long?>(null)
    val ultimaSincronizacao: StateFlow<Long?> = _ultimaSincronizacao.asStateFlow()

    fun startRealtimeSync() {
        produtoRepo.startListening()
        negocianteRepo.startListening()
        avaliacaoRepo.startListening()
    }

    fun stopRealtimeSync() {
        produtoRepo.stopListening()
        negocianteRepo.stopListening()
        avaliacaoRepo.stopListening()
    }

    suspend fun syncAgora() {
        _status.value = SyncStatus.SINCRONIZANDO
        try {
            produtoRepo.pushPendingChanges()
            negocianteRepo.pushPendingChanges()
            avaliacaoRepo.pushPendingChanges()
            _ultimaSincronizacao.value = System.currentTimeMillis()
            _status.value = SyncStatus.OCIOSO
        } catch (e: Exception) {
            _status.value = SyncStatus.ERRO
        }
    }
}
