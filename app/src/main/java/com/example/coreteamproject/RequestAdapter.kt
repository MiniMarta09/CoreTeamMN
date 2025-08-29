package com.example.coreteamproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RequestAdapter(
    private val currentUserId: String?,
    private val onDeleteClicked: (RequestsViewModel.Request) -> Unit
) : ListAdapter<RequestsViewModel.Request, RequestAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewRequestType: TextView = itemView.findViewById(R.id.textViewRequestType)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewDateRange: TextView = itemView.findViewById(R.id.textViewDateRange)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewReason: TextView = itemView.findViewById(R.id.textViewReason)
        private val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)
        private val layoutTime: View = itemView.findViewById(R.id.layoutTimeDisplay)
        private val layoutDateRange: View = itemView.findViewById(R.id.layoutDateRangeDisplay)

        fun bind(request: RequestsViewModel.Request) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            // Tipo di richiesta
            textViewRequestType.text = request.getTypeDisplayName()

            // Status con colori
            // RIMOSSO - non più necessario

            // Data di creazione
            textViewDate.text = dateTimeFormat.format(request.timestamp)

            // Configurazione visualizzazione date/orari in base al tipo
            when (request.type) {
                RequestsViewModel.RequestType.FERIE, RequestsViewModel.RequestType.SMARTWORKING -> {
                    layoutDateRange.visibility = View.VISIBLE
                    layoutTime.visibility = View.GONE

                    val startDateStr = dateFormat.format(request.startDate)
                    val endDateStr = request.endDate?.let { dateFormat.format(it) } ?: "N/A"
                    textViewDateRange.text = "Dal $startDateStr al $endDateStr"
                }
                RequestsViewModel.RequestType.PERMESSO_ENTRATA, RequestsViewModel.RequestType.PERMESSO_USCITA -> {
                    layoutDateRange.visibility = View.VISIBLE
                    layoutTime.visibility = View.VISIBLE

                    textViewDateRange.text = "Data: ${dateFormat.format(request.startDate)}"

                    when (request.type) {
                        RequestsViewModel.RequestType.PERMESSO_ENTRATA -> {
                            textViewTime.text = "Entrata posticipata alle: ${request.startTime ?: "N/A"}"
                        }
                        RequestsViewModel.RequestType.PERMESSO_USCITA -> {
                            textViewTime.text = "Uscita anticipata alle: ${request.startTime ?: "N/A"}"
                        }
                        else -> {}
                    }
                }
            }

            // Motivazione
            textViewReason.text = "Motivazione: ${request.reason}"

            // Note dell'amministratore (rimosse)
            // RIMOSSO - non più necessario

            // Bottone elimina (sempre visibile per l'utente proprietario)
            if (request.userId == currentUserId) {
                buttonDelete.visibility = View.VISIBLE
                buttonDelete.setOnClickListener { onDeleteClicked(request) }
            } else {
                buttonDelete.visibility = View.GONE
            }
        }
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<RequestsViewModel.Request>() {
        override fun areItemsTheSame(oldItem: RequestsViewModel.Request, newItem: RequestsViewModel.Request): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RequestsViewModel.Request, newItem: RequestsViewModel.Request): Boolean {
            return oldItem == newItem
        }
    }
}