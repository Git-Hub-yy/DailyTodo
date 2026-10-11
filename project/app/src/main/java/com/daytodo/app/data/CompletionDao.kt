package com.daytodo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CompletionDao {

    @Query("SELECT * FROM completions")
    suspend fun all(): List<Completion>

    @Query("SELECT * FROM completions WHERE taskId = :taskId")
    suspend fun forTask(taskId: Long): List<Completion>

    @Query("SELECT * FROM completions WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun find(taskId: Long, date: Long): Completion?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: Completion): Long

    @Query("DELETE FROM completions WHERE taskId = :taskId AND date = :date")
    suspend fun delete(taskId: Long, date: Long)
}
