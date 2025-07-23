package com.example.coreteamproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    // Launcher per gestire il risultato dell'attività di login FirebaseUI
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Controlla se l'utente è già loggato tramite FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Utente già autenticato: mostra il logo per 2 secondi e poi avvia MainActivity
            Handler(Looper.getMainLooper()).postDelayed({
                startMainActivity()
            }, 2000) // 2000 millisecondi = 2 secondi
            return // Non proseguire oltre perché utente già loggato
        }

        // Registra il launcher per ricevere il risultato dell'autenticazione
        signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { res ->
            onSignInResult(res) // Gestisce il risultato dell'autenticazione
        }

        // Mostra il logo per 2 secondi, poi avvia la procedura di autenticazione
        Handler(Looper.getMainLooper()).postDelayed({
            startAuthentication()
        }, 2000)
    }

    // Avvia la schermata di login usando FirebaseUI con Email e Google come provider
    private fun startAuthentication() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),   // Login con email e password
            AuthUI.IdpConfig.GoogleBuilder().build()   // Login con account Google
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)          // Provider di login supportati
            .setTheme(R.style.Theme_CoreTeamProject)   // Tema personalizzato per UI login
            .setLogo(R.drawable.logonosfondo)           // Logo da mostrare nella schermata login
            .setAlwaysShowSignInMethodScreen(true)     // Mostra sempre la scelta del metodo di login
            .build()

        // Lancia l'attività di login e aspetta il risultato
        signInLauncher.launch(signInIntent)
    }

    // Gestisce il risultato dell'attività di login FirebaseUI
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Login effettuato con successo
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("AuthSuccess", "Utente autenticato: ${user?.email}")
            startMainActivity() // Avvia la MainActivity
        } else {
            // Login fallito o annullato dall'utente
            if (response == null) {
                // L'utente ha annullato il login
                Toast.makeText(this, "Accesso annullato", Toast.LENGTH_SHORT).show()
                finish() // Chiude l'activity (esci dall'app o torna indietro)
            } else {
                // Errore durante il login
                val errorCode = response.error?.errorCode
                val errorMessage = response.error?.message ?: "Errore sconosciuto"
                Toast.makeText(this, "Errore: $errorMessage", Toast.LENGTH_LONG).show()
                Log.e("AuthError", "Codice: $errorCode, Messaggio: $errorMessage")

            }
        }
    }

    // Avvia la MainActivity e chiude WelcomeActivity
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
