package com.example.coreteamproject

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
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

        // Richiede al ViewModel di caricare la lista dei dipendenti
        viewModel.caricaDipendenti()

        // Restituisce la root view del binding da visualizzare
        return binding.root
    }

    private fun setupObservers() {
        // Osserva la lista dei dipendenti per aggiornarla nell'interfaccia
        viewModel.dipendenti.observe(viewLifecycleOwner, Observer { dipendenti ->
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

    // Metodo che aggiorna la UI mostrando la lista di dipendenti come card personalizzate
    private fun mostraDipendenti(dipendenti: List<Dipendente>) {
        // Pulisce il layout container per evitare duplicati
        binding.teamMembersLayout.removeAllViews()

        if (dipendenti.isEmpty()) {
            // Se non ci sono dipendenti, mostra messaggio di lista vuota
            mostraMessaggioVuoto()
            return
        }

        Log.d("TeamFragment", "Mostrando ${dipendenti.size} dipendenti:")

        // Cicla ogni dipendente per creare e aggiungere una card nel layout
        for ((index, dipendente) in dipendenti.withIndex()) {
            Log.d("TeamFragment", "Dipendente: ${dipendente.namelastname}, Email: '${dipendente.email}'")
            val cardView = creaDipendenteCard(dipendente)
            binding.teamMembersLayout.addView(cardView)

            // Aggiunge uno spazio tra le card tranne dopo l'ultima
            if (index < dipendenti.size - 1) {
                val space = android.widget.Space(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        24 // altezza spazio in pixel
                    )
                }
                binding.teamMembersLayout.addView(space)
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
