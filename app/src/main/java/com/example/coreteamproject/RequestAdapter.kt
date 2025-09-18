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

// Adapter per la RecyclerView che mostra la lista delle richieste
class RequestAdapter(
    private val currentUserId: String?,
    private val isAdmin: Boolean,
    private val onDeleteClicked: (RequestsViewModel.Request) -> Unit,
    private val onApproveClicked: (RequestsViewModel.Request) -> Unit,
    private val onRejectClicked: (RequestsViewModel.Request) -> Unit
) : ListAdapter<RequestsViewModel.Request, RequestAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder che contiene i riferimenti agli elementi della UI di una singola card di richiesta
    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewRequestType: TextView = itemView.findViewById(R.id.textViewRequestType)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewDateRange: TextView = itemView.findViewById(R.id.textViewDateRange)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewReason: TextView = itemView.findViewById(R.id.textViewReason)
        private val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
        private val textViewAuthor: TextView = itemView.findViewById(R.id.textViewAuthor) // Aggiunto per il nome dell'autore
        private val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)
        private val buttonApprove: Button = itemView.findViewById(R.id.buttonApprove)
        private val buttonReject: Button = itemView.findViewById(R.id.buttonReject)
        private val layoutTime: View = itemView.findViewById(R.id.layoutTimeDisplay)
        private val layoutDateRange: View = itemView.findViewById(R.id.layoutDateRangeDisplay)
        private val layoutAdminButtons: View = itemView.findViewById(R.id.layoutAdminButtons)

        // Metodo per collegare i dati di una richiesta specifica agli elementi della UI della card
        fun bind(request: RequestsViewModel.Request) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            textViewRequestType.text = request.getTypeDisplayName()
            textViewDate.text = dateTimeFormat.format(request.timestamp)
            textViewReason.text = "Motivazione: ${request.reason}"

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

            // Imposta il testo e il colore per lo stato di approvazione
            textViewStatus.text = "Stato: ${request.getStatusDisplayName()}"
            val statusColor = when (request.status) {
                RequestsViewModel.RequestStatus.ACCETTATA -> ContextCompat.getColor(itemView.context, R.color.green)
                RequestsViewModel.RequestStatus.RIFIUTATA -> ContextCompat.getColor(itemView.context, R.color.red)
                RequestsViewModel.RequestStatus.IN_ATTESA -> ContextCompat.getColor(itemView.context, R.color.orange)
            }
            textViewStatus.setTextColor(statusColor)


            // Logica per mostrare/nascondere i bottoni e le info in base al ruolo
            if (isAdmin) {
                // Vista Admin
                textViewAuthor.visibility = View.VISIBLE
                textViewAuthor.text = "Richiesta di: ${request.authorName} (${request.settore})"
                buttonDelete.visibility = View.GONE // L'admin non può eliminare, solo accettare/rifiutare

                // Gestione visibilità pulsanti admin
                if (request.status == RequestsViewModel.RequestStatus.IN_ATTESA) {
                    layoutAdminButtons.visibility = View.VISIBLE
                    buttonApprove.setOnClickListener { onApproveClicked(request) }
                    buttonReject.setOnClickListener { onRejectClicked(request) }
                } else {
                    layoutAdminButtons.visibility = View.GONE
                }
            } else {
                // Vista Utente Standard
                textViewAuthor.visibility = View.GONE
                layoutAdminButtons.visibility = View.GONE

                // Mostra "Elimina" solo se l'utente è il proprietario e la richiesta è in attesa
                if (request.userId == currentUserId && request.status == RequestsViewModel.RequestStatus.IN_ATTESA) {
                    buttonDelete.visibility = View.VISIBLE
                    buttonDelete.setOnClickListener { onDeleteClicked(request) }
                } else {
                    buttonDelete.visibility = View.GONE
                }
            }
        }
    }

    // DiffUtil per calcolare in modo efficiente le differenze tra la vecchia e la nuova lista di richieste
    // Questo ottimizza le performance della RecyclerView, aggiornando solo gli elementi cambiati
    class RequestDiffCallback : DiffUtil.ItemCallback<RequestsViewModel.Request>() {
        override fun areItemsTheSame(oldItem: RequestsViewModel.Request, newItem: RequestsViewModel.Request): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RequestsViewModel.Request, newItem: RequestsViewModel.Request): Boolean {
            return oldItem == newItem
        }
    }
}