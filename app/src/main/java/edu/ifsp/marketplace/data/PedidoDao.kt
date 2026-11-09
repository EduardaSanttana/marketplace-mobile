package edu.ifsp.marketplace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    @Insert
    suspend fun insert(pedido: Pedido): Long

    @Update
    suspend fun update(pedido: Pedido)

    @Delete
    suspend fun delete(pedido: Pedido)

    // Um pedido apagado sem conexão espera o envio da exclusão, mas já sai da lista
    @Query("SELECT * FROM pedidos WHERE pendingDelete = 0 ORDER BY id DESC")
    fun getAll(): Flow<List<Pedido>>

    @Query("SELECT * FROM pedidos WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<Pedido>

    @Query("SELECT * FROM pedidos WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<Pedido>

    @Query("SELECT * FROM pedidos WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): Pedido?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromRemote(pedido: Pedido): Long

    @Query("DELETE FROM pedidos WHERE id = :id")
    suspend fun hardDeleteById(id: Long)

    @Query("DELETE FROM pedidos WHERE firestoreId = :firestoreId")
    suspend fun hardDeleteByFirestoreId(firestoreId: String)

    @Query("UPDATE pedidos SET pendingDelete = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)

    /** O entregador pegou o pacote: em transporte, com a hora da retirada para a linha do tempo. */
    @Query("UPDATE pedidos SET status = 'EM_TRANSPORTE', retiradoEm = :agora, updatedAt = :agora, pendingSync = 1 WHERE id = :id")
    suspend fun registrarRetirada(id: Long, agora: Long)

    /** Grava a comprovação da entrega: foto, coordenadas e endereço. */
    @Query(
        """
        UPDATE pedidos SET
            status = :status,
            fotoEntregaPath = :fotoPath,
            latitudeEntregue = :latitude,
            longitudeEntregue = :longitude,
            enderecoEntregue = :endereco,
            entregueEm = :entregueEm,
            updatedAt = :updatedAt,
            pendingSync = 1
        WHERE id = :id
        """
    )
    suspend fun registrarEntrega(
        id: Long,
        status: String,
        fotoPath: String?,
        latitude: Double?,
        longitude: Double?,
        endereco: String?,
        entregueEm: Long,
        updatedAt: Long
    )

    /** Entregas registradas sem conexão: já têm coordenadas, falta o endereço. */
    @Query("SELECT * FROM pedidos WHERE status = 'ENTREGUE' AND latitudeEntregue IS NOT NULL AND longitudeEntregue IS NOT NULL AND (enderecoEntregue IS NULL OR enderecoEntregue = '') AND pendingDelete = 0")
    suspend fun getEntreguesSemEndereco(): List<Pedido>

    @Query("UPDATE pedidos SET enderecoEntregue = :endereco, updatedAt = :updatedAt, pendingSync = 1 WHERE id = :id")
    suspend fun atualizarEnderecoEntregue(id: Long, endereco: String, updatedAt: Long)

    /**
     * Leva o ponto de entrega do comprador para os pedidos dele que ainda não
     * saíram para entrega. Depois da retirada o destino só muda se o pedido
     * estava sem ponto ou se é o mesmo ponto recebendo o endereço.
     *
     * O comprador é achado pelo id do Firestore. Pedidos antigos, sem ele, usam o
     * id local junto com o nome, que evita alterar o pedido de outro comprador
     * sincronizado de outro aparelho.
     */
    @Query(
        """
        UPDATE pedidos SET
            enderecoEntrega = :endereco,
            latitudeEntrega = :latitude,
            longitudeEntrega = :longitude,
            updatedAt = :updatedAt,
            pendingSync = 1
        WHERE (
                compradorFirestoreId = :compradorFirestoreId
                OR (compradorFirestoreId IS NULL AND compradorId = :compradorId AND compradorNome = :compradorNome)
            )
            AND pendingDelete = 0
            AND (
                status = 'AGUARDANDO'
                OR (status = 'EM_TRANSPORTE' AND (
                    latitudeEntrega IS NULL
                    OR (latitudeEntrega = :latitude AND longitudeEntrega = :longitude)
                ))
            )
        """
    )
    suspend fun atualizarDestinoEmAberto(
        compradorFirestoreId: String?,
        compradorId: Long,
        compradorNome: String,
        endereco: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long
    )

    /**
     * Leva o endereço do vendedor para as vendas dele que ainda não foram retiradas.
     * O vendedor é achado como o comprador no destino: id do Firestore, ou id local e nome.
     */
    @Query(
        """
        UPDATE pedidos SET
            enderecoRetirada = :endereco,
            latitudeRetirada = :latitude,
            longitudeRetirada = :longitude,
            updatedAt = :updatedAt,
            pendingSync = 1
        WHERE (
                vendedorFirestoreId = :vendedorFirestoreId
                OR (vendedorFirestoreId IS NULL AND vendedorId = :vendedorId AND vendedorNome = :vendedorNome)
            )
            AND pendingDelete = 0
            AND status = 'AGUARDANDO'
        """
    )
    suspend fun atualizarRetiradaEmAberto(
        vendedorFirestoreId: String?,
        vendedorId: Long,
        vendedorNome: String,
        endereco: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long
    )

    /** Só libera a pendência se o pedido não mudou enquanto era enviado à nuvem. */
    @Query("UPDATE pedidos SET firestoreId = :firestoreId, pendingSync = CASE WHEN updatedAt = :updatedAt THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun marcarSincronizado(id: Long, firestoreId: String, updatedAt: Long)

    /** Guarda os ids do Firestore descobertos no envio, sem marcar o pedido como alterado. */
    @Query("UPDATE pedidos SET produtoFirestoreId = :produto, compradorFirestoreId = :comprador, vendedorFirestoreId = :vendedor WHERE id = :id")
    suspend fun definirVinculos(id: Long, produto: String?, comprador: String?, vendedor: String?)

    @Query(
        """
        UPDATE pedidos SET produtoId =
            (SELECT p.id FROM produtos p WHERE p.firestoreId = pedidos.produtoFirestoreId)
        WHERE produtoFirestoreId IS NOT NULL
            AND produtoId IS NOT
                (SELECT p.id FROM produtos p WHERE p.firestoreId = pedidos.produtoFirestoreId)
            AND EXISTS (SELECT 1 FROM produtos p WHERE p.firestoreId = pedidos.produtoFirestoreId)
        """
    )
    suspend fun religarProdutos()

    @Query(
        """
        UPDATE pedidos SET compradorId =
            (SELECT n.id FROM negociantes n WHERE n.firestoreId = pedidos.compradorFirestoreId)
        WHERE compradorFirestoreId IS NOT NULL
            AND compradorId IS NOT
                (SELECT n.id FROM negociantes n WHERE n.firestoreId = pedidos.compradorFirestoreId)
            AND EXISTS (SELECT 1 FROM negociantes n WHERE n.firestoreId = pedidos.compradorFirestoreId)
        """
    )
    suspend fun religarCompradores()

    @Query(
        """
        UPDATE pedidos SET vendedorId =
            (SELECT n.id FROM negociantes n WHERE n.firestoreId = pedidos.vendedorFirestoreId)
        WHERE vendedorFirestoreId IS NOT NULL
            AND vendedorId IS NOT
                (SELECT n.id FROM negociantes n WHERE n.firestoreId = pedidos.vendedorFirestoreId)
            AND EXISTS (SELECT 1 FROM negociantes n WHERE n.firestoreId = pedidos.vendedorFirestoreId)
        """
    )
    suspend fun religarVendedores()

    /**
     * Pedidos de antes dos ids do Firestore: o nome copiado na compra aponta o
     * comprador, desde que só um negociante tenha esse nome.
     */
    @Query(
        """
        UPDATE pedidos SET compradorId =
            (SELECT n.id FROM negociantes n WHERE n.nome = pedidos.compradorNome)
        WHERE compradorFirestoreId IS NULL
            AND (SELECT COUNT(*) FROM negociantes n WHERE n.nome = pedidos.compradorNome) = 1
            AND compradorId IS NOT (SELECT n.id FROM negociantes n WHERE n.nome = pedidos.compradorNome)
        """
    )
    suspend fun religarCompradoresPeloNome()

    @Query(
        """
        UPDATE pedidos SET vendedorId =
            (SELECT n.id FROM negociantes n WHERE n.nome = pedidos.vendedorNome)
        WHERE vendedorFirestoreId IS NULL
            AND vendedorNome != ''
            AND (SELECT COUNT(*) FROM negociantes n WHERE n.nome = pedidos.vendedorNome) = 1
            AND vendedorId IS NOT (SELECT n.id FROM negociantes n WHERE n.nome = pedidos.vendedorNome)
        """
    )
    suspend fun religarVendedoresPeloNome()

    @Query(
        """
        UPDATE pedidos SET produtoId =
            (SELECT p.id FROM produtos p WHERE p.nome = pedidos.produtoNome)
        WHERE produtoFirestoreId IS NULL
            AND (SELECT COUNT(*) FROM produtos p WHERE p.nome = pedidos.produtoNome) = 1
            AND produtoId IS NOT (SELECT p.id FROM produtos p WHERE p.nome = pedidos.produtoNome)
        """
    )
    suspend fun religarProdutosPeloNome()
}
