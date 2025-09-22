package com.example.coreteamproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coreteamproject.databinding.CardOrarioContrattoBinding

class OrariContrattoAdapter : ListAdapter<OrarioContratto, OrariContrattoAdapter.OrarioViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrarioViewHolder {
        val binding = CardOrarioContrattoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrarioViewHolder, position: Int) {
        val orario = getItem(position)
        holder.bind(orario)
    }

    class OrarioViewHolder(private val binding: CardOrarioContrattoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(orario: OrarioContratto) {
            binding.textViewSettore.text = orario.settore
            binding.textViewTipoContratto.text = orario.tipoContratto
            binding.textViewGiorni.text = orario.giorniLavorativi
            binding.textViewOrario.text = orario.orario
            binding.textViewPausa.text = orario.pausa
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrarioContratto>() {
        override fun areItemsTheSame(oldItem: OrarioContratto, newItem: OrarioContratto): Boolean {
            return oldItem.settore == newItem.settore
        }

        override fun areContentsTheSame(oldItem: OrarioContratto, newItem: OrarioContratto): Boolean {
            return oldItem == newItem
        }
    }
}
