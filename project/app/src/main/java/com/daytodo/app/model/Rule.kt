package com.daytodo.app.model

object Rule {
    const val ONCE = 0
    const val DAILY = 1
    const val WEEKDAYS = 2
    const val EVERY_N_DAYS = 3
    const val MONTHLY = 4
    const val WORKDAY = 5
    const val WEEKEND = 6

    val names = arrayOf(
        "仅这一天",
        "每天",
        "指定星期",
        "每隔 N 天",
        "每月某日",
        "工作日（周一到周五）",
        "周末（周六、周日）"
    )
}

object Kind {
    /** 每日打卡型：今天完成，明天恢复未完成 */
    const val DAILY_RESET = 0

    /** 整段一次性型：连续多天算一件事，完成一次即结束 */
    const val SPAN_ONCE = 1
}
