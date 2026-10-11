package com.daytodo.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.daytodo.app.R
import com.daytodo.app.databinding.ItemDateBinding
import java.time.LocalDate

class DateStripAdapter(
    private val dates: List<LocalDate>,
    private var selected: LocalDate,
    private val onPick: (LocalDate) -> Unit
) : RecyclerView.Adapter<DateStripAdapter.VH>() {

    private var counts: Map<Long, Int> = emptyMap()

    class VH(val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = dates.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val date = dates[position]
        val b = holder.binding
        val today = LocalDate.now()
        val isSelected = date == selected

        b.root.setBackgroundResource(
            if (isSelected) R.drawable.bg_day_selected else R.drawable.bg_day_normal
        )
        val color = ContextCompat.getColor(
            b.root.context,
            if (isSelected) R.color.chip_text_selected else R.color.chip_text
        )
        b.textWeek.setTextColor(color)
        b.textDay.setTextColor(color)
        b.textMonthTag.setTextColor(color)

        b.textWeek.text = when (date) {
            today -> "今天"
            today.plusDays(1) -> "明天"
            today.minusDays(1) -> "昨天"
            else -> WEEK_CN[date.dayOfWeek.value - 1]
        }
        b.textDay.text = date.dayOfMonth.toString()
        b.textMonthTag.text = if (date.dayOfMonth == 1) "${date.monthValue}月" else ""
        b.dot.isVisible = (counts[date.toEpochDay()] ?: 0) > 0

        b.root.setOnClickListener { onPick(date) }
    }

    fun setSelected(date: LocalDate) {
        if (date == selected) return
        val oldPos = dates.indexOf(selected)
        val newPos = dates.indexOf(date)
        selected = date
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    fun setCounts(map: Map<Long, Int>) {
        counts = map
        notifyDataSetChanged()
    }

    fun positionOf(date: LocalDate): Int = dates.indexOf(date)

    companion object {
        private val WEEK_CN = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }
}
