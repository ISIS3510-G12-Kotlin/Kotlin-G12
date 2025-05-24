package com.example.explorandes.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.explorandes.databinding.ItemVisitedBinding
import com.example.explorandes.models.EventDetail
import java.text.SimpleDateFormat
import java.util.*

class VisitedSimpleAdapter :
    RecyclerView.Adapter<VisitedSimpleAdapter.VisitedSimpleViewHolder>() {

    private var items: List<Pair<EventDetail, Boolean>> = emptyList()

    fun submitList(list: List<Pair<EventDetail, Boolean>>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitedSimpleViewHolder {
        val binding = ItemVisitedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitedSimpleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitedSimpleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VisitedSimpleViewHolder(private val binding: ItemVisitedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<EventDetail, Boolean>) {
            val (event, wasSynced) = item
            binding.tvTitle.text = event.title
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date()) // O usa event.startTime si lo conviertes
            binding.tvStatus.text = if (wasSynced) "✓ Sincronizado" else "✗ Sin conexión"
        }
    }
}
