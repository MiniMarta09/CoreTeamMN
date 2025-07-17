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

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var viewModel: UsersViewModel
    private var isEditing = false

    // Opzioni per il settore di occupazione
    private val settoriOccupazione = arrayOf(
        "Seleziona settore",
        "Contabilità e amministrazione",
        "Magazzino e Logistica",
        "Vendite",
        "Risorse Umane",
        "Assistenza Clienti"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_profile, container, false
        )

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]

        // Imposta il ViewModel nel binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Configura lo spinner
        setupSpinner()

        // Configura gli observer
        setupObservers()

        // Mostra i dati base dell'utente
        setupUserData()

        // Configura i listener dei pulsanti
        setupClickListeners()

        // Carica il profilo dell'utente
        viewModel.caricaProfiloUtente()

        return binding.root
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, settoriOccupazione)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSettore.adapter = adapter
        binding.spinnerSettore.isEnabled = false
    }

    private fun setupObservers() {
        // Osserva il profilo dell'utente corrente
        viewModel.currentUserProfile.observe(viewLifecycleOwner, Observer { dipendente ->
            if (dipendente != null) {
                aggiornaCampiProfilo(dipendente)
            }
        })

        // Osserva lo stato di loading
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBarProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Osserva gli errori
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.resetError()
            }
        })

        // Osserva il successo del salvataggio
        viewModel.saveSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Dati salvati!", Toast.LENGTH_SHORT).show()
                disabilitaModifica()
                viewModel.resetSaveSuccess()
            }
        })
    }

    private fun setupUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.textNamelastname.text = user.displayName ?: "Non disponibile"
            binding.textEmail.text = user.email ?: "Non disponibile"
        }
    }

    private fun setupClickListeners() {
        // Pulsante Modifica Profilo
        binding.btnModifica.setOnClickListener {
            if (!isEditing) {
                abilitaModifica()
            } else {
                disabilitaModifica()
            }
        }

        // Pulsante Salva
        binding.btnSalva.setOnClickListener {
            salvaDatiProfilo()
        }

        // Pulsante Esci
        binding.btnLogout.setOnClickListener {
            AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener {
                    val intent = Intent(requireContext(), WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
        }
    }

    private fun aggiornaCampiProfilo(dipendente: Dipendente) {
        binding.editDataNascita.setText(dipendente.dataNascita)
        // Non mostrare la password per sicurezza
        binding.editPassword.setText("")

        // Imposta il settore di occupazione
        if (dipendente.settoreOccupazione.isNotEmpty()) {
            val position = settoriOccupazione.indexOf(dipendente.settoreOccupazione)
            if (position != -1) {
                binding.spinnerSettore.setSelection(position)
            }
        }
    }

    private fun abilitaModifica() {
        isEditing = true

        // Abilita i campi editabili
        binding.editDataNascita.isEnabled = true
        binding.editPassword.isEnabled = true
        binding.spinnerSettore.isEnabled = true

        // Mostra il pulsante Salva
        binding.btnSalva.visibility = View.VISIBLE
        binding.btnModifica.text = "Annulla"
    }

    private fun disabilitaModifica() {
        isEditing = false

        // Disabilita i campi
        binding.editDataNascita.isEnabled = false
        binding.editPassword.isEnabled = false
        binding.spinnerSettore.isEnabled = false

        // Nascondi il pulsante Salva
        binding.btnSalva.visibility = View.GONE
        binding.btnModifica.text = "Modifica Profilo"
    }

    private fun salvaDatiProfilo() {
        val dataNascita = binding.editDataNascita.text.toString()
        val password = binding.editPassword.text.toString()
        val settoreSelezionato = binding.spinnerSettore.selectedItem.toString()

        // Verifica che sia stato selezionato un settore valido
        if (settoreSelezionato == "Seleziona settore") {
            Toast.makeText(requireContext(), "Seleziona un settore di occupazione", Toast.LENGTH_SHORT).show()
            return
        }

        // Salva tramite il ViewModel
        viewModel.salvaProfilo(dataNascita, password, settoreSelezionato)
    }
}