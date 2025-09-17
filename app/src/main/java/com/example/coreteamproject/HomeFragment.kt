package com.example.coreteamproject

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController

// Fragment per la gestione schermata home
class HomeFragment : Fragment() {
    
    private lateinit var viewModel: UsersViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]
        
        // Carica il profilo utente per ottenere il ruolo
        viewModel.caricaProfiloUtente()
        
        // Osserva il ruolo dell'utente
        viewModel.userRole.observe(viewLifecycleOwner, Observer { ruolo ->
            if (ruolo == UserRole.ADMIN) {
                mostraScrittaAdmin(view)
            }
        })

        // Imposta i listener per i bottoni di navigazione principali.
        // Ogni bottone naviga verso il rispettivo fragment utilizzando un'azione definita in nav_graph.xml.

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

        // Listener per il bottone "Bacheca".
        // Naviga al BoardFragment, che mostra la bacheca degli annunci.
        view.findViewById<Button>(R.id.buttonBacheca).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_boardFragment)
        }

        view.findViewById<Button>(R.id.buttonRequests).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_requestsFragment)
        }
    }
    
    private fun mostraScrittaAdmin(view: View) {
        // Trova il root layout (ConstraintLayout)
        val rootLayout = view as ConstraintLayout
        
        // Controlla se la scritta è già presente
        var adminTextExists = false
        for (i in 0 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i)
            if (child is TextView && child.text == "🔧 AMMINISTRATORE") {
                adminTextExists = true
                break
            }
        }
        
        if (!adminTextExists) {
            // Crea la scritta "AMMINISTRATORE"
            val adminTextView = TextView(requireContext()).apply {
                text = "🔧 AMMINISTRATORE"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                setPadding(16, 8, 16, 8)
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.background_light))
                id = View.generateViewId()
            }
            
            // Aggiunge la scritta in cima al layout
            rootLayout.addView(adminTextView)
            
            // Imposta i constraint per posizionarla in alto
            val layoutParams = adminTextView.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 16
            }
            adminTextView.layoutParams = layoutParams
            
            Log.d("HomeFragment", "Scritta AMMINISTRATORE aggiunta")
        }
    }
}