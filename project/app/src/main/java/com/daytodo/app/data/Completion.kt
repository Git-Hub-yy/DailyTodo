package com.daytodo.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index(value = ["taskId", "date"], unique = true)]
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val date: Long,
    val completedAt: Long = System.currentTimeMillis()
)
