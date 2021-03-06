package com.gdavidpb.tuindice.ui.adapters.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gdavidpb.tuindice.ui.viewholders.base.BaseViewHolder

abstract class BaseAdapter<T> : RecyclerView.Adapter<BaseViewHolder<T>>() {
    protected val items = mutableListOf<T>()

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        val item = items[position]

        holder.bindView(item)
    }

    abstract fun provideComparator(): Comparator<T>

    open fun swapItems(new: List<T>) {
        val diffUtil = GenericDiffUtil(old = items, new = new, comparator = provideComparator())

        DiffUtil.calculateDiff(diffUtil, true).dispatchUpdatesTo(this)

        items.clear()
        items.addAll(new)
    }

    open fun getItem(position: Int): T {
        return items[position]
    }

    open fun removeItemAt(position: Int, notifyChange: Boolean = true) {
        items.removeAt(position)

        if (notifyChange)
            notifyItemRemoved(position)
    }

    open fun addItemAt(item: T, position: Int, notifyChange: Boolean = true) {
        items.add(position, item)

        if (notifyChange)
            notifyItemInserted(position)
    }

    open fun replaceItemAt(item: T, position: Int, notifyChange: Boolean = true) {
        items[position] = item

        if (notifyChange)
            notifyItemChanged(position)
    }

    inline fun <reified Q : T> Q.compareTo(b: Any, comparator: (a: Q, b: Q) -> Boolean): Int {
        return if (b is Q && comparator(this, b)) 0 else -1
    }

    private inner class GenericDiffUtil(
            private val old: List<T>,
            private val new: List<T>,
            private val comparator: Comparator<T>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = old.size

        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = old[oldItemPosition]
            val newItem = new[newItemPosition]

            return comparator.compare(oldItem, newItem) == 0
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = old[oldItemPosition]
            val newItem = new[newItemPosition]

            return oldItem == newItem
        }
    }
}