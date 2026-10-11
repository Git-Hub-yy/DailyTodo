package com.daytodo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * kind = 0 每日打卡型：完成状态按天计算，今天完成明天恢复未完成
 * kind = 1 整段一次性型：startDate..endDate 算一件事，任意一天完成即结束
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String = "",
    val kind: Int = 0,
    val rule: Int = 1,
    val weekdaysMask: Int = 0b0011111,
    val intervalDays: Int = 2,
    val monthDay: Int = 1,
    val startDate: Long,
    val endDate: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
