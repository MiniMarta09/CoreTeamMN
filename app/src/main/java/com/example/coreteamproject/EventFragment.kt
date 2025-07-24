package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.coreteamproject.databinding.FragmentEventBinding
import java.text.SimpleDateFormat
import java.util.*

// Fragment per gestire gli eventi
class EventFragment : Fragment() {

    // Variabili per data binding e viewmodel
    private lateinit var binding: FragmentEventBinding
    private lateinit var viewModel: EventViewModel

    // Metodo chiamato alla creazione del fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inizializza data binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_event, container, false)
        
        // Inizializza ViewModel
        viewModel = ViewModelProvider(this)[EventViewModel::class.java]
        
        // Collega ViewModel al binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        
        // Setup UI
        setupUI()
        setupObservers()
        
        return binding.root
    }

    // Configurazione interfaccia utente
    private fun setupUI() {
        // Listener per selezione sul calendario
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.updateSelectedDate(year, month, dayOfMonth)
        }

        // Bottone aggiungi
        binding.btnAddEvent.setOnClickListener {
            showAddDialog()
        }
    }
    
    // Configurazione degli observer per reagire ai cambiamenti nel viewmodel
    private fun setupObservers() {
        // Osserva la lista degli eventi
        viewModel.eventi.observe(viewLifecycleOwner) { eventi ->
            updateEventsUI(eventi)
        }
        
        // Osserva gli errori
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.resetError()
            }
        }
        
        // Osserva il successo del salvataggio
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Evento aggiunto!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveSuccess()
            }
        }
        
        // Osserva il successo dell'eliminazione
        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Evento eliminato!", Toast.LENGTH_SHORT).show()
                viewModel.resetDeleteSuccess()
            }
        }
        
        // Osserva lo stato vuoto
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            if (isEmpty) {
                showEmptyState()
            }
        }
    }
    
    // Aggioranamento visualizzazione eventi
    private fun updateEventsUI(eventi: List<Evento>) {
        binding.eventsLayout.removeAllViews()
        
        for (evento in eventi) {
            val eventView = createEventView(evento)
            binding.eventsLayout.addView(eventView)
        }
    }
    
    // Messaggio se non ci sono eventi
    private fun showEmptyState() {
        binding.eventsLayout.removeAllViews()
        val noEventsText = TextView(requireContext())
        noEventsText.text = "Nessun evento"
        noEventsText.textSize = 16f
        noEventsText.setPadding(16, 16, 16, 16)
        binding.eventsLayout.addView(noEventsText)
    }

    // Finestra di dialogo per aggiungere nuovi eventi
    private fun showAddDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

       // Titolo evento
        val editTitle = EditText(requireContext())
        editTitle.hint = "Titolo evento"
        layout.addView(editTitle)

        // Ora evento
        val editTime = EditText(requireContext())
        editTime.hint = "Ora (es. 14:30)"
        layout.addView(editTime)

       // Descrizione evento
        val editDescription = EditText(requireContext())
        editDescription.hint = "Descrizione"
        layout.addView(editDescription)

        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Evento")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val title = editTitle.text.toString().trim()
                val time = editTime.text.toString().trim()
                val description = editDescription.text.toString().trim()
                //salva evento solo se ha titolo
                if (title.isNotEmpty()) {
                    viewModel.salvaEvento(title, time, description)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Crea visualizzazione evento
    private fun createEventView(evento: Evento): View {
        val eventLayout = LinearLayout(requireContext())
        eventLayout.orientation = LinearLayout.VERTICAL
        eventLayout.setPadding(32, 32, 32, 32)
        eventLayout.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))

      // Margini per separare eventi
       val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 16, 0, 16)
        eventLayout.layoutParams = layoutParams

        // Titolo evento
        val titleText = TextView(requireContext())
        titleText.text = evento.title
        titleText.textSize = 18f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        eventLayout.addView(titleText)

        // Ora evento
        if (evento.time.isNotEmpty()) {
            val timeText = TextView(requireContext())
            timeText.text = "Ora: ${evento.time}"
            timeText.textSize = 14f
            eventLayout.addView(timeText)
        }

        // Descrizione evento
        if (evento.description.isNotEmpty()) {
            val descText = TextView(requireContext())
            descText.text = evento.description
            descText.textSize = 14f
            eventLayout.addView(descText)
        }

       // Eliminazione dell'evento
        eventLayout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Elimina Evento")
                .setMessage("Vuoi eliminare '${evento.title}'?")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.eliminaEvento(evento.id)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        return eventLayout
    }
}