package com.daytodo.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daytodo.app.databinding.ItemHistoryEntryBinding
import com.daytodo.app.databinding.ItemHistoryHeaderBinding
import com.daytodo.app.model.HistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = ArrayList<Row>()

    sealed class Row {
        data class Header(val date: LocalDate, val count: Int) : Row()
        data class Entry(val entry: HistoryEntry) : Row()
    }

    class HeaderVH(val binding: ItemHistoryHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    class EntryVH(val binding: ItemHistoryEntryBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(entries: List<HistoryEntry>) {
        rows.clear()
        entries.groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .forEach { (date, list) ->
                rows.add(Row.Header(LocalDate.ofEpochDay(date), list.size))
                list.sortedByDescending { it.completedAt }
                    .forEach { rows.add(Row.Entry(it)) }
            }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ENTRY

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemHistoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            EntryVH(ItemHistoryEntryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> {
                val b = (holder as HeaderVH).binding
                b.root.text = "${HEADER_FORMAT.format(row.date)} · 完成 ${row.count} 项"
            }
            is Row.Entry -> {
                val b = (holder as EntryVH).binding
                b.textTitle.text = row.entry.task.title
                val time = Instant.ofEpochMilli(row.entry.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                b.textTime.text = TIME_FORMAT.format(time)
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1

        private val HEADER_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    }
}
