package edu.ifsp.marketplace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {
    @Insert
    suspend fun insert(produto: Produto): Long

    @Update
    suspend fun update(produto: Produto)

    @Delete
    suspend fun delete(produto: Produto)

    @Query("SELECT * FROM produtos ORDER BY id DESC")
    fun getAll(): Flow<List<Produto>>

    @Query("SELECT * FROM produtos WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<Produto>

    @Query("SELECT * FROM produtos WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<Produto>

    @Query("SELECT * FROM produtos WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): Produto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromRemote(produto: Produto): Long

    @Query("DELETE FROM produtos WHERE id = :id")
    suspend fun hardDeleteById(id: Long)

    @Query("DELETE FROM produtos WHERE firestoreId = :firestoreId")
    suspend fun hardDeleteByFirestoreId(firestoreId: String)

    @Query("UPDATE produtos SET pendingDelete = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)
}
