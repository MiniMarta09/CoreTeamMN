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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        // Controlla il ruolo dell'utente
        loadUserProfile()

        // Restituisce la view
        return binding.root
    }

    // Lista completa dei dipendenti per il filtro
    private var listaDipendentiCompleta = listOf<Dipendente>()
    private var isAdmin: Boolean = false
    
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

        // Determina il colore dell'header in base al ruolo
        val headerColor = if (isAdmin) R.color.admin_primary else R.color.purple_500

        // Itera sui settori
        for (settore in settoriOrdinati) {
            // Aggiunge l'header del settore con il colore corretto
            val headerSettore = creaHeaderSettore(settore, headerColor)
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
    
    // Crea l'header per un settore con un colore specifico
    private fun creaHeaderSettore(nomeSettore: String, colorRes: Int): TextView {
        return TextView(requireContext()).apply {
            text = nomeSettore
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, colorRes))
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

    // Carica il profilo utente per determinare il ruolo
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

    // Applica i colori della UI per la vista Admin
    private fun updateUiForAdmin() {
        val adminPrimaryColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        val adminVariantColor = ContextCompat.getColor(requireContext(), R.color.admin_primary_variant)

        // Cambia colore a titolo e sottotitolo
        binding.textViewTeam.setTextColor(adminPrimaryColor)
        binding.textDescriptionTeam.setTextColor(adminVariantColor)

        // Cambia colore alla barra di ricerca
        binding.searchLayout.boxStrokeColor = adminPrimaryColor
        binding.searchLayout.hintTextColor = android.content.res.ColorStateList.valueOf(adminPrimaryColor)
        binding.searchLayout.startIconDrawable?.setTint(adminPrimaryColor)

        // Ricarica la lista per applicare il colore agli header
        mostraDipendenti(listaDipendentiCompleta)
    }
}
