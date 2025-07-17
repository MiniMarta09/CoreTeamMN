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

class EventFragment : Fragment() {

    private lateinit var binding: FragmentEventBinding
    private lateinit var viewModel: EventViewModel

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

    private fun setupUI() {
        // Calendario
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.updateSelectedDate(year, month, dayOfMonth)
        }

        // Bottone aggiungi
        binding.btnAddEvent.setOnClickListener {
            showAddDialog()
        }
    }
    
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
    
    private fun updateEventsUI(eventi: List<Evento>) {
        binding.eventsLayout.removeAllViews()
        
        for (evento in eventi) {
            val eventView = createEventView(evento)
            binding.eventsLayout.addView(eventView)
        }
    }
    
    private fun showEmptyState() {
        binding.eventsLayout.removeAllViews()
        val noEventsText = TextView(requireContext())
        noEventsText.text = "Nessun evento"
        noEventsText.textSize = 16f
        noEventsText.setPadding(16, 16, 16, 16)
        binding.eventsLayout.addView(noEventsText)
    }

    private fun showAddDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

        val editTitle = EditText(requireContext())
        editTitle.hint = "Titolo evento"
        layout.addView(editTitle)

        val editTime = EditText(requireContext())
        editTime.hint = "Ora (es. 14:30)"
        layout.addView(editTime)

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
                if (title.isNotEmpty()) {
                    viewModel.salvaEvento(title, time, description)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun createEventView(evento: Evento): View {
        val eventLayout = LinearLayout(requireContext())
        eventLayout.orientation = LinearLayout.VERTICAL
        eventLayout.setPadding(32, 32, 32, 32)
        eventLayout.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 16, 0, 16)
        eventLayout.layoutParams = layoutParams

        // Titolo
        val titleText = TextView(requireContext())
        titleText.text = evento.title
        titleText.textSize = 18f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        eventLayout.addView(titleText)

        // Ora
        if (evento.time.isNotEmpty()) {
            val timeText = TextView(requireContext())
            timeText.text = "Ora: ${evento.time}"
            timeText.textSize = 14f
            eventLayout.addView(timeText)
        }

        // Descrizione
        if (evento.description.isNotEmpty()) {
            val descText = TextView(requireContext())
            descText.text = evento.description
            descText.textSize = 14f
            eventLayout.addView(descText)
        }

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