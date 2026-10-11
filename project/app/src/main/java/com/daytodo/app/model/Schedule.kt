package com.daytodo.app.model

import com.daytodo.app.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Schedule {

    private const val WEEK_CN = "一二三四五六日"
    private val md = DateTimeFormatter.ofPattern("M月d日")

    fun occursOn(task: Task, date: LocalDate): Boolean {
        val epoch = date.toEpochDay()
        if (epoch < task.startDate) return false
        task.endDate?.let { if (epoch > it) return false }

        // 整段一次性型：区间内每一天都算“这件事”
        if (task.kind == Kind.SPAN_ONCE) return true

        return when (task.rule) {
            Rule.ONCE -> epoch == task.startDate
            Rule.DAILY -> true
            Rule.WEEKDAYS -> ((task.weekdaysMask shr (date.dayOfWeek.value - 1)) and 1) == 1
            Rule.EVERY_N_DAYS -> {
                val n = task.intervalDays.coerceAtLeast(1).toLong()
                (epoch - task.startDate) % n == 0L
            }
            Rule.MONTHLY -> date.dayOfMonth == task.monthDay
            Rule.WORKDAY -> date.dayOfWeek.value <= 5
            Rule.WEEKEND -> date.dayOfWeek.value >= 6
            else -> false
        }
    }

    fun weekdayLabel(mask: Int): String {
        val sb = StringBuilder()
        for (i in 0..6) {
            if (((mask shr i) and 1) == 1) {
                if (sb.isNotEmpty()) sb.append("、")
                sb.append(WEEK_CN[i])
            }
        }
        return if (sb.isEmpty()) "—" else sb.toString()
    }

    fun describe(task: Task): String {
        val start = LocalDate.ofEpochDay(task.startDate)
        val end = task.endDate?.let { LocalDate.ofEpochDay(it) }
        val sb = StringBuilder()

        if (task.kind == Kind.SPAN_ONCE) {
            val days = if (end != null) ChronoUnit.DAYS.between(start, end) + 1 else 1L
            sb.append("连续 ").append(days).append(" 天，完成一次即结束")
            if (days > 1) sb.append(" · ").append(md.format(start)).append(" 起")
            return sb.toString()
        }

        when (task.rule) {
            Rule.ONCE -> sb.append(md.format(start))
            Rule.DAILY -> sb.append("每天")
            Rule.WEEKDAYS -> sb.append("每周").append(weekdayLabel(task.weekdaysMask))
            Rule.EVERY_N_DAYS -> sb.append("每 ").append(task.intervalDays).append(" 天")
            Rule.MONTHLY -> sb.append("每月 ").append(task.monthDay).append(" 日")
            Rule.WORKDAY -> sb.append("工作日")
            Rule.WEEKEND -> sb.append("周末")
        }

        if (task.rule != Rule.ONCE) {
            sb.append(" · ").append(md.format(start)).append(" 起")
            if (end != null) {
                sb.append(" 至 ").append(md.format(end))
            }
        }
        return sb.toString()
    }
}
