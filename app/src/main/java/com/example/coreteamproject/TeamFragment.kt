package com.example.coreteamproject

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

// Fragment per la visualizzazione della lista di dipendenti
class TeamFragment : Fragment() {

    // Binding per il layout del fragment
    private lateinit var binding: FragmentTeamBinding

    // ViewModel per gestire i dati degli utenti
    private lateinit var viewModel: UsersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding collegando layout e fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_team, container, false
        )

        // Inizializza il ViewModel per il ciclo di vita di questo fragment
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]

        // Associa il ViewModel al binding per usarlo nel layout (es. binding.viewModel)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this // Lifecycle owner per LiveData

        // Imposta gli observer per aggiornare UI in base ai dati e stati del ViewModel
        setupObservers()
        
        // Imposta il listener per la barra di ricerca
        setupSearchListener()

        // Richiede al ViewModel di caricare la lista dei dipendenti
        viewModel.caricaDipendenti()

        // Restituisce la root view del binding da visualizzare
        return binding.root
    }

    // Lista originale di dipendenti, usata per il filtraggio
    private var listaDipendentiCompleta = listOf<Dipendente>()
    
    private fun setupObservers() {
        // Osserva la lista dei dipendenti per aggiornarla nell'interfaccia
        viewModel.dipendenti.observe(viewLifecycleOwner, Observer { dipendenti ->
            listaDipendentiCompleta = dipendenti
            mostraDipendenti(dipendenti)
        })

        // Osserva lo stato di caricamento per mostrare o nascondere la progress bar
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBarTeam.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Osserva se la lista è vuota per mostrare un messaggio appropriato
        viewModel.isEmpty.observe(viewLifecycleOwner, Observer { isEmpty ->
            if (isEmpty) {
                mostraMessaggioVuoto()
            }
        })

        // Osserva eventuali errori e li mostra come Toast, poi li resetta
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                Log.e("TeamFragment", "Errore: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.resetError()
            }
        })
    }

    // Configura il listener per la barra di ricerca
    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Non necessario
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Non necessario
            }
            
            override fun afterTextChanged(s: Editable?) {
                // Filtra la lista ogni volta che cambia il testo
                filtraDipendenti(s.toString())
            }
        })
    }
    
    // Filtra i dipendenti in base al testo di ricerca
    private fun filtraDipendenti(query: String) {
        if (query.isEmpty()) {
            // Se la query è vuota, mostra tutti i dipendenti
            mostraDipendenti(listaDipendentiCompleta)
            return
        }
        
        val queryLowerCase = query.lowercase().trim()
        
        // Filtra i dipendenti il cui nome/cognome contiene la query
        val dipendentiFiltrati = listaDipendentiCompleta.filter { dipendente ->
            dipendente.namelastname.lowercase().contains(queryLowerCase)
        }
        
        // Mostra i dipendenti filtrati o un messaggio se non ce ne sono
        if (dipendentiFiltrati.isEmpty()) {
            mostraMessaggioRicercaVuota(query)
        } else {
            mostraDipendenti(dipendentiFiltrati)
        }
    }
    
    // Mostra un messaggio quando la ricerca non produce risultati
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

    // Metodo che aggiorna la UI mostrando la lista di dipendenti raggruppati per settore
    private fun mostraDipendenti(dipendenti: List<Dipendente>) {
        // Pulisce il layout container per evitare duplicati
        binding.teamMembersLayout.removeAllViews()

        if (dipendenti.isEmpty()) {
            // Se non ci sono dipendenti, mostra messaggio di lista vuota
            mostraMessaggioVuoto()
            return
        }

        Log.d("TeamFragment", "Mostrando ${dipendenti.size} dipendenti:")
        
        // Raggruppa i dipendenti per settore
        val dipendentiPerSettore = dipendenti.groupBy { it.settoreOccupazione.ifEmpty { "Non specificato" } }
        
        // Ordina i settori alfabeticamente
        val settoriOrdinati = dipendentiPerSettore.keys.sorted()
        
        // Per ogni settore, visualizza intestazione e dipendenti
        for (settore in settoriOrdinati) {
            // Aggiungi intestazione del settore
            val headerSettore = creaHeaderSettore(settore)
            binding.teamMembersLayout.addView(headerSettore)
            
            // Aggiungi i dipendenti di questo settore
            val dipendentiSettore = dipendentiPerSettore[settore] ?: emptyList()
            for ((index, dipendente) in dipendentiSettore.withIndex()) {
                Log.d("TeamFragment", "Dipendente: ${dipendente.namelastname}, Settore: ${dipendente.settoreOccupazione}")
                val cardView = creaDipendenteCard(dipendente)
                binding.teamMembersLayout.addView(cardView)

                // Aggiunge uno spazio tra le card tranne dopo l'ultima del settore
                if (index < dipendentiSettore.size - 1) {
                    val space = android.widget.Space(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            16 // altezza spazio in pixel
                        )
                    }
                    binding.teamMembersLayout.addView(space)
                }
            }
            
            // Aggiunge spazio maggiore tra i gruppi di settore
            val sectorSpace = android.widget.Space(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    40 // altezza spazio in pixel
                )
            }
            binding.teamMembersLayout.addView(sectorSpace)
        }
    }
    
    // Crea l'intestazione per un settore
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

    // Mostra un messaggio quando la lista dei dipendenti è vuota
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

    // Crea una card personalizzata per visualizzare i dati di un singolo dipendente
    private fun creaDipendenteCard(dipendente: Dipendente): FrameLayout {
        // FrameLayout esterno con bordo nero arrotondato
        val frameLayout = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }

            // Background nero con angoli arrotondati
            background = GradientDrawable().apply {
                setColor(resources.getColor(android.R.color.black, null))
                cornerRadius = 18f
            }

            setPadding(2, 2, 2, 2) // Padding per il bordo
        }

        // CardView interna bianca con ombra e angoli arrotondati
        val cardView = CardView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(3, 3, 3, 3)
            }
            radius = 12f
            cardElevation = 6f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }

        // LinearLayout verticale per contenere i testi
        val linearLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        // TextView per il nome e cognome
        val textNameLastName = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            text = if (dipendente.namelastname.isNotEmpty()) dipendente.namelastname else "Nome non disponibile"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.purple_500))
            setTypeface(null, Typeface.BOLD)
        }

        // TextView per il settore di occupazione
        val textSettore = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            text = "Settore: ${dipendente.settoreOccupazione}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, Typeface.BOLD)
        }

        // TextView per l'email
        val textEmail = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            text = "Email: ${dipendente.email}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, Typeface.BOLD)
        }

        // TextView per la data di nascita
        val textDataNascita = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Data di nascita: ${dipendente.dataNascita}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, Typeface.BOLD)
        }

        // Aggiunge tutte le TextView al layout verticale
        linearLayout.addView(textNameLastName)
        linearLayout.addView(textSettore)
        linearLayout.addView(textEmail)
        linearLayout.addView(textDataNascita)

        // Aggiunge il layout al CardView
        cardView.addView(linearLayout)

        // Aggiunge il CardView al FrameLayout per creare l'effetto bordo nero arrotondato
        frameLayout.addView(cardView)

        // Ritorna la view completa pronta per essere inserita nel layout principale
        return frameLayout
    }
}
