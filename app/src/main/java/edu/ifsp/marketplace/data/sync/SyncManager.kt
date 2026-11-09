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
    private val vinculos = Vinculos(db)

    private val produtoRepo = ProdutoSyncRepository(db.produtoDao(), db.negocianteDao(), firestore, scope, vinculos::religar)
    private val negocianteRepo = NegocianteSyncRepository(db.negocianteDao(), firestore, scope, vinculos::religar)
    private val avaliacaoRepo = AvaliacaoSyncRepository(db.avaliacaoDao(), db.produtoDao(), firestore, scope, vinculos::religar)
    private val pedidoRepo = PedidoSyncRepository(db.pedidoDao(), db.produtoDao(), db.negocianteDao(), firestore, scope, vinculos::religar)

    private val mutex = Mutex()

    private val _status = MutableStateFlow(SyncStatus.OCIOSO)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun startRealtimeSync() {
        produtoRepo.startListening()
        negocianteRepo.startListening()
        avaliacaoRepo.startListening()
        pedidoRepo.startListening()
    }

    fun stopRealtimeSync() {
        produtoRepo.stopListening()
        negocianteRepo.stopListening()
        avaliacaoRepo.stopListening()
        pedidoRepo.stopListening()
    }

    suspend fun syncAgora() {
        mutex.withLock {
            _status.value = SyncStatus.SINCRONIZANDO
            try {
                vinculos.religar()
                // Quem é referenciado vai antes: o envio de um produto precisa do id do
                // vendedor no Firestore, e o de um pedido, dos ids do produto e dos negociantes
                negocianteRepo.pushPendingChanges()
                produtoRepo.pushPendingChanges()
                avaliacaoRepo.pushPendingChanges()
                pedidoRepo.pushPendingChanges()
                _status.value = SyncStatus.OCIOSO
            } catch (e: Exception) {
                _status.value = SyncStatus.ERRO
            }
        }
    }
}
