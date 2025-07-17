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

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Controlla se l'utente è già autenticato
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Utente già autenticato, vai direttamente alla MainActivity dopo aver mostrato il logo
            Handler(Looper.getMainLooper()).postDelayed({
                startMainActivity()
            }, 2000) // 2 secondi
            return
        }

        // Inizializza il launcher per FirebaseUI
        signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { res ->
            onSignInResult(res)
        }

        // Mostra il logo per 2 secondi, poi procedi con l'autenticazione
        Handler(Looper.getMainLooper()).postDelayed({
            startAuthentication()
        }, 2000) // 2 secondi
    }

    private fun startAuthentication() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        // Crea e lancia l'intento di login con configurazioni migliorate
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_CoreTeamProject)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("AuthSuccess", "Utente autenticato: ${user?.email}")
            startMainActivity()
        } else {
            // Accesso fallito o annullato
            if (response == null) {
                Toast.makeText(this, "Accesso annullato", Toast.LENGTH_SHORT).show()
                // Opzionale: riprova o chiudi l'app
                finish()
            } else {
                val errorCode = response.error?.errorCode
                val errorMessage = response.error?.message ?: "Errore sconosciuto"
                Toast.makeText(this, "Errore: $errorMessage", Toast.LENGTH_LONG).show()
                Log.e("AuthError", "Codice: $errorCode, Messaggio: $errorMessage")

                // Gestisci errori specifici
                when (errorCode) {
                    // Puoi gestire errori specifici qui se necessario
                    else -> {
                        // Per altri errori, potresti voler riprovare o chiudere
                    }
                }
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}