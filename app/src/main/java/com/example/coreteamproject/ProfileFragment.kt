package com.example.coreteamproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    // Inizializza Firestore come dice il prof
    private val db = Firebase.firestore

    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var editDataNascita: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnModifica: Button
    private lateinit var btnSalva: Button
    private lateinit var btnLogout: Button

    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Collega i campi
        textName = view.findViewById(R.id.text_name)
        textEmail = view.findViewById(R.id.text_email)
        editDataNascita = view.findViewById(R.id.edit_data_nascita)
        editPassword = view.findViewById(R.id.edit_password)
        btnModifica = view.findViewById(R.id.btn_modifica)
        btnSalva = view.findViewById(R.id.btn_salva)
        btnLogout = view.findViewById(R.id.btn_logout)

        // Mostra i dati dell'utente
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Display only the name without any welcome message
            textName.text = user.displayName?.split(" ")?.firstOrNull() ?: "Non disponibile"
            textEmail.text = user.email ?: "Non disponibile"

            // Carica i dati dal database
            leggiDatiProfilo(user.uid)
        }

        // Pulsante Modifica Profilo
        btnModifica.setOnClickListener {
            if (!isEditing) {
                abilitaModifica()
            }
        }

        // Pulsante Salva
        btnSalva.setOnClickListener {
            salvaDatiProfilo()
        }

        // Pulsante Esci
        btnLogout.setOnClickListener {
            AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener {
                    val intent = Intent(requireContext(), WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
        }

        return view
    }

    private fun abilitaModifica() {
        isEditing = true

        // Abilita i campi editabili
        editDataNascita.isEnabled = true
        editPassword.isEnabled = true

        // Mostra il pulsante Salva
        btnSalva.visibility = View.VISIBLE
        btnModifica.text = "Annulla"

        // Cambia il comportamento del pulsante Modifica
        btnModifica.setOnClickListener {
            disabilitaModifica()
        }
    }

    private fun disabilitaModifica() {
        isEditing = false

        // Disabilita i campi
        editDataNascita.isEnabled = false
        editPassword.isEnabled = false

        // Nascondi il pulsante Salva
        btnSalva.visibility = View.GONE
        btnModifica.text = "Modifica Profilo"

        // Ripristina il comportamento del pulsante Modifica
        btnModifica.setOnClickListener {
            abilitaModifica()
        }
    }

    private fun salvaDatiProfilo() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val dataNascita = editDataNascita.text.toString()
            val password = editPassword.text.toString()

            // Crea un oggetto con i dati come nell'esempio del prof
            val profiloDipendente = hashMapOf(
                "dataNascita" to dataNascita,
                "password" to password,
                "userId" to user.uid
            )

            // Salva usando il metodo set() come dice il prof
            db.collection("Profili").document(user.uid)
                .set(profiloDipendente)
                .addOnSuccessListener {
                    Log.d("ProfileFragment", "Profilo salvato con successo!")
                    Toast.makeText(requireContext(), "Dati salvati!", Toast.LENGTH_SHORT).show()
                    disabilitaModifica()
                }
                .addOnFailureListener { e ->
                    Log.w("ProfileFragment", "Errore nel salvataggio", e)
                    Toast.makeText(requireContext(), "Errore nel salvataggio", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun leggiDatiProfilo(userId: String) {
        // Leggi i dati come dice il prof
        db.collection("Profili").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d("ProfileFragment", "Dati trovati: ${document.data}")
                    editDataNascita.setText(document.getString("dataNascita") ?: "")
                    editPassword.setText(document.getString("password") ?: "")
                } else {
                    Log.d("ProfileFragment", "Nessun documento trovato")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("ProfileFragment", "Errore nel leggere i dati", exception)
            }
    }
}