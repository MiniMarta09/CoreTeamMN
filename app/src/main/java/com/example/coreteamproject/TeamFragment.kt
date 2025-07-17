package com.example.coreteamproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class TeamFragment : Fragment() {
    
    private lateinit var teamMembersLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: TeamViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_team, container, false)
        
        // Inizializza le view
        teamMembersLayout = view.findViewById(R.id.teamMembersLayout)
        progressBar = view.findViewById(R.id.progressBarTeam)
        
        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[TeamViewModel::class.java]
        
        // Osserva i cambiamenti nei dati
        setupObservers()
        
        // Carica i dipendenti
        viewModel.caricaDipendenti()
        
        return view
    }
    
    private fun setupObservers() {
        // Osserva la lista dei dipendenti
        viewModel.dipendenti.observe(viewLifecycleOwner) { dipendenti ->
            mostraDipendenti(dipendenti)
        }
        
        // Osserva lo stato di loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Osserva gli errori
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e("TeamFragment", "Errore: $error")
                // Qui potresti mostrare un Toast o un messaggio di errore
            }
        }
    }
    
    private fun mostraDipendenti(dipendenti: List<Dipendente>) {
        // Pulisce il layout prima di aggiungere nuovi elementi
        teamMembersLayout.removeAllViews()
        
        if (dipendenti.isEmpty()) {
            // Mostra messaggio se non ci sono dipendenti
            val textViewEmpty = TextView(requireContext()).apply {
                text = "Nessun dipendente trovato"
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(32, 32, 32, 32)
            }
            teamMembersLayout.addView(textViewEmpty)
            return
        }
        
        // Crea una card per ogni dipendente
        for (dipendente in dipendenti) {
            val cardView = creaDipendenteCard(dipendente)
            teamMembersLayout.addView(cardView)
        }
    }
    
    private fun creaDipendenteCard(dipendente: Dipendente): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            radius = 12f
            cardElevation = 6f
            setCardBackgroundColor(resources.getColor(android.R.color.white, null))
        }
        
        val linearLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }
        
        // TextView per il nome completo
        val textNameLastName = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            text = if (dipendente.namelastname.isNotEmpty()) dipendente.namelastname else "Nome non disponibile"
            textSize = 18f
            setTextColor(resources.getColor(R.color.purple_500, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        // TextView per il settore
        val textSettore = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            text = "Settore: ${dipendente.settoreOccupazione}"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
        
        // TextView per la data di nascita
        val textDataNascita = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Data di nascita: ${dipendente.dataNascita}"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
        
        // Aggiungi tutti i TextView al LinearLayout
        linearLayout.addView(textNameLastName)
        linearLayout.addView(textSettore)
        linearLayout.addView(textDataNascita)
        
        // Aggiungi il LinearLayout al CardView
        cardView.addView(linearLayout)
        
        return cardView
    }
}