package com.hacybeyker.cartera.legacy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.billers.BillerRow
import com.hacybeyker.cartera.legacy.databinding.LegacyRowCategoryBinding

private const val TYPE_HEADER = 0
private const val TYPE_BILLER = 1

internal class FullBillersAdapter(private val rows: List<BillerRow>, private val onBiller: (String) -> Unit) :
    RecyclerView.Adapter<FullBillersAdapter.RowHolder>() {

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is BillerRow.Header -> TYPE_HEADER
        is BillerRow.Item -> TYPE_BILLER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val row = LegacyRowCategoryBinding.inflate(inflater, parent, false).root
            ViewCompat.setAccessibilityHeading(row, true)
            RowHolder(row, row)
        } else {
            val row = inflater.inflate(R.layout.legacy_row_biller_full, parent, false)
            RowHolder(row, checkNotNull(row.findText()))
        }
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        when (val row = rows[position]) {
            is BillerRow.Header -> holder.text.text = row.category
            is BillerRow.Item -> {
                holder.text.text = row.name
                holder.text.setOnClickListener { onBiller(row.id) }
            }
        }
    }

    internal class RowHolder(row: View, val text: TextView) : RecyclerView.ViewHolder(row)
}

private fun View.findText(): TextView? = when (this) {
    is TextView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findText() }
    else -> null
}
