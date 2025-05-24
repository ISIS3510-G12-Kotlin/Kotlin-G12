package com.example.explorandes.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.databinding.ItemVisitedBinding
import com.example.explorandes.models.VisitedItem
import java.text.SimpleDateFormat
import java.util.*

class VisitedAdapter : ListAdapter<VisitedItem, VisitedAdapter.VisitedViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitedViewHolder {
        val binding = ItemVisitedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VisitedViewHolder(private val binding: ItemVisitedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VisitedItem) {
            binding.tvTitle.text = item.title
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(item.timestamp))
            binding.tvStatus.text = if (item.wasSynced) "✓ Sincronizado" else "✗ Sin conexión"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VisitedItem>() {
        override fun areItemsTheSame(oldItem: VisitedItem, newItem: VisitedItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VisitedItem, newItem: VisitedItem) =
            oldItem == newItem
    }
}
