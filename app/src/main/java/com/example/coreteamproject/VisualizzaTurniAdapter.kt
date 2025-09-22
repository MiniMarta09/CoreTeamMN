package com.example.coreteamproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coreteamproject.databinding.CardProgrammazioneSettimanaleBinding
import com.example.coreteamproject.databinding.ItemTurnoGiornoBinding

class VisualizzaTurniAdapter : ListAdapter<ProgrammazioneSettimanalePersona, VisualizzaTurniAdapter.ProgrammazioneViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgrammazioneViewHolder {
        val binding = CardProgrammazioneSettimanaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProgrammazioneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgrammazioneViewHolder, position: Int) {
        val programmazione = getItem(position)
        holder.bind(programmazione)
    }

    class ProgrammazioneViewHolder(private val binding: CardProgrammazioneSettimanaleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(programmazione: ProgrammazioneSettimanalePersona) {
            binding.textViewNomeDipendente.text = programmazione.nomeDipendente

            // Trova il settore dal primo turno valido (sarà lo stesso per tutti)
            val settore = programmazione.turniSettimanali.values.firstOrNull { it != null }?.settore ?: "N/D"
            binding.textViewSettoreDipendente.text = settore

            val giorniViews = mapOf(
                "Lunedì" to ItemTurnoGiornoBinding.bind(binding.lunedi.root),
                "Martedì" to ItemTurnoGiornoBinding.bind(binding.martedi.root),
                "Mercoledì" to ItemTurnoGiornoBinding.bind(binding.mercoledi.root),
                "Giovedì" to ItemTurnoGiornoBinding.bind(binding.giovedi.root),
                "Venerdì" to ItemTurnoGiornoBinding.bind(binding.venerdi.root),
                "Sabato" to ItemTurnoGiornoBinding.bind(binding.sabato.root),
                "Domenica" to ItemTurnoGiornoBinding.bind(binding.domenica.root)
            )

            giorniViews.forEach { (giorno, viewBinding) ->
                viewBinding.textViewGiorno.text = giorno
                val turno = programmazione.turniSettimanali[giorno]
                if (turno != null) {
                    // Mostra solo l'orario, il settore è già nel titolo della card
                    viewBinding.textViewDettagliTurno.text = turno.orario
                } else {
                    viewBinding.textViewDettagliTurno.text = "Riposo"
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProgrammazioneSettimanalePersona>() {
        override fun areItemsTheSame(oldItem: ProgrammazioneSettimanalePersona, newItem: ProgrammazioneSettimanalePersona): Boolean {
            return oldItem.nomeDipendente == newItem.nomeDipendente
        }

        override fun areContentsTheSame(oldItem: ProgrammazioneSettimanalePersona, newItem: ProgrammazioneSettimanalePersona): Boolean {
            return oldItem == newItem
        }
    }
}
