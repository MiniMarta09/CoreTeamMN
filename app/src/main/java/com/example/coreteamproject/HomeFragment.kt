package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController

// Fragment per gestire la home
class HomeFragment : Fragment() {

    private lateinit var viewModel: UsersViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Chiamata al layout corrispondente della home
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]


        // Trova i TextView per il titolo e la descrizione
        val welcomeTextView = view.findViewById<TextView>(R.id.textViewHome)
        val descriptionTextView = view.findViewById<TextView>(R.id.textDescriptionHome)

        // Carica il profilo utente per ottenere il ruolo
        viewModel.caricaProfiloUtente()

        // Osserva il ruolo dell'utente per personalizzare i testi
        viewModel.userRole.observe(viewLifecycleOwner, Observer { ruolo ->
            // Controlla se l'utente è un admin
            if (ruolo == UserRole.ADMIN) {
                // Se l'utente è admin, personalizza sia il titolo che la descrizione
                welcomeTextView.text = getString(R.string.welcome_admin)
                descriptionTextView.text = getString(R.string.description_admin)
            } else {
                // Se l'utente è standard, personalizza solo il titolo
                // La descrizione rimane quella definita nel file di layout XML
                welcomeTextView.text = getString(R.string.welcome_user)
            }
        })

        // Imposta i listener per i bottoni di navigazione principali.
        view.findViewById<Button>(R.id.buttonDiary).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_diaryFragment)
        }

        view.findViewById<Button>(R.id.buttonEvent).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_eventFragment)
        }

        view.findViewById<Button>(R.id.buttonShift).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_shiftFragment)
        }

        view.findViewById<Button>(R.id.buttonTeam).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_teamFragment)
        }

        view.findViewById<Button>(R.id.buttonBacheca).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_boardFragment)
        }

        view.findViewById<Button>(R.id.buttonRequests).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_requestsFragment)
        }
    }

}