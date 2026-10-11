package com.daytodo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.daytodo.app.R
import com.daytodo.app.app
import com.daytodo.app.prefs.Prefs
import com.daytodo.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object WidgetUpdater {

    private const val TAG = "DayTodoWidget"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

    private val rowIds = intArrayOf(
        R.id.widgetRow1, R.id.widgetRow2, R.id.widgetRow3, R.id.widgetRow4, R.id.widgetRow5
    )
    private val markIds = intArrayOf(
        R.id.widgetMark1, R.id.widgetMark2, R.id.widgetMark3, R.id.widgetMark4, R.id.widgetMark5
    )
    private val textIds = intArrayOf(
        R.id.widgetText1, R.id.widgetText2, R.id.widgetText3, R.id.widgetText4, R.id.widgetText5
    )

    /** 供 App 内部调用：后台刷新，不阻塞界面 */
    fun refresh(context: Context) {
        val ctx = context.applicationContext
        scope.launch {
            try {
                updateAll(ctx)
            } catch (t: Throwable) {
                Log.e(TAG, "refresh failed", t)
            }
        }
    }

    /** 刷新所有小部件 */
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, TodayWidgetProvider::class.java)
        )
        update(context, manager, ids)
    }

    suspend fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val views = try {
                build(context, manager, id)
            } catch (t: Throwable) {
                Log.e(TAG, "build widget views failed id=$id", t)
                fallback(context)
            }
            try {
                manager.updateAppWidget(id, views)
            } catch (t: Throwable) {
                Log.e(TAG, "updateAppWidget failed id=$id", t)
            }
        }
    }

    // 万一正常内容构建失败，也要送一份最简单的界面过去，
    // 这样桌面上显示的是提示文字，而不是系统的"载入小部件出现问题"。
    private fun fallback(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setTextViewText(R.id.widgetDate, dateFormat.format(LocalDate.now()))
        views.setTextViewText(R.id.widgetCount, "")
        views.setTextViewText(R.id.widgetEmpty, context.getString(R.string.widget_loading))
        views.setViewVisibility(R.id.widgetEmpty, View.VISIBLE)
        views.setViewVisibility(R.id.widgetBg, View.GONE)
        views.setInt(R.id.widgetDim, "setBackgroundColor", Color.argb(210, 0, 0, 0))
        for (i in rowIds.indices) views.setViewVisibility(rowIds[i], View.GONE)
        views.setViewVisibility(R.id.widgetMore, View.GONE)
        views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent(context))
        return views
    }

    private suspend fun build(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ): RemoteViews {
        val prefs = Prefs(context)
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val today = LocalDate.now()
        val items = context.app().repo.itemsFor(today)
        val total = items.size
        val pending = items.count { !it.completed }

        val bgPath = prefs.widgetBgPath
        val bgFile = bgPath?.let { File(it) }
        val bitmap = if (bgFile != null && bgFile.exists()) {
            loadBackgroundBitmap(context, bgFile, manager, widgetId)
        } else {
            null
        }
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widgetBg, bitmap)
            views.setViewVisibility(R.id.widgetBg, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widgetBg, View.GONE)
        }

        val dim = (prefs.widgetDim.coerceIn(0, 100) * 255) / 100
        views.setInt(R.id.widgetDim, "setBackgroundColor", Color.argb(dim, 0, 0, 0))

        val darkText = when (prefs.widgetTextColor) {
            Prefs.TEXT_LIGHT -> false
            Prefs.TEXT_DARK -> true
            else -> prefs.widgetDim < 45
        }
        val textColor = if (darkText) DARK else Color.WHITE
        views.setTextColor(R.id.widgetDate, textColor)
        views.setTextColor(R.id.widgetCount, withAlpha(textColor, 0.80f))
        views.setTextColor(R.id.widgetEmpty, withAlpha(textColor, 0.80f))
        views.setTextColor(R.id.widgetMore, withAlpha(textColor, 0.70f))
        for (id in textIds) views.setTextColor(id, textColor)
        for (id in markIds) views.setTextColor(id, textColor)

        views.setTextViewText(R.id.widgetDate, dateFormat.format(today))
        views.setTextViewText(
            R.id.widgetCount,
            context.getString(R.string.progress_format, pending, total)
        )

        val options = manager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val maxRows = ((minHeight - 62) / 26).coerceIn(1, rowIds.size)

        val doneItems = if (prefs.widgetShowDone) items.filter { it.completed } else emptyList()
        val display = items.filter { !it.completed } + doneItems

        views.setViewVisibility(
            R.id.widgetEmpty,
            if (display.isEmpty()) View.VISIBLE else View.GONE
        )

        for (index in rowIds.indices) {
            if (index >= maxRows || index >= display.size) {
                views.setViewVisibility(rowIds[index], View.GONE)
                continue
            }
            val item = display[index]
            views.setViewVisibility(rowIds[index], View.VISIBLE)
            views.setTextViewText(textIds[index], item.task.title)
            views.setTextViewText(markIds[index], if (item.completed) "●" else "○")
            views.setTextColor(
                textIds[index],
                if (item.completed) withAlpha(textColor, 0.45f) else textColor
            )
            views.setTextColor(
                markIds[index],
                if (item.completed) withAlpha(textColor, 0.45f) else textColor
            )
            views.setOnClickPendingIntent(
                rowIds[index],
                togglePendingIntent(context, item.task.id, today.toEpochDay(), index)
            )
        }

        val shown = minOf(display.size, maxRows, rowIds.size)
        val hidden = display.size - shown
        if (hidden > 0) {
            views.setViewVisibility(R.id.widgetMore, View.VISIBLE)
            views.setTextViewText(R.id.widgetMore, context.getString(R.string.widget_more, hidden))
        } else {
            views.setViewVisibility(R.id.widgetMore, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.widgetRoot, openAppPendingIntent(context))
        return views
    }

    // 按小部件的实际像素尺寸缩放背景图。小部件的数据要经 Binder 传递，
    // 图片过大会直接导致小部件刷新失败，所以既按尺寸缩放，也限制总像素数。
    private fun loadBackgroundBitmap(
        context: Context,
        file: File,
        manager: AppWidgetManager,
        widgetId: Int
    ): Bitmap? {
        val options = manager.getAppWidgetOptions(widgetId)
        val density = context.resources.displayMetrics.density
        var maxWidth =
            (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 250) * density).toInt()
        var maxHeight =
            (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110) * density).toInt()
        if (maxWidth <= 0) maxWidth = 700
        if (maxHeight <= 0) maxHeight = 320

        val maxPixels = 150_000
        val shrink = kotlin.math.sqrt((maxWidth.toLong() * maxHeight) / maxPixels.toDouble())
        if (shrink > 1.0) {
            maxWidth = (maxWidth / shrink).toInt().coerceAtLeast(120)
            maxHeight = (maxHeight / shrink).toInt().coerceAtLeast(80)
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth &&
            bounds.outHeight / (sample * 2) >= maxHeight
        ) {
            sample *= 2
        }

        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null

        val scaled = Bitmap.createScaledBitmap(decoded, maxWidth, maxHeight, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun togglePendingIntent(
        context: Context,
        taskId: Long,
        epoch: Long,
        row: Int
    ): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_TOGGLE
            data = Uri.parse("daytodo://toggle/$taskId/$epoch")
            putExtra(WidgetActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(WidgetActionReceiver.EXTRA_DATE, epoch)
        }
        return PendingIntent.getBroadcast(
            context,
            row,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun withAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private val DARK = 0xFF1B1D23.toInt()
}
