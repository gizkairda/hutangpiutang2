package com.example.hutangpiutang2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class DebtAdapter(
    private var debts: List<Debt>,
    private val onEditClick: (Debt) -> Unit,
    private val onDeleteClick: (Debt) -> Unit,
    private val onStatusClick: (Debt) -> Unit
) : RecyclerView.Adapter<DebtAdapter.DebtViewHolder>() {

    fun updateList(newList: List<Debt>) {
        debts = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(debts[position])
    }

    override fun getItemCount() = debts.size

    inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(debt: Debt) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = debt.title
            itemView.findViewById<TextView>(R.id.tvPersonName).text = debt.personName
            itemView.findViewById<TextView>(R.id.tvAmount).text = "Rp ${String.format("%,.0f", debt.amount)}"
            itemView.findViewById<TextView>(R.id.tvDescription).text = debt.description

            val tvType = itemView.findViewById<TextView>(R.id.tvType)
            tvType.text = debt.type.uppercase(Locale.getDefault())

            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            tvStatus.text = debt.status.uppercase(Locale.getDefault())

            val tvDueDate = itemView.findViewById<TextView>(R.id.tvDueDate)
            debt.dueDate?.let {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvDueDate.text = "Jatuh Tempo: ${dateFormat.format(it)}"
            } ?: run {
                tvDueDate.text = "Tidak ada jatuh tempo"
            }

            // Warna berdasarkan jenis
            if (debt.type == "hutang") {
                tvType.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                itemView.findViewById<TextView>(R.id.tvAmount).setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            } else {
                tvType.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                itemView.findViewById<TextView>(R.id.tvAmount).setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            }

            // Warna status
            val btnToggleStatus = itemView.findViewById<MaterialButton>(R.id.btnToggleStatus)
            if (debt.status == "lunas") {
                tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                btnToggleStatus.text = "Tandai Aktif"
            } else {
                tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                btnToggleStatus.text = "Tandai Lunas"
            }

            itemView.findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener {
                onEditClick(debt)
            }
            itemView.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
                onDeleteClick(debt)
            }
            btnToggleStatus.setOnClickListener {
                onStatusClick(debt)
            }
        }
    }
}