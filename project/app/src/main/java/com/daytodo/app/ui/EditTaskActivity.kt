package com.daytodo.app.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.daytodo.app.R
import com.daytodo.app.app
import com.daytodo.app.data.Task
import com.daytodo.app.databinding.ActivityEditBinding
import com.daytodo.app.model.Kind
import com.daytodo.app.model.Rule
import com.daytodo.app.widget.WidgetUpdater
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class EditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    private val chips = ArrayList<Chip>()

    private var taskId: Long = 0L
    private var existing: Task? = null

    private var kind = Kind.DAILY_RESET
    private var rule = Rule.DAILY
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now().plusDays(29)
    private var infinite = true
    private var intervalDays = 2
    private var monthDay = 1
    private var spanDays = 2

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val dateExtra = intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay())
        startDate = LocalDate.ofEpochDay(dateExtra)
        endDate = startDate.plusDays(29)

        binding.toolbar.title =
            getString(if (taskId > 0) R.string.title_edit_task else R.string.title_new_task)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnDelete.isVisible = taskId > 0

        chips.add(binding.chipW1)
        chips.add(binding.chipW2)
        chips.add(binding.chipW3)
        chips.add(binding.chipW4)
        chips.add(binding.chipW5)
        chips.add(binding.chipW6)
        chips.add(binding.chipW7)

        binding.editRule.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, Rule.names)
        )
        binding.editRule.setOnItemClickListener { _, _, position, _ ->
            rule = position
            render()
        }

        binding.toggleKind.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            kind = if (checkedId == R.id.btnKindSpan) Kind.SPAN_ONCE else Kind.DAILY_RESET
            render()
        }
        binding.switchInfinite.setOnCheckedChangeListener { _, checked ->
            infinite = checked
            render()
        }
        binding.rowStartDate.setOnClickListener {
            pickDate(startDate) { picked ->
                startDate = picked
                if (rule == Rule.ONCE) endDate = picked
                render()
            }
        }
        binding.rowEndDate.setOnClickListener {
            pickDate(endDate) { picked ->
                endDate = picked
                render()
            }
        }

        binding.editInterval.doAfterTextChanged {
            intervalDays = it?.toString()?.toIntOrNull()?.coerceIn(1, 365) ?: intervalDays
        }
        binding.editMonthDay.doAfterTextChanged {
            monthDay = it?.toString()?.toIntOrNull()?.coerceIn(1, 31) ?: monthDay
        }
        binding.editSpanDays.doAfterTextChanged {
            spanDays = it?.toString()?.toIntOrNull()?.coerceIn(1, 3650) ?: spanDays
            binding.textSpanEnd.text = spanEndText()
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        if (taskId > 0) {
            lifecycleScope.launch {
                val task = app().repo.taskById(taskId)
                if (task != null) {
                    existing = task
                    applyTask(task)
                }
                render()
            }
        } else {
            applyDefaults()
            render()
        }
    }

    private fun applyDefaults() {
        kind = Kind.DAILY_RESET
        rule = Rule.DAILY
        infinite = true
        intervalDays = 2
        monthDay = 1
        spanDays = 2
        for (i in chips.indices) {
            chips[i].isChecked = i <= 4
        }
    }

    private fun applyTask(t: Task) {
        binding.editTitle.setText(t.title)
        binding.editNote.setText(t.note)
        kind = t.kind
        rule = if (t.kind == Kind.SPAN_ONCE) Rule.DAILY else t.rule
        startDate = LocalDate.ofEpochDay(t.startDate)
        intervalDays = t.intervalDays
        monthDay = t.monthDay
        for (i in chips.indices) {
            chips[i].isChecked = ((t.weekdaysMask shr i) and 1) == 1
        }
        if (t.kind == Kind.SPAN_ONCE) {
            val end = t.endDate ?: t.startDate
            spanDays = (end - t.startDate + 1).toInt().coerceAtLeast(1)
            endDate = LocalDate.ofEpochDay(end)
            infinite = true
        } else {
            val end = t.endDate
            infinite = end == null || t.rule == Rule.ONCE
            endDate = end?.let { LocalDate.ofEpochDay(it) } ?: startDate.plusDays(29)
        }
    }

    private fun render() {
        val isDaily = kind == Kind.DAILY_RESET

        val wantKind = if (isDaily) R.id.btnKindDaily else R.id.btnKindSpan
        if (binding.toggleKind.checkedButtonId != wantKind) {
            binding.toggleKind.check(wantKind)
        }
        binding.textKindHint.text =
            getString(if (isDaily) R.string.kind_daily_hint else R.string.kind_span_hint)

        binding.groupRepeat.isVisible = isDaily
        binding.groupSpan.isVisible = !isDaily
        binding.labelStartDate.text =
            if (isDaily && rule == Rule.ONCE) "日期" else getString(R.string.label_start_date)

        if (!binding.editRule.text.toString().startsWith(Rule.names[rule])) {
            binding.editRule.setText(Rule.names[rule], false)
        }
        binding.groupWeekdays.isVisible = isDaily && rule == Rule.WEEKDAYS
        binding.layoutInterval.isVisible = isDaily && rule == Rule.EVERY_N_DAYS
        binding.layoutMonthDay.isVisible = isDaily && rule == Rule.MONTHLY
        binding.groupEnd.isVisible = isDaily && rule != Rule.ONCE

        setIfChanged(binding.editInterval, intervalDays)
        setIfChanged(binding.editMonthDay, monthDay)
        setIfChanged(binding.editSpanDays, spanDays)

        binding.rowStartDate.text = dateFormat.format(startDate)
        binding.switchInfinite.isChecked = infinite
        binding.labelEndDate.isVisible = !infinite
        binding.rowEndDate.isVisible = !infinite
        binding.rowEndDate.text = dateFormat.format(endDate)
        binding.textSpanEnd.text = spanEndText()
    }

    private fun setIfChanged(edit: EditText, value: Int) {
        if (edit.text?.toString() != value.toString()) {
            edit.setText(value.toString())
        }
    }

    private fun spanEndText(): String {
        val end = startDate.plusDays((spanDays - 1).toLong())
        return "结束日期：" + dateFormat.format(end)
    }

    private fun pickDate(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    private fun currentMask(): Int {
        var mask = 0
        for (i in chips.indices) {
            if (chips[i].isChecked) mask = mask or (1 shl i)
        }
        return mask
    }

    private fun save() {
        val title = binding.editTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            binding.editTitle.error = "请填写要做的事"
            return
        }
        val note = binding.editNote.text?.toString()?.trim().orEmpty()

        val mask = currentMask()
        if (kind == Kind.DAILY_RESET && rule == Rule.WEEKDAYS && mask == 0) {
            Toast.makeText(this, "请至少选择一个星期", Toast.LENGTH_SHORT).show()
            return
        }

        val finalEnd: Long? = if (kind == Kind.SPAN_ONCE) {
            startDate.plusDays((spanDays - 1).toLong()).toEpochDay()
        } else {
            when {
                rule == Rule.ONCE -> startDate.toEpochDay()
                infinite -> null
                endDate.isBefore(startDate) -> startDate.toEpochDay()
                else -> endDate.toEpochDay()
            }
        }

        val task = Task(
            id = existing?.id ?: 0L,
            title = title,
            note = note,
            kind = kind,
            rule = if (kind == Kind.SPAN_ONCE) Rule.ONCE else rule,
            weekdaysMask = if (kind == Kind.SPAN_ONCE) 0 else mask,
            intervalDays = intervalDays,
            monthDay = monthDay,
            startDate = startDate.toEpochDay(),
            endDate = finalEnd,
            sortOrder = existing?.sortOrder ?: 0,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )

        lifecycleScope.launch {
            app().repo.save(task)
            WidgetUpdater.refresh(this@EditTaskActivity)
            finish()
        }
    }

    private fun confirmDelete() {
        val task = existing ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(task.title)
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    app().repo.delete(task)
                    WidgetUpdater.refresh(this@EditTaskActivity)
                    finish()
                }
            }
            .show()
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_DATE = "date"

        fun newIntent(context: Context, date: LocalDate): Intent =
            Intent(context, EditTaskActivity::class.java)
                .putExtra(EXTRA_DATE, date.toEpochDay())

        fun editIntent(context: Context, taskId: Long): Intent =
            Intent(context, EditTaskActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
    }
}
