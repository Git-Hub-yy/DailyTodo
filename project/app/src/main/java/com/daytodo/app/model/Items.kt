package com.daytodo.app.model

import com.daytodo.app.data.Task

data class DayItem(
    val task: Task,
    val completed: Boolean,
    val completedAt: Long = 0L
)

data class HistoryEntry(
    val task: Task,
    val date: Long,
    val completedAt: Long
)
