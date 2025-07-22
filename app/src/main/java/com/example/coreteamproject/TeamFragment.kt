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

class TeamFragment : Fragment() {

    private lateinit var binding: FragmentTeamBinding
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

        // Imposta il ViewModel nel binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Osserva i cambiamenti nei dati
        setupObservers()

        // Carica i dipendenti
        viewModel.caricaDipendenti()

        return binding.root
    }

    private fun setupObservers() {
        // Osserva la lista dei dipendenti
        viewModel.dipendenti.observe(viewLifecycleOwner, Observer { dipendenti ->
            mostraDipendenti(dipendenti)
        })

        // Osserva lo stato di loading
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

    private fun mostraDipendenti(dipendenti: List<Dipendente>) {
        // Pulisce il layout prima di aggiungere nuovi elementi
        binding.teamMembersLayout.removeAllViews()

        if (dipendenti.isEmpty()) {
            mostraMessaggioVuoto()
            return
        }

        Log.d("TeamFragment", "Mostrando ${dipendenti.size} dipendenti:")

        // Crea una card per ogni dipendente
        for ((index, dipendente) in dipendenti.withIndex()) {
            Log.d("TeamFragment", "Dipendente: ${dipendente.namelastname}, Email: '${dipendente.email}'")
            val cardView = creaDipendenteCard(dipendente)
            binding.teamMembersLayout.addView(cardView)

            // Spazio tra le card
            if (index < dipendenti.size - 1) {
                val space = android.widget.Space(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        24 //
                    )
                }
                binding.teamMembersLayout.addView(space)
            }
        }
    }

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

    private fun creaDipendenteCard(dipendente: Dipendente): FrameLayout {
        // Creiamo il FrameLayout esterno che farà da bordo nero
        val frameLayout = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }

            // Impostiamo il background del FrameLayout come nero con bordi arrotondati
            background = GradientDrawable().apply {
                setColor(resources.getColor(android.R.color.black, null)) // Colore nero
                cornerRadius = 18f // Bordi arrotondati
            }

            // Aggiungiamo padding interno ridotto per bordo meno spesso
            setPadding(2, 2, 2, 2)
        }

        // Creiamo la CardView interna che ospiterà i contenuti
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
            setTextColor(ContextCompat.getColor(context, R.color.purple_500))
            setTypeface(null, Typeface.BOLD)
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
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, Typeface.BOLD)
        }

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

        // Aggiungi tutti i TextView al LinearLayout
        linearLayout.addView(textNameLastName)
        linearLayout.addView(textSettore)
        linearLayout.addView(textEmail)
        linearLayout.addView(textDataNascita)

        // Aggiungi il LinearLayout al CardView
        cardView.addView(linearLayout)

        // Aggiungi la CardView al FrameLayout per creare l'effetto bordo
        frameLayout.addView(cardView)

        return frameLayout
    }
}
