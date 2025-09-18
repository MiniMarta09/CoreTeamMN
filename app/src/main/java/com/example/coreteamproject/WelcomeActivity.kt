package com.example.coreteamproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Debug rapido per confermare avvio
        Toast.makeText(this, "WelcomeActivity start", Toast.LENGTH_SHORT).show()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Se l'utente è già loggato, vai direttamente alla selezione del ruolo
            startRoleSelectionActivity()
            return
        }

        signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { res -> onSignInResult(res) }

        Handler(Looper.getMainLooper()).postDelayed({
            startAuthentication()
        }, 2000)
    }

    private fun startAuthentication() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_CoreTeamProject)
            .setLogo(R.drawable.logonosfondo)
            .setAlwaysShowSignInMethodScreen(true)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                val firestore = Firebase.firestore
                firestore.collection("Profili").document(it.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        // SEZIONE MODIFICATA: Recupera il ruolo se il documento esiste
                        // Che il documento esista o meno, vai sempre alla selezione del ruolo
                        if (doc.exists()) {
                            startRoleSelectionActivity()
                        } else {
                            val data = hashMapOf(
                                "userId" to it.uid,
                                "email" to (it.email ?: ""),
                                "namelastname" to (it.displayName ?: ""),
                                "ruolo" to "USER"
                            )
                            firestore.collection("Profili").document(it.uid)
                                .set(data)
                                .addOnSuccessListener { startRoleSelectionActivity() } // Vai alla selezione del ruolo
                                .addOnFailureListener { startMainActivity() } // Fallback a MainActivity
                        }
                    }
                    .addOnFailureListener { startRoleSelectionActivity() }
            } ?: startRoleSelectionActivity()
        } else {
            if (response == null) finish()
            else Toast.makeText(this, "Login error: ${response.error?.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // QUESTA FUNZIONE NON È PIÙ USATA NEL FLUSSO MODIFICATO
    private fun startRoleSelectionActivity() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}