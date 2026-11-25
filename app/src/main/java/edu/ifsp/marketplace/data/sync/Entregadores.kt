package edu.ifsp.marketplace.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Quem é entregador é decidido pela equipe, não pela própria conta: cada
 * entregador tem um documento na coleção `entregadores`, com o e-mail da conta
 * como id. Os documentos são criados pelo console do Firebase, e as regras do
 * Firestore (`firestore.rules`) impedem que o app grave nessa coleção.
 */
const val COLECAO_ENTREGADORES = "entregadores"

/** Id do documento do entregador: o e-mail da conta, sem espaços e em minúsculas. */
internal fun idDoEntregador(email: String?): String? =
    email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

/**
 * Emite se a conta está na lista de entregadores, e de novo sempre que a lista
 * mudar no console. Sem rede, responde pelo cache do Firestore; erro de leitura
 * (permissão negada, por exemplo) conta como "não é entregador".
 */
fun observarSeEhEntregador(
    email: String?,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
): Flow<Boolean> {
    val id = idDoEntregador(email) ?: return flowOf(false)
    return callbackFlow {
        val registro = firestore.collection(COLECAO_ENTREGADORES).document(id)
            .addSnapshotListener { doc, erro ->
                trySend(erro == null && doc?.exists() == true)
            }
        awaitClose { registro.remove() }
    }
}
