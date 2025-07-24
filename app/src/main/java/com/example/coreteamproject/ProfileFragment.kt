package com.example.coreteamproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.coreteamproject.databinding.FragmentProfileBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

// Fragment per la visualizzazione e modifica del profilo utente
class ProfileFragment : Fragment() {

    // Binding per accedere aile componenti del layout
    private lateinit var binding: FragmentProfileBinding

    // ViewModel associato per gestire i dati e la logica
    private lateinit var viewModel: UsersViewModel

    // Flag che indica se l'utente è in modalità modifica
    private var isEditing = false

    // Lista dei settori lavorativi disponibili per lo spinner
    private val settoriOccupazione = arrayOf(
        "Seleziona settore",
        "Contabilità e amministrazione",
        "Magazzino e Logistica",
        "Vendite",
        "Risorse Umane",
        "Assistenza Clienti"
    )

    // Metodo principale del Fragment, eseguito quando viene creato il layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il binding per il layout XML
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_profile, container, false
        )

        // Ottiene il ViewModel associato a questo Fragment
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]

        // Collega il ViewModel al layout
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Imposta lo spinner (menu a tendina)
        setupSpinner()

        // Imposta gli observer per aggiornare la UI
        setupObservers()

        // Visualizza i dati base dell'utente Firebase
        setupUserData()

        // Collega i listener ai pulsanti "Modifica" e "Salva"
        setupClickListeners()

        // Richiede il caricamento del profilo utente dal ViewModel
        viewModel.caricaProfiloUtente()

        // Ritorna la vista associata al Fragment
        return binding.root
    }

    // Configura lo spinner con i settori lavorativi
    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, settoriOccupazione)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSettore.adapter = adapter
        binding.spinnerSettore.isEnabled = false // inizialmente disabilitato
    }

    // Configura gli observer per i LiveData nel ViewModel
    private fun setupObservers() {
        // Osserva il profilo utente e aggiorna i campi se non è nullo
        viewModel.currentUserProfile.observe(viewLifecycleOwner, Observer { dipendente ->
            if (dipendente != null) {
                aggiornaCampiProfilo(dipendente)
            }
        })

        // Mostra o nasconde la progress bar in base allo stato di loading
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBarProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Mostra un messaggio di errore se presente
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.resetError()
            }
        })

        // Mostra messaggio di successo al salvataggio e disabilita la modifica
        viewModel.saveSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Dati salvati!", Toast.LENGTH_SHORT).show()
                disabilitaModifica()
                viewModel.resetSaveSuccess()
            }
        })
    }

    // Visualizza il nome e l'email dell'utente loggato
    private fun setupUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.textNamelastname.text = user.displayName ?: "Non disponibile"
            binding.textEmail.text = user.email ?: "Non disponibile"
        }
    }

    // Configura i click dei pulsanti "Modifica" e "Salva"
    private fun setupClickListeners() {

        binding.btnModifica.setOnClickListener {
            if (!isEditing) {
                abilitaModifica()
            } else {
                disabilitaModifica()
            }
        }

        // Salva i dati quando si preme su "Salva"
        binding.btnSalva.setOnClickListener {
            salvaDatiProfilo()
        }
    }

    // Aggiorna i campi del profilo utente con i dati ricevuti
    private fun aggiornaCampiProfilo(dipendente: Dipendente) {
        binding.editDataNascita.setText(dipendente.dataNascita)
        binding.editPassword.setText("") // per sicurezza, non si mostra la password

        // Se il settore è presente, seleziona il valore corretto nello spinner
        if (dipendente.settoreOccupazione.isNotEmpty()) {
            val position = settoriOccupazione.indexOf(dipendente.settoreOccupazione)
            if (position != -1) {
                binding.spinnerSettore.setSelection(position)
            }
        }
    }

    // Abilita i campi per modificare il profilo
    private fun abilitaModifica() {
        isEditing = true
        binding.editDataNascita.isEnabled = true
        binding.editPassword.isEnabled = true
        binding.spinnerSettore.isEnabled = true
        binding.btnSalva.visibility = View.VISIBLE
        binding.btnModifica.text = "Annulla"
    }

    // Disabilita la modalità modifica e ripristina lo stato iniziale
    private fun disabilitaModifica() {
        isEditing = false
        binding.editDataNascita.isEnabled = false
        binding.editPassword.isEnabled = false
        binding.spinnerSettore.isEnabled = false
        binding.btnSalva.visibility = View.GONE
        binding.btnModifica.text = "Modifica Profilo"
    }

    // Legge i dati dai campi e invia la richiesta di salvataggio al ViewModel
    private fun salvaDatiProfilo() {
        val dataNascita = binding.editDataNascita.text.toString()
        val password = binding.editPassword.text.toString()
        val settoreSelezionato = binding.spinnerSettore.selectedItem.toString()

        // Controlla che sia stato selezionato un settore valido
        if (settoreSelezionato == "Seleziona settore") {
            Toast.makeText(requireContext(), "Seleziona un settore di occupazione", Toast.LENGTH_SHORT).show()
            return
        }

        // Passa i dati al ViewModel per il salvataggio
        viewModel.salvaProfilo(dataNascita, password, settoreSelezionato)
    }
}
