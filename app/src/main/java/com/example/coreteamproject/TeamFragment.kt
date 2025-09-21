package com.example.coreteamproject

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.coreteamproject.databinding.FragmentTeamBinding

// Visualizza la lista dei dipendenti
class TeamFragment : Fragment() {

    // Binding per il layout
    private lateinit var binding: FragmentTeamBinding

    // ViewModel per i dati degli utenti
    private lateinit var viewModel: UsersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_team, container, false
        )

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]

        // Associa il ViewModel al binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this // Imposta il lifecycle owner

        // Imposta gli observer
        setupObservers()
        
        // Imposta la ricerca
        setupSearchListener()

        // Carica i dipendenti
        viewModel.caricaDipendenti()
        
        // Controlla se l'utente è admin e mostra il pulsante disponibilità
        checkAdminAndShowAvailabilityButton()

        // Restituisce la view
        return binding.root
    }

    // Lista completa dei dipendenti per il filtro
    private var listaDipendentiCompleta = listOf<Dipendente>()
    
    private fun setupObservers() {
        // Osserva i dipendenti e aggiorna la UI
        viewModel.dipendenti.observe(viewLifecycleOwner, Observer { dipendenti ->
            listaDipendentiCompleta = dipendenti
            mostraDipendenti(dipendenti)
        })

        // Osserva lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBarTeam.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Osserva se la lista è vuota
        viewModel.isEmpty.observe(viewLifecycleOwner, Observer { isEmpty ->
            if (isEmpty) {
                mostraMessaggioVuoto()
            }
        })

        // Osserva gli errori
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                Log.e("TeamFragment", "Errore: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.resetError()
            }
        })
    }

    // Configura la ricerca
    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Non necessario
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Non necessario
            }
            
            override fun afterTextChanged(s: Editable?) {
                // Filtra i dipendenti in base al testo
                filtraDipendenti(s.toString())
            }
        })
    }
    
    // Filtra i dipendenti
    private fun filtraDipendenti(query: String) {
        if (query.isEmpty()) {
            // Se la ricerca è vuota, mostra tutti
            mostraDipendenti(listaDipendentiCompleta)
            return
        }
        
        val queryLowerCase = query.lowercase().trim()
        
        // Filtra per nome e cognome
        val dipendentiFiltrati = listaDipendentiCompleta.filter { dipendente ->
            dipendente.namelastname.lowercase().contains(queryLowerCase)
        }
        
        // Mostra i risultati o un messaggio di vuoto
        if (dipendentiFiltrati.isEmpty()) {
            mostraMessaggioRicercaVuota(query)
        } else {
            mostraDipendenti(dipendentiFiltrati)
        }
    }
    
    // Messaggio per ricerca senza risultati
    private fun mostraMessaggioRicercaVuota(query: String) {
        binding.teamMembersLayout.removeAllViews()
        val textViewEmpty = TextView(requireContext()).apply {
            text = "Nessun dipendente trovato per: '$query'"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(32, 32, 32, 32)
        }
        binding.teamMembersLayout.addView(textViewEmpty)
    }

    // Aggiorna la UI con i dipendenti raggruppati per settore
    private fun mostraDipendenti(dipendenti: List<Dipendente>) {
        // Pulisce il layout per evitare duplicati
        binding.teamMembersLayout.removeAllViews()

        if (dipendenti.isEmpty()) {
            // Se la lista è vuota, mostra un messaggio
            mostraMessaggioVuoto()
            return
        }

        Log.d("TeamFragment", "Mostrando ${dipendenti.size} dipendenti:")

        // Raggruppa i dipendenti per settore
        val dipendentiPerSettore = dipendenti.groupBy { it.settoreOccupazione.ifEmpty { "Non specificato" } }

        // Ordina i settori
        val settoriOrdinati = dipendentiPerSettore.keys.sorted()

        // Itera sui settori
        for (settore in settoriOrdinati) {
            // Aggiunge l'header del settore
            val headerSettore = creaHeaderSettore(settore)
            binding.teamMembersLayout.addView(headerSettore)

            // Aggiunge le card dei dipendenti
            val dipendentiSettore = dipendentiPerSettore[settore] ?: emptyList()
            for (dipendente in dipendentiSettore) {
                Log.d("TeamFragment", "Dipendente: ${dipendente.namelastname}, Settore: ${dipendente.settoreOccupazione}")
                val cardView = creaDipendenteCard(dipendente)
                binding.teamMembersLayout.addView(cardView)
            }
        }
    }
    
    // Crea l'header per un settore
    private fun creaHeaderSettore(nomeSettore: String): TextView {
        return TextView(requireContext()).apply {
            text = nomeSettore
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.purple_500))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 0, 16, 16)
            }
            setPadding(16, 8, 16, 8)
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, android.R.color.darker_gray).let { 
                    // Crea un colore più chiaro per lo sfondo
                    android.graphics.Color.argb(30, 
                        android.graphics.Color.red(it),
                        android.graphics.Color.green(it),
                        android.graphics.Color.blue(it))
                })
                cornerRadius = 8f
            }
        }
    }

    // Messaggio per lista vuota
    private fun mostraMessaggioVuoto() {
        binding.teamMembersLayout.removeAllViews()
        val textViewEmpty = TextView(requireContext()).apply {
            text = "Nessun dipendente trovato"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(32, 32, 32, 32)
        }
        binding.teamMembersLayout.addView(textViewEmpty)
    }

    // Crea la card del dipendente dal layout XML
    private fun creaDipendenteCard(dipendente: Dipendente): View {
        val cardView = LayoutInflater.from(context).inflate(R.layout.card_dipendente, binding.teamMembersLayout, false)

        // Trova le view nella card
        val nameTextView = cardView.findViewById<TextView>(R.id.textViewName)
        val sectorTextView = cardView.findViewById<TextView>(R.id.textViewSector)
        val emailTextView = cardView.findViewById<TextView>(R.id.textViewEmail)
        val birthDateTextView = cardView.findViewById<TextView>(R.id.textViewBirthDate)

        // Popola la card con i dati
        nameTextView.text = dipendente.namelastname
        sectorTextView.text = dipendente.settoreOccupazione.ifEmpty { "Settore non specificato" }
        emailTextView.text = dipendente.email
        birthDateTextView.text = dipendente.dataNascita

        // Click listener per inviare email
        emailTextView.setOnClickListener {
            mostraDialogEmail(dipendente.email, dipendente.namelastname)
        }

        return cardView
    }

    // Mostra il dialog per l'email
    private fun mostraDialogEmail(emailAddress: String, name: String) {
        // Crea il dialog
        val dialogBuilder = android.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_email, null)
        dialogBuilder.setView(dialogView)
        
        // Ottieni le view dal dialog
        val textViewDestinatario = dialogView.findViewById<TextView>(R.id.textViewDestinatario)
        val editTextOggetto = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextOggetto)
        val editTextMessaggio = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextMessaggio)
        val buttonInvia = dialogView.findViewById<Button>(R.id.buttonInvia)
        val buttonAnnulla = dialogView.findViewById<Button>(R.id.buttonAnnulla)
        
        // Imposta i dati iniziali
        textViewDestinatario.text = emailAddress
        editTextOggetto.setText("Messaggio per $name")
        
        // Crea il dialog
        val alertDialog = dialogBuilder.create()
        alertDialog.setCancelable(true)
        
        // Listener per il pulsante invia
        buttonInvia.setOnClickListener {
            val oggetto = editTextOggetto.text.toString()
            val messaggio = editTextMessaggio.text.toString()
            
            // Controlla se il messaggio è vuoto
            if (messaggio.isBlank()) {
                Toast.makeText(requireContext(), "Inserisci un messaggio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Invia email
            inviaEmail(emailAddress, oggetto, messaggio)
            
            // Chiude il dialog
            alertDialog.dismiss()
        }
        
        // Listener per il pulsante annulla
        buttonAnnulla.setOnClickListener {
            // Chiude il dialog senza fare nulla
            alertDialog.dismiss()
        }
        
        // Mostra il dialog
        alertDialog.show()
        
        // Imposta la dimensione del dialog
        val window = alertDialog.window
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    // Invia un'email al dipendente
    private fun inviaEmail(emailAddress: String, oggetto: String, messaggio: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // solo le app di email devono gestire questo intent
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, oggetto)
                putExtra(Intent.EXTRA_TEXT, messaggio)
            }
            
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
                Toast.makeText(
                    requireContext(),
                    "Apertura client email...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Nessuna app email trovata sul dispositivo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Errore nell'apertura del client email: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("TeamFragment", "Errore nell'apertura del client email", e)
        }
    }

    // Controlla se l'utente è admin e mostra il pulsante disponibilità
    private fun checkAdminAndShowAvailabilityButton() {
        // Carica il profilo utente per determinare il ruolo
        viewModel.caricaProfiloUtente()
        
        // Osserva il ruolo dell'utente
        viewModel.userRole.observe(viewLifecycleOwner) { ruolo ->
            if (ruolo == UserRole.ADMIN) {
                // Se è admin, aggiungi il pulsante disponibilità
                addAvailabilityButton()
            }
        }
    }
    
    // Aggiunge il pulsante per visualizzare le disponibilità (solo admin)
    private fun addAvailabilityButton() {
        try {
            // Crea il pulsante
            val btnAvailability = Button(requireContext())
            btnAvailability.text = "📋 Disponibilità Dipendenti"
            btnAvailability.setBackgroundColor(resources.getColor(R.color.admin_primary, null))
            btnAvailability.setTextColor(resources.getColor(R.color.white, null))
            btnAvailability.setOnClickListener {
                showEmployeeAvailability()
            }
            
            // Imposta l'ID per il pulsante
            btnAvailability.id = View.generateViewId()
            
            // Imposta i parametri del layout per ConstraintLayout
            val layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(16, 16, 16, 8)
            
            // Posiziona il pulsante sotto la barra di ricerca
            layoutParams.topToBottom = binding.searchLayout.id
            layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            
            btnAvailability.layoutParams = layoutParams
            
            // Aggiunge il pulsante al ConstraintLayout
            val constraintLayout = binding.root as androidx.constraintlayout.widget.ConstraintLayout
            constraintLayout.addView(btnAvailability)
            
            // Aggiorna i vincoli dello ScrollView per essere sotto il pulsante
            val scrollViewParams = binding.scrollViewTeam.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            scrollViewParams.topToBottom = btnAvailability.id
            binding.scrollViewTeam.layoutParams = scrollViewParams
            
        } catch (e: Exception) {
            // Se c'è un errore, non fare nulla per evitare crash
            Log.e("TeamFragment", "Errore nell'aggiungere il pulsante disponibilità", e)
        }
    }
    
    // Mostra le disponibilità di tutti i dipendenti
    private fun showEmployeeAvailability() {
        if (listaDipendentiCompleta.isNotEmpty()) {
            // Crea le disponibilità di esempio per tutti i dipendenti
            val disponibilitaList = mutableListOf<DisponibilitaDipendente>()
            
            for (dipendente in listaDipendentiCompleta) {
                val disponibilita = createSampleAvailability(dipendente.userId)
                
                val disponibilitaDipendente = DisponibilitaDipendente(
                    userId = dipendente.userId,
                    nomeCompleto = dipendente.namelastname,
                    email = dipendente.email,
                    disponibilita = disponibilita
                )
                
                disponibilitaList.add(disponibilitaDipendente)
            }
            
            // Mostra il dialog con le disponibilità
            showAvailabilityDialog(disponibilitaList)
        } else {
            Toast.makeText(requireContext(), "Nessun dipendente caricato", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Crea disponibilità di esempio diverse per ogni dipendente
    private fun createSampleAvailability(userId: String): Map<String, List<String>> {
        val hash = userId.hashCode()
        
        return when (hash % 4) {
            0 -> mapOf(
                "Lunedì" to listOf("09:00-13:00", "14:00-18:00"),
                "Martedì" to listOf("09:00-13:00", "14:00-18:00"),
                "Mercoledì" to listOf("09:00-13:00", "14:00-18:00"),
                "Giovedì" to listOf("09:00-13:00", "14:00-18:00"),
                "Venerdì" to listOf("09:00-13:00", "14:00-18:00")
            )
            1 -> mapOf(
                "Lunedì" to listOf("08:00-12:00"),
                "Martedì" to listOf("08:00-12:00"),
                "Mercoledì" to listOf("14:00-18:00"),
                "Giovedì" to listOf("08:00-12:00"),
                "Venerdì" to listOf("08:00-12:00"),
                "Sabato" to listOf("09:00-13:00")
            )
            2 -> mapOf(
                "Lunedì" to listOf("10:00-15:00"),
                "Martedì" to listOf("10:00-15:00"),
                "Mercoledì" to listOf("10:00-15:00"),
                "Giovedì" to listOf("10:00-15:00"),
                "Venerdì" to listOf("10:00-15:00"),
                "Domenica" to listOf("14:00-17:00")
            )
            else -> mapOf(
                "Lunedì" to listOf("07:00-15:00"),
                "Martedì" to listOf("07:00-15:00"),
                "Mercoledì" to listOf("13:00-21:00"),
                "Giovedì" to listOf("07:00-15:00"),
                "Venerdì" to listOf("07:00-15:00"),
                "Sabato" to listOf("08:00-16:00"),
                "Domenica" to listOf("10:00-18:00")
            )
        }
    }
    
    // Mostra il dialog con l'elenco delle disponibilità
    private fun showAvailabilityDialog(disponibilitaList: List<DisponibilitaDipendente>) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)
        
        val scrollView = android.widget.ScrollView(requireContext())
        val contentLayout = LinearLayout(requireContext())
        contentLayout.orientation = LinearLayout.VERTICAL
        
        // Titolo
        val titleText = TextView(requireContext())
        titleText.text = "Disponibilità Dipendenti (${disponibilitaList.size})"
        titleText.textSize = 18f
        titleText.setTypeface(null, Typeface.BOLD)
        titleText.setTextColor(resources.getColor(R.color.admin_primary, null))
        titleText.setPadding(0, 0, 0, 16)
        contentLayout.addView(titleText)
        
        // Crea una card per ogni dipendente
        for (dipendente in disponibilitaList) {
            val dipendenteCard = createAvailabilityCard(dipendente)
            contentLayout.addView(dipendenteCard)
        }
        
        scrollView.addView(contentLayout)
        layout.addView(scrollView)
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Elenco Disponibilità")
            .setView(layout)
            .setPositiveButton("Chiudi", null)
            .show()
    }
    
    // Crea una card per mostrare le disponibilità di un dipendente
    private fun createAvailabilityCard(dipendente: DisponibilitaDipendente): View {
        val cardLayout = LinearLayout(requireContext())
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(16, 16, 16, 16)
        cardLayout.setBackgroundColor(resources.getColor(R.color.card_background, null))
        
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 12)
        cardLayout.layoutParams = layoutParams
        
        // Nome dipendente
        val nameText = TextView(requireContext())
        nameText.text = "👤 ${dipendente.nomeCompleto}"
        nameText.textSize = 16f
        nameText.setTextColor(resources.getColor(R.color.admin_primary, null))
        nameText.setTypeface(null, Typeface.BOLD)
        cardLayout.addView(nameText)
        
        // Email dipendente
        val emailText = TextView(requireContext())
        emailText.text = "📧 ${dipendente.email}"
        emailText.textSize = 14f
        emailText.setTextColor(resources.getColor(R.color.gray, null))
        emailText.setPadding(0, 4, 0, 8)
        cardLayout.addView(emailText)
        
        // Disponibilità per giorno
        val availabilityTitle = TextView(requireContext())
        availabilityTitle.text = "📅 Disponibilità:"
        availabilityTitle.textSize = 15f
        availabilityTitle.setTypeface(null, Typeface.BOLD)
        availabilityTitle.setTextColor(resources.getColor(R.color.black, null))
        cardLayout.addView(availabilityTitle)
        
        for ((giorno, orari) in dipendente.disponibilita) {
            if (orari.isNotEmpty()) {
                val dayText = TextView(requireContext())
                dayText.text = "  • $giorno: ${orari.joinToString(", ")}"
                dayText.textSize = 13f
                dayText.setPadding(0, 2, 0, 2)
                dayText.setTextColor(resources.getColor(R.color.black, null))
                cardLayout.addView(dayText)
            }
        }
        
        return cardLayout
    }
}
