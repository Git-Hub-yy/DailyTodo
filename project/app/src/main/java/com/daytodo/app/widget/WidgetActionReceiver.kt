package com.daytodo.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daytodo.app.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 小部件上直接点一下勾选 / 取消勾选 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val epoch = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        if (taskId <= 0L || epoch == Long.MIN_VALUE) return

        val application = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val task = application.app().database.taskDao().byId(taskId)
                if (task != null) {
                    application.app().repo.toggle(task, LocalDate.ofEpochDay(epoch))
                }
            } finally {
                WidgetUpdater.refresh(application)
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.daytodo.app.action.TOGGLE"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_DATE = "date"
    }
}
