package com.daytodo.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/**
 * 跨零点、系统时间变化、开机、解锁屏幕之后刷新小部件。
 *
 * 这里必须**在返回之前**把刷新做完：如果只是丢一个后台任务就返回，
 * 系统（尤其是小米这类省电激进的系统）可能在任务跑完之前就把进程挂起，
 * 结果就是小部件停在昨天没更新。
 */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        try {
            runBlocking {
                withTimeoutOrNull(TIMEOUT_MS) {
                    WidgetUpdater.updateAll(appContext)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "refresh widget failed", t)
        }

        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> WidgetScheduler.scheduleMidnight(appContext)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.daytodo.app.action.REFRESH"
        private const val TAG = "DayTodoWidget"
        private const val TIMEOUT_MS = 8000L
    }
}

object WidgetScheduler {

    private const val REQUEST_CODE = 100

    fun scheduleMidnight(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 20)
            set(Calendar.MILLISECOND, 0)
        }
        val intent = Intent(context, WidgetRefreshReceiver::class.java)
            .setAction(WidgetRefreshReceiver.ACTION_REFRESH)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = calendar.timeInMillis
        try {
            // 系统允许用精确闹钟就用精确的，保证零点准时刷新；
            // 不允许（Android 12+ 需要单独授权）就退回系统的"允许在待机时触发"模式。
            if (canUseExactAlarm(manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (t: Throwable) {
            Log.e("DayTodoWidget", "schedule midnight alarm failed", t)
            runCatching { manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending) }
        }
    }

    private fun canUseExactAlarm(manager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager.canScheduleExactAlarms() }.getOrDefault(false)
        } else {
            true
        }
    }
}
