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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
            return
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
            user?.let {
                Log.d("AuthSuccess", "Utente autenticato: ${it.email}")
                
                // Controlla se il profilo esiste già
                Firebase.firestore.collection("Profili").document(it.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // L'utente esiste già, non sovrascrivere i dati
                            Log.d("Firestore", "Utente esistente, profilo non modificato")
                            startRoleSelectionActivity()
                        } else {
                            // Nuovo utente, crea il profilo con ruolo USER
                            val userData = hashMapOf(
                                "userId" to it.uid,
                                "email" to (it.email ?: ""),
                                "namelastname" to (it.displayName ?: ""),
                                "ruolo" to "USER"  // Solo per nuovi utenti
                            )
                            
                            Firebase.firestore.collection("Profili").document(it.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Nuovo profilo utente creato con successo")
                                    startRoleSelectionActivity()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Errore nel salvataggio del profilo", e)
                                    Toast.makeText(this, "Errore nel salvataggio del profilo", Toast.LENGTH_SHORT).show()
                                    startMainActivity() // Continua comunque all'app
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Errore nel controllo profilo esistente", e)
                        startMainActivity() // Continua comunque all'app
                    }
            } ?: run {
                // Se per qualche motivo user è null, avvia comunque l'app
                startMainActivity()
            }
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

    // Avvia la MainActivity
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Chiude la WelcomeActivity per evitare di tornare indietro
    }
    
    // Avvia la RoleSelectionActivity
    private fun startRoleSelectionActivity() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Chiude la WelcomeActivity per evitare di tornare indietro
    }
}
