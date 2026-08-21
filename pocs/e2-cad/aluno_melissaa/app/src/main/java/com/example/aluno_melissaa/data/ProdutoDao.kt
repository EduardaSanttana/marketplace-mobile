package com.example.aluno_melissaa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {
    @Insert
    suspend fun insert(produto: Produto): Long

    @Delete
    suspend fun delete(produto: Produto)

    @Query("SELECT * FROM produtos ORDER BY id DESC")
    fun getAll(): Flow<List<Produto>>
}
