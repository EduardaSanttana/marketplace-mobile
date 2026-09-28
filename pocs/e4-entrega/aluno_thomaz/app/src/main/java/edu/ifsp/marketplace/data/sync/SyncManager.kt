package edu.ifsp.marketplace.data.sync

import edu.ifsp.marketplace.data.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncStatus { OCIOSO, SINCRONIZANDO, ERRO }

class SyncManager(
    db: AppDatabase,
    scope: CoroutineScope,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val produtoRepo = ProdutoSyncRepository(db.produtoDao(), firestore, scope)
    private val negocianteRepo = NegocianteSyncRepository(db.negocianteDao(), firestore, scope)
    private val avaliacaoRepo = AvaliacaoSyncRepository(db.avaliacaoDao(), firestore, scope)

    private val mutex = Mutex()

    private val _status = MutableStateFlow(SyncStatus.OCIOSO)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

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
        mutex.withLock {
            _status.value = SyncStatus.SINCRONIZANDO
            try {
                produtoRepo.pushPendingChanges()
                negocianteRepo.pushPendingChanges()
                avaliacaoRepo.pushPendingChanges()
                _status.value = SyncStatus.OCIOSO
            } catch (e: Exception) {
                _status.value = SyncStatus.ERRO
            }
        }
    }
}
