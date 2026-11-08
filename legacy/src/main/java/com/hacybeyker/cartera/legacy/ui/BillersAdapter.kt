package com.hacybeyker.cartera.legacy.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hacybeyker.cartera.legacy.billers.BillerRow
import com.hacybeyker.cartera.legacy.databinding.LegacyRowBillerBinding
import com.hacybeyker.cartera.legacy.databinding.LegacyRowCategoryBinding

private const val TYPE_HEADER = 0
private const val TYPE_BILLER = 1

internal class BillersAdapter(private val onBiller: (String) -> Unit) :
    ListAdapter<BillerRow, RecyclerView.ViewHolder>(BillerRowDiff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is BillerRow.Header -> TYPE_HEADER
        is BillerRow.Item -> TYPE_BILLER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val row = LegacyRowCategoryBinding.inflate(inflater, parent, false).root
            ViewCompat.setAccessibilityHeading(row, true)
            RowHolder(row)
        } else {
            RowHolder(LegacyRowBillerBinding.inflate(inflater, parent, false).root)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val text = holder.itemView as TextView
        when (val row = getItem(position)) {
            is BillerRow.Header -> text.text = row.category
            is BillerRow.Item -> {
                text.text = row.name
                text.setOnClickListener { onBiller(row.id) }
            }
        }
    }

    private class RowHolder(row: TextView) : RecyclerView.ViewHolder(row)
}

private object BillerRowDiff : DiffUtil.ItemCallback<BillerRow>() {
    override fun areItemsTheSame(oldItem: BillerRow, newItem: BillerRow): Boolean = when {
        oldItem is BillerRow.Header && newItem is BillerRow.Header -> oldItem.category == newItem.category
        oldItem is BillerRow.Item && newItem is BillerRow.Item -> oldItem.id == newItem.id
        else -> false
    }

    override fun areContentsTheSame(oldItem: BillerRow, newItem: BillerRow): Boolean = oldItem == newItem
}
