package com.daytodo.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 小部件提供者。
 *
 * onUpdate 必须在返回之前就把内容更新好。小米桌面对首次加载有超时限制，
 * 如果只是丢一个后台任务就返回，很容易出现"载入窗口小部件时出现问题"。
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetScheduler.scheduleMidnight(context)
        updateNow(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        WidgetScheduler.scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateNow(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun updateNow(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        try {
            runBlocking {
                withTimeoutOrNull(TIMEOUT_MS) {
                    WidgetUpdater.update(appContext, appWidgetManager, appWidgetIds)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "update widget failed", t)
        }
    }

    companion object {
        private const val TAG = "DayTodoWidget"
        private const val TIMEOUT_MS = 8000L
    }
}
