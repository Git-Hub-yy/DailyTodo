package com.daytodo.app.model

import com.daytodo.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleTest {

    private fun task(
        kind: Int = Kind.DAILY_RESET,
        rule: Int = Rule.DAILY,
        start: LocalDate,
        end: LocalDate? = null,
        mask: Int = 0b0011111,
        interval: Int = 2,
        monthDay: Int = 1
    ) = Task(
        id = 1L,
        title = "测试",
        kind = kind,
        rule = rule,
        weekdaysMask = mask,
        intervalDays = interval,
        monthDay = monthDay,
        startDate = start.toEpochDay(),
        endDate = end?.toEpochDay()
    )

    // ---------- 期限规则 ----------

    @Test
    fun dailyOccursEveryDayFromStart() {
        val start = LocalDate.of(2026, 10, 11)
        val t = task(rule = Rule.DAILY, start = start)

        assertFalse(Schedule.occursOn(t, start.minusDays(1)))
        assertTrue(Schedule.occursOn(t, start))
        assertTrue(Schedule.occursOn(t, start.plusDays(1)))
        assertTrue(Schedule.occursOn(t, start.plusDays(400)))
    }

    @Test
    fun dailyStopsAtEndDate() {
        val start = LocalDate.of(2026, 10, 11)
        val end = LocalDate.of(2026, 10, 13)
        val t = task(rule = Rule.DAILY, start = start, end = end)

        assertTrue(Schedule.occursOn(t, end))
        assertFalse(Schedule.occursOn(t, end.plusDays(1)))
    }

    @Test
    fun weekdayMaskMatchesMondayWednesdayFriday() {
        val start = LocalDate.of(2026, 10, 1)
        val mask = 0b0010101 // 周一、周三、周五
        val t = task(rule = Rule.WEEKDAYS, start = start, mask = mask)

        var day = start
        var visited = 0
        while (day.isBefore(start.plusDays(14))) {
            val expected = day.dayOfWeek.value == 1 ||
                day.dayOfWeek.value == 3 ||
                day.dayOfWeek.value == 5
            assertEquals("$day", expected, Schedule.occursOn(t, day))
            visited++
            day = day.plusDays(1)
        }
        assertEquals(14, visited)
    }

    @Test
    fun everyNDaysCountsFromStart() {
        val start = LocalDate.of(2026, 10, 1)
        val t = task(rule = Rule.EVERY_N_DAYS, start = start, interval = 3)

        assertTrue(Schedule.occursOn(t, start))
        assertFalse(Schedule.occursOn(t, start.plusDays(1)))
        assertFalse(Schedule.occursOn(t, start.plusDays(2)))
        assertTrue(Schedule.occursOn(t, start.plusDays(3)))
        assertTrue(Schedule.occursOn(t, start.plusDays(30)))
    }

    @Test
    fun monthlyMatchesSelectedDayOfMonth() {
        val start = LocalDate.of(2026, 10, 1)
        val t = task(rule = Rule.MONTHLY, start = start, monthDay = 15)

        assertTrue(Schedule.occursOn(t, LocalDate.of(2026, 10, 15)))
        assertFalse(Schedule.occursOn(t, LocalDate.of(2026, 10, 16)))
        assertTrue(Schedule.occursOn(t, LocalDate.of(2026, 11, 15)))
    }

    @Test
    fun workdayAndWeekendRules() {
        val start = LocalDate.of(2026, 10, 1)
        val workday = task(rule = Rule.WORKDAY, start = start)
        val weekend = task(rule = Rule.WEEKEND, start = start)

        var day = start
        while (day.isBefore(start.plusDays(10))) {
            val isWeekend = day.dayOfWeek.value >= 6
            assertEquals("$day", !isWeekend, Schedule.occursOn(workday, day))
            assertEquals("$day", isWeekend, Schedule.occursOn(weekend, day))
            day = day.plusDays(1)
        }
    }

    @Test
    fun onceOnlyMatchesSingleDay() {
        val start = LocalDate.of(2026, 10, 11)
        val t = task(rule = Rule.ONCE, start = start)

        assertTrue(Schedule.occursOn(t, start))
        assertFalse(Schedule.occursOn(t, start.plusDays(1)))
    }

    // ---------- 跨天任务 ----------

    @Test
    fun spanTaskCoversEveryDayInRange() {
        val start = LocalDate.of(2026, 10, 11)
        val end = LocalDate.of(2026, 10, 12)
        val t = task(kind = Kind.SPAN_ONCE, rule = Rule.ONCE, start = start, end = end)

        assertFalse(Schedule.occursOn(t, start.minusDays(1)))
        assertTrue(Schedule.occursOn(t, start))
        assertTrue(Schedule.occursOn(t, end))
        assertFalse(Schedule.occursOn(t, end.plusDays(1)))
    }

    // ---------- 两种完成逻辑（需求核心） ----------

    @Test
    fun dailyTaskResetsNextDay() {
        val day1 = LocalDate.of(2026, 10, 11)
        val day2 = day1.plusDays(1)
        val t = task(kind = Kind.DAILY_RESET, rule = Rule.DAILY, start = day1)

        // 第一天完成之前是未完成
        assertEquals(Visibility.PENDING, visibilityOn(t, day1, emptySet()))

        // 第一天打勾 → 当天显示为已完成
        assertEquals(Visibility.DONE, visibilityOn(t, day1, setOf(day1.toEpochDay())))

        // 第二天自动恢复未完成
        assertEquals(Visibility.PENDING, visibilityOn(t, day2, setOf(day1.toEpochDay())))
    }

    @Test
    fun spanTaskDisappearsAfterDoneOnFirstDay() {
        val day1 = LocalDate.of(2026, 10, 11)
        val day2 = day1.plusDays(1)
        val t = task(
            kind = Kind.SPAN_ONCE,
            rule = Rule.ONCE,
            start = day1,
            end = day2
        )

        // 两天都没完成时，两天都要显示
        assertEquals(Visibility.PENDING, visibilityOn(t, day1, emptySet()))
        assertEquals(Visibility.PENDING, visibilityOn(t, day2, emptySet()))

        // 第一天就完成 → 第二天不再出现
        assertEquals(Visibility.DONE, visibilityOn(t, day1, setOf(day1.toEpochDay())))
        assertEquals(Visibility.HIDDEN, visibilityOn(t, day2, setOf(day1.toEpochDay())))

        // 第二天才完成 → 第二天显示为完成
        assertEquals(Visibility.DONE, visibilityOn(t, day2, setOf(day2.toEpochDay())))
    }
}
