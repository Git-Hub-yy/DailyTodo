package com.daytodo.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.daytodo.app.R
import com.daytodo.app.databinding.ItemTaskBinding
import com.daytodo.app.model.DayItem
import com.daytodo.app.model.Schedule
import com.google.android.material.color.MaterialColors

class TaskAdapter(
    private val onClick: (DayItem) -> Unit,
    private val onEdit: (DayItem) -> Unit,
    private val onDelete: (DayItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private val items = ArrayList<DayItem>()

    class VH(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    fun submit(list: List<DayItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.binding

        val primary =
            MaterialColors.getColor(b.icon, com.google.android.material.R.attr.colorPrimary)
        val muted = MaterialColors.getColor(
            b.icon,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        b.icon.setImageResource(
            if (item.completed) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
        )
        b.icon.imageTintList = ColorStateList.valueOf(if (item.completed) muted else primary)

        b.textTitle.text = item.task.title
        b.textTitle.paint.isStrikeThruText = item.completed
        b.textTitle.alpha = if (item.completed) 0.45f else 1f

        val note = item.task.note.trim()
        val desc = Schedule.describe(item.task)
        b.textSubtitle.text = if (note.isEmpty()) desc else "$desc · $note"
        b.textSubtitle.alpha = if (item.completed) 0.35f else 0.7f

        b.root.setOnClickListener { onClick(item) }
        b.btnMore.isVisible = true
        b.btnMore.setOnClickListener { anchor -> showMenu(anchor, item) }
    }

    private fun showMenu(anchor: View, item: DayItem) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menu.add(0, MENU_EDIT, 0, R.string.action_edit)
        popup.menu.add(0, MENU_DELETE, 1, R.string.action_delete)
        popup.setOnMenuItemClickListener { mi ->
            when (mi.itemId) {
                MENU_EDIT -> {
                    onEdit(item)
                    true
                }
                MENU_DELETE -> {
                    onDelete(item)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    companion object {
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
    }
}
