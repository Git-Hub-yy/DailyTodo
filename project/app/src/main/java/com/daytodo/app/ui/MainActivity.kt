package com.daytodo.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daytodo.app.R
import com.daytodo.app.app
import com.daytodo.app.databinding.ActivityMainBinding
import com.daytodo.app.model.DayItem
import com.daytodo.app.widget.WidgetUpdater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var stripAdapter: DateStripAdapter
    private lateinit var taskAdapter: TaskAdapter
    private val dates = ArrayList<LocalDate>()
    private var selected: LocalDate = LocalDate.now()

    private val titleFormat = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { onMenuItem(it) }

        val today = LocalDate.now()
        for (offset in -60..365) {
            dates.add(today.plusDays(offset.toLong()))
        }

        stripAdapter = DateStripAdapter(dates, selected) { picked ->
            selected = picked
            refresh()
        }
        binding.dateStrip.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dateStrip.adapter = stripAdapter

        taskAdapter = TaskAdapter(
            onClick = { toggle(it) },
            onEdit = { startActivity(EditTaskActivity.editIntent(this, it.task.id)) },
            onDelete = { confirmDelete(it) }
        )
        binding.taskList.layoutManager = LinearLayoutManager(this)
        binding.taskList.adapter = taskAdapter

        binding.fab.setOnClickListener {
            startActivity(EditTaskActivity.newIntent(this, selected))
        }

        binding.dateStrip.scrollToPosition(60)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun onMenuItem(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_today -> {
            selected = LocalDate.now()
            scrollToSelected()
            refresh()
            true
        }
        R.id.action_history -> {
            startActivity(Intent(this, HistoryActivity::class.java))
            true
        }
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        else -> false
    }

    private fun scrollToSelected() {
        val pos = stripAdapter.positionOf(selected)
        if (pos >= 0) binding.dateStrip.scrollToPosition(pos)
    }

    private fun refresh() {
        binding.textDayTitle.text = titleOf(selected)
        lifecycleScope.launch {
            val repo = app().repo
            val items = repo.itemsFor(selected)
            val counts = repo.pendingCounts(dates)
            stripAdapter.setSelected(selected)
            stripAdapter.setCounts(counts)
            taskAdapter.submit(items)
            binding.textEmpty.isVisible = items.isEmpty()
            val pending = items.count { !it.completed }
            binding.textProgress.text = getString(R.string.progress_format, pending, items.size)
        }
    }

    private fun titleOf(date: LocalDate): String {
        val today = LocalDate.now()
        val base = titleFormat.format(date)
        return when (date) {
            today -> "今天 · $base"
            today.plusDays(1) -> "明天 · $base"
            today.minusDays(1) -> "昨天 · $base"
            else -> base
        }
    }

    private fun toggle(item: DayItem) {
        lifecycleScope.launch {
            app().repo.toggle(item.task, selected)
            WidgetUpdater.refresh(this@MainActivity)
            refresh()
        }
    }

    private fun confirmDelete(item: DayItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.task.title)
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    app().repo.delete(item.task)
                    WidgetUpdater.refresh(this@MainActivity)
                    refresh()
                }
            }
            .show()
    }
}
