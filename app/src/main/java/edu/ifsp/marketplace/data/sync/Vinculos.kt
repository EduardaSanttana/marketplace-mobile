package edu.ifsp.marketplace.data.sync

import edu.ifsp.marketplace.data.AppDatabase

/**
 * Escolhe o id local de uma referência (vendedor, comprador, produto) que chegou
 * de outro aparelho.
 *
 * O id local de um registro muda de aparelho para aparelho; o id do Firestore é o
 * mesmo em todos. Por isso a referência vale pelo id do Firestore, e o id local é
 * só a tradução dele neste aparelho.
 *
 * @param firestoreIdDoVinculo id do Firestore do registro referenciado, se o documento o tiver
 * @param idPeloFirestore id local do registro com esse id do Firestore, se ele já chegou aqui
 * @param idLocalAtual valor que este aparelho já tinha para a referência
 * @param idRemoto id local do aparelho de origem: só serve a documentos antigos, sem o vínculo
 */
internal fun idLocalDoVinculo(
    firestoreIdDoVinculo: String?,
    idPeloFirestore: Long?,
    idLocalAtual: Long?,
    idRemoto: Long
): Long = when {
    idPeloFirestore != null -> idPeloFirestore
    // O registro referenciado ainda não chegou: Vinculos.religar() corrige quando chegar
    firestoreIdDoVinculo != null -> 0
    else -> idLocalAtual ?: idRemoto
}

/**
 * Corrige os ids locais a partir dos ids do Firestore.
 *
 * Roda depois de cada mudança vinda da nuvem: um pedido pode chegar antes do
 * produto ou do negociante a que se refere.
 */
class Vinculos(private val db: AppDatabase) {

    suspend fun religar() {
        db.produtoDao().religarVendedores()
        db.avaliacaoDao().religarProdutos()
        db.pedidoDao().apply {
            religarProdutos()
            religarCompradores()
            religarVendedores()
            religarCompradoresPeloNome()
            religarVendedoresPeloNome()
            religarProdutosPeloNome()
        }
    }
}
