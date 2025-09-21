package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
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

        // Osserva il ruolo dell'utente per personalizzare i testi e colori
        viewModel.userRole.observe(viewLifecycleOwner, Observer { ruolo ->
            // Controlla se l'utente è un admin
            if (ruolo == UserRole.ADMIN) {
                // Se l'utente è admin, personalizza sia il titolo che la descrizione
                welcomeTextView.text = getString(R.string.welcome_admin)
                descriptionTextView.text = getString(R.string.description_admin)
                // Applica i colori admin
                applyAdminColors(view)
            } else {
                // Se l'utente è standard, personalizza solo il titolo
                // La descrizione rimane quella definita nel file di layout XML
                welcomeTextView.text = getString(R.string.welcome_user)
                // Applica i colori standard
                applyUserColors(view)
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

    // Applica i colori per gli amministratori
    private fun applyAdminColors(view: View) {
        val adminColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        val adminDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.round_button_admin)
        
        // Aggiorna il colore del titolo principale
        view.findViewById<TextView>(R.id.textViewHome).setTextColor(adminColor)
        
        // Applica il background admin a tutti i bottoni
        updateButtonBackgrounds(view, adminDrawable)
        
        // Trova e aggiorna tutti i TextView con il colore admin
        updateTextViewColors(view, adminColor)
    }

    // Applica i colori standard per gli utenti
    private fun applyUserColors(view: View) {
        val userColor = ContextCompat.getColor(requireContext(), R.color.purple_500)
        val userDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.round_button)
        
        // Aggiorna il colore del titolo principale
        view.findViewById<TextView>(R.id.textViewHome).setTextColor(userColor)
        
        // Applica il background standard a tutti i bottoni
        updateButtonBackgrounds(view, userDrawable)
        
        // Aggiorna tutti i TextView con il colore viola standard
        updateTextViewColors(view, userColor)
    }

    // Metodo helper per aggiornare i colori dei TextView
    private fun updateTextViewColors(view: View, color: Int) {
        // Lista degli ID dei layout che contengono i TextView da aggiornare
        val layoutIds = listOf(
            R.id.diaryLayout,
            R.id.eventLayout,
            R.id.shiftLayout,
            R.id.teamLayout,
            R.id.bachecaLayout,
            R.id.requestsLayout
        )
        
        // Per ogni layout, trova il TextView figlio e aggiorna il colore
        layoutIds.forEach { layoutId ->
            val layout = view.findViewById<View>(layoutId)
            if (layout is ViewGroup) {
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is TextView) {
                        child.setTextColor(color)
                    }
                }
            }
        }
    }

    // Metodo helper per aggiornare i background dei bottoni
    private fun updateButtonBackgrounds(view: View, drawable: android.graphics.drawable.Drawable?) {
        // Lista degli ID dei bottoni da aggiornare
        val buttonIds = listOf(
            R.id.buttonDiary,
            R.id.buttonEvent,
            R.id.buttonShift,
            R.id.buttonTeam,
            R.id.buttonBacheca,
            R.id.buttonRequests
        )
        
        // Per ogni bottone, aggiorna il background
        buttonIds.forEach { buttonId ->
            val button = view.findViewById<Button>(buttonId)
            button?.background = drawable
        }
    }

}