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
}
