package com.example.aluno_bruno.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AvaliacaoDao {
    @Insert
    suspend fun insert(avaliacao: Avaliacao): Long

    @Update
    suspend fun update(avaliacao: Avaliacao)

    @Delete
    suspend fun delete(avaliacao: Avaliacao)

    @Query("SELECT * FROM avaliacoes ORDER BY id DESC")
    fun getAll(): Flow<List<Avaliacao>>

    @Query("SELECT * FROM avaliacoes WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<Avaliacao>

    @Query("SELECT * FROM avaliacoes WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<Avaliacao>

    @Query("SELECT * FROM avaliacoes WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): Avaliacao?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromRemote(avaliacao: Avaliacao): Long

    @Query("DELETE FROM avaliacoes WHERE id = :id")
    suspend fun hardDeleteById(id: Long)

    @Query("DELETE FROM avaliacoes WHERE firestoreId = :firestoreId")
    suspend fun hardDeleteByFirestoreId(firestoreId: String)

    @Query("UPDATE avaliacoes SET pendingDelete = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)
}
