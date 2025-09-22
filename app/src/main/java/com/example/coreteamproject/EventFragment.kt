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
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Fragment per gestire gli eventi
class EventFragment : Fragment() {

    // Variabili per data binding e viewmodel
    private lateinit var binding: FragmentEventBinding
    private lateinit var viewModel: EventViewModel
    private var isAdmin: Boolean = false

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

        // Carica il profilo utente per determinare il ruolo
        loadUserProfile()
        
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
            // inflata il layout della card usando il data binding
            val cardBinding = com.example.coreteamproject.databinding.CardEventoBinding.inflate(layoutInflater, binding.eventsLayout, false)

            // imposta la variabile evento nel layout della card
            cardBinding.evento = evento

            // imposta il listener per l'eliminazione
            cardBinding.root.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Elimina Evento")
                    .setMessage("Vuoi eliminare '${evento.title}'?")
                    .setPositiveButton("Elimina") { _, _ ->
                        viewModel.eliminaEvento(evento.id)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }

            // aggiunge la card al layout
            binding.eventsLayout.addView(cardBinding.root)
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
    private fun updateUiForAdmin() {
        val adminPrimaryColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        val adminVariantColor = ContextCompat.getColor(requireContext(), R.color.admin_primary_variant)

        // Aggiorna titolo e sottotitolo
        binding.textViewEvent.setTextColor(adminPrimaryColor)
        binding.textDescriptionEvent.setTextColor(adminVariantColor)
        binding.textDescriptionEvent.text = "Gestione eventi del team"

        // Aggiorna colore testo data
        binding.textSelectedDate.setTextColor(adminPrimaryColor)

        // Aggiorna colore pulsante
        binding.btnAddEvent.background.setTint(adminPrimaryColor)
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("Profili").document(userId).get()
            .addOnSuccessListener { document ->
                val userRole = document.getString("RUOLO")
                isAdmin = userRole == "ADMIN"
                if (isAdmin) {
                    updateUiForAdmin()
                }
            }
            .addOnFailureListener {
                // In caso di errore, si procede con la UI di default
                isAdmin = false
            }
    }

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
        
        // Spazio prima del checkbox
        val space = Space(requireContext())
        space.minimumHeight = 20
        layout.addView(space)

        // Checkbox per privacy
        val checkBoxPrivate = CheckBox(requireContext())
        checkBoxPrivate.text = "Evento privato (visibile solo a te)"
        layout.addView(checkBoxPrivate)

        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Evento")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val title = editTitle.text.toString().trim()
                val time = editTime.text.toString().trim()
                val description = editDescription.text.toString().trim()
                val isPrivate = checkBoxPrivate.isChecked
                //salva evento solo se ha titolo
                if (title.isNotEmpty()) {
                    viewModel.salvaEvento(title, time, description, isPrivate)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}