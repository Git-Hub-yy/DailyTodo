package com.daytodo.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.daytodo.app.app
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/** 在小部件上直接点一下勾选 / 取消勾选 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val epoch = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        if (taskId <= 0L || epoch == Long.MIN_VALUE) return

        val appContext = context.applicationContext
        val pending = goAsync()
        try {
            // 同样要在返回之前做完：先改数据，再刷新小部件
            runBlocking {
                withTimeoutOrNull(TIMEOUT_MS) {
                    val task = appContext.app().database.taskDao().byId(taskId)
                    if (task != null) {
                        appContext.app().repo.toggle(task, LocalDate.ofEpochDay(epoch))
                    }
                    WidgetUpdater.updateAll(appContext)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "toggle from widget failed", t)
        } finally {
            pending.finish()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.daytodo.app.action.TOGGLE"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_DATE = "date"
        private const val TAG = "DayTodoWidget"
        private const val TIMEOUT_MS = 8000L
    }
}
