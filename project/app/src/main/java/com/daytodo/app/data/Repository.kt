package com.daytodo.app.data

import com.daytodo.app.model.DayItem
import com.daytodo.app.model.HistoryEntry
import com.daytodo.app.model.Kind
import com.daytodo.app.model.Schedule
import com.daytodo.app.model.Visibility
import com.daytodo.app.model.visibilityOn
import java.time.LocalDate

class Repository(private val db: AppDatabase) {

    private val tasks = db.taskDao()
    private val completions = db.completionDao()

    suspend fun allTasks(): List<Task> = tasks.all()

    suspend fun taskById(id: Long): Task? = tasks.byId(id)

    suspend fun itemsFor(date: LocalDate): List<DayItem> {
        val epoch = date.toEpochDay()
        val allTasks = tasks.all()
        val allCompletions = completions.all()

        val datesByTask = HashMap<Long, MutableSet<Long>>()
        val completedAtToday = HashMap<Long, Long>()
        for (c in allCompletions) {
            datesByTask.getOrPut(c.taskId) { HashSet() }.add(c.date)
            if (c.date == epoch) {
                val prev = completedAtToday[c.taskId]
                if (prev == null || c.completedAt < prev) completedAtToday[c.taskId] = c.completedAt
            }
        }

        val pending = ArrayList<DayItem>()
        val done = ArrayList<DayItem>()

        for (t in allTasks) {
            if (!Schedule.occursOn(t, date)) continue
            val completionDates = datesByTask[t.id] ?: emptySet()
            when (visibilityOn(t, date, completionDates)) {
                Visibility.PENDING -> pending.add(DayItem(t, false))
                Visibility.DONE -> done.add(
                    DayItem(t, true, completedAtToday[t.id] ?: 0L)
                )
                Visibility.HIDDEN -> {
                    // 整段一次性型任务完成过之后，剩下的日子不再出现
                }
            }
        }

        val sortedPending = pending.sortedWith(compareBy({ it.task.sortOrder }, { it.task.id }))
        val sortedDone = done.sortedBy { it.completedAt }
        return sortedPending + sortedDone
    }

    /** 每一天还有多少件没完成，用于日期条上的小圆点 */
    suspend fun pendingCounts(dates: List<LocalDate>): Map<Long, Int> {
        val allTasks = tasks.all()
        val allCompletions = completions.all()
        val datesByTask = HashMap<Long, MutableSet<Long>>()
        for (c in allCompletions) {
            datesByTask.getOrPut(c.taskId) { HashSet() }.add(c.date)
        }

        val result = HashMap<Long, Int>(dates.size)
        for (d in dates) {
            var count = 0
            for (t in allTasks) {
                if (!Schedule.occursOn(t, d)) continue
                if (visibilityOn(t, d, datesByTask[t.id] ?: emptySet()) == Visibility.PENDING) {
                    count++
                }
            }
            result[d.toEpochDay()] = count
        }
        return result
    }

    suspend fun toggle(task: Task, date: LocalDate) {
        val epoch = date.toEpochDay()
        if (task.kind == Kind.DAILY_RESET) {
            if (completions.find(task.id, epoch) != null) {
                completions.delete(task.id, epoch)
            } else {
                completions.insert(Completion(taskId = task.id, date = epoch))
            }
        } else {
            val existing = completions.forTask(task.id).minByOrNull { it.completedAt }
            if (existing != null) {
                completions.delete(task.id, existing.date)
            } else {
                completions.insert(Completion(taskId = task.id, date = epoch))
            }
        }
    }

    suspend fun save(task: Task): Long {
        return if (task.id == 0L) {
            val order = tasks.maxSortOrder() + 1
            tasks.insert(task.copy(sortOrder = order))
        } else {
            tasks.update(task)
            task.id
        }
    }

    suspend fun delete(task: Task) {
        tasks.deleteById(task.id)
    }

    suspend fun history(): List<HistoryEntry> {
        val taskById = tasks.all().associateBy { it.id }
        return completions.all()
            .mapNotNull { c ->
                taskById[c.taskId]?.let { HistoryEntry(it, c.date, c.completedAt) }
            }
            .sortedWith(compareByDescending<HistoryEntry> { it.date }.thenByDescending { it.completedAt })
    }

    /** 返回 (总完成次数, 有记录的天数) */
    suspend fun stats(): Pair<Int, Int> {
        val all = completions.all()
        return all.size to all.map { it.date }.toHashSet().size
    }
}
