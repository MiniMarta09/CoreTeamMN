package com.example.coreteamproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class RoleSelectionActivity : AppCompatActivity() {
    
    private lateinit var viewModel: UsersViewModel
    private var userActualRole: UserRole = UserRole.USER
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)
        
        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[UsersViewModel::class.java]
        
        // Carica il profilo utente per ottenere il ruolo reale
        viewModel.caricaProfiloUtente()
        
        // Trova i pulsanti
        val buttonDipendente = findViewById<Button>(R.id.buttonDipendente)
        val buttonCapo = findViewById<Button>(R.id.buttonCapo)

        // Disabilita i pulsanti finché il ruolo non è stato verificato
        buttonDipendente.isEnabled = false
        buttonCapo.isEnabled = false

        // Osserva il ruolo dell'utente
        viewModel.userRole.observe(this, Observer { ruolo ->
            userActualRole = ruolo
            Log.d("RoleSelection", "Ruolo utente caricato: $ruolo")

            // Ora che il ruolo è noto, riattiva i pulsanti
            buttonDipendente.isEnabled = true
            buttonCapo.isEnabled = true
        })
        
        // Imposta i listener per i bottoni
        setupButtonListeners(buttonDipendente, buttonCapo)
    }
    
    private fun setupButtonListeners(buttonDipendente: Button, buttonCapo: Button) {
        
        // Bottone Dipendente - Sempre accessibile
        buttonDipendente.setOnClickListener {
            Log.d("RoleSelection", "Selezionato: DIPENDENTE")
            procediConAccesso("DIPENDENTE")
        }
        
        // Bottone Capo - Solo per ADMIN
        buttonCapo.setOnClickListener {
            Log.d("RoleSelection", "Tentativo accesso: CAPO (Ruolo reale: $userActualRole)")
            
            if (userActualRole == UserRole.ADMIN) {
                // L'utente è effettivamente un admin, può accedere come capo
                procediConAccesso("CAPO")
            } else {
                // L'utente non è admin, mostra popup di errore
                mostraPopupPermessiNegati()
            }
        }
    }
    
    private fun procediConAccesso(ruoloSelezionato: String) {
        Log.d("RoleSelection", "Accesso consentito come: $ruoloSelezionato")
        
        // Prepara l'intent per la MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            // Aggiungi il ruolo selezionato come extra
            // Usiamo "ADMIN" o "USER" per coerenza con il resto dell'app
            val finalRole = if (ruoloSelezionato == "CAPO") "ADMIN" else "USER"
            putExtra("USER_ROLE", finalRole)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Avvia la MainActivity
        startActivity(intent)
        finish()
    }
    
    private fun mostraPopupPermessiNegati() {
        AlertDialog.Builder(this)
            .setTitle("Accesso Negato")
            .setMessage("Non hai i permessi per accedere come CAPO.\n\nPer ottenere i privilegi di amministratore, contatta il tuo supervisore.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        
        Log.d("RoleSelection", "Popup permessi negati mostrato")
    }
}
