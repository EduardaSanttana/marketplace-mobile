package com.example.aluno_melissaa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocianteDao {
    @Insert
    suspend fun insert(negociante: Negociante): Long

    @Update
    suspend fun update(negociante: Negociante)

    @Delete
    suspend fun delete(negociante: Negociante)

    @Query("SELECT * FROM negociantes ORDER BY id DESC")
    fun getAll(): Flow<List<Negociante>>

    @Query("SELECT * FROM negociantes WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<Negociante>

    @Query("SELECT * FROM negociantes WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<Negociante>

    @Query("SELECT * FROM negociantes WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): Negociante?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromRemote(negociante: Negociante): Long

    @Query("DELETE FROM negociantes WHERE id = :id")
    suspend fun hardDeleteById(id: Long)

    @Query("DELETE FROM negociantes WHERE firestoreId = :firestoreId")
    suspend fun hardDeleteByFirestoreId(firestoreId: String)

    @Query("UPDATE negociantes SET pendingDelete = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)
}
