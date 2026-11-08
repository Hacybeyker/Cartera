package com.hacybeyker.cartera.legacy.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.databinding.LegacyRowDebtBinding
import com.hacybeyker.cartera.legacy.debts.DebtItem

internal class DebtsAdapter(private val onToggle: (String) -> Unit) :
    ListAdapter<DebtItem, DebtsAdapter.DebtHolder>(DebtItemDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtHolder =
        DebtHolder(LegacyRowDebtBinding.inflate(LayoutInflater.from(parent.context), parent, false), onToggle)

    override fun onBindViewHolder(holder: DebtHolder, position: Int) = holder.bind(getItem(position))

    class DebtHolder(private val row: LegacyRowDebtBinding, onToggle: (String) -> Unit) :
        RecyclerView.ViewHolder(row.root) {

        private var debtId: String? = null

        init {
            row.root.setOnClickListener { debtId?.let(onToggle) }
        }

        fun bind(item: DebtItem) {
            debtId = item.id
            row.legacyRecibosCasilla.text = item.periodLabel
            row.legacyRecibosCasilla.isChecked = item.checked
            row.legacyRecibosVencimiento.text =
                row.root.context.getString(R.string.legacy_debts_due, item.dueDateLabel)
            row.legacyRecibosImporte.text = item.amountLabel
        }
    }
}

private object DebtItemDiff : DiffUtil.ItemCallback<DebtItem>() {
    override fun areItemsTheSame(oldItem: DebtItem, newItem: DebtItem): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: DebtItem, newItem: DebtItem): Boolean = oldItem == newItem
}
