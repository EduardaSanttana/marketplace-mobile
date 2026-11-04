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

    @Query("SELECT * FROM pedidos ORDER BY id DESC")
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

    @Query("UPDATE pedidos SET status = :status, updatedAt = :updatedAt, pendingSync = 1 WHERE id = :id")
    suspend fun atualizarStatus(id: Long, status: String, updatedAt: Long)

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
     * O id do comprador é local de cada aparelho; o nome evita alterar o pedido
     * de outro comprador que tenha sido sincronizado de outro dispositivo.
     */
    @Query(
        """
        UPDATE pedidos SET
            enderecoEntrega = :endereco,
            latitudeEntrega = :latitude,
            longitudeEntrega = :longitude,
            updatedAt = :updatedAt,
            pendingSync = 1
        WHERE compradorId = :compradorId
            AND compradorNome = :compradorNome
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
        compradorId: Long,
        compradorNome: String,
        endereco: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long
    )

    /** Só libera a pendência se o pedido não mudou enquanto era enviado à nuvem. */
    @Query("UPDATE pedidos SET firestoreId = :firestoreId, pendingSync = CASE WHEN updatedAt = :updatedAt THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun marcarSincronizado(id: Long, firestoreId: String, updatedAt: Long)
}
