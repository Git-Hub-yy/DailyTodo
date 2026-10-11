package com.daytodo.app.model

import com.daytodo.app.data.Task
import java.time.LocalDate

enum class Visibility {
    /** 这一天不显示这个任务 */
    HIDDEN,

    /** 显示，并且是未完成 */
    PENDING,

    /** 显示，并且已经完成（置底、变暗） */
    DONE
}

/**
 * 判断某个任务在某一天应该是什么状态。
 *
 * @param completionDates 这个任务所有的完成日期（epochDay）。整段一次性型任务最多只有一条。
 *
 * - 每日打卡型：只有“这一天本身”有完成记录才算完成，所以今天打勾，明天自动回到未完成。
 * - 整段一次性型：只要完成过（无论哪天），整件事就结束了；完成当天仍然显示为已完成，
 *   之后的日子直接不显示。
 */
fun visibilityOn(task: Task, date: LocalDate, completionDates: Set<Long>): Visibility {
    val epoch = date.toEpochDay()
    return if (task.kind == Kind.DAILY_RESET) {
        if (epoch in completionDates) Visibility.DONE else Visibility.PENDING
    } else {
        when {
            completionDates.isEmpty() -> Visibility.PENDING
            epoch in completionDates -> Visibility.DONE
            else -> Visibility.HIDDEN
        }
    }
}
