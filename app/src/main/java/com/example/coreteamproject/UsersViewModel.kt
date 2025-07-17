package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Data class per rappresentare un dipendente
data class Dipendente(
    val userId: String = "",
    val namelastname: String = "",
    val email: String = "",
    val dataNascita: String = "",
    val settoreOccupazione: String = ""
)

// ViewModel condiviso per gestire i dati degli utenti
class UsersViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // LiveData per la lista di tutti i dipendenti (per TeamFragment)
    private val _dipendenti = MutableLiveData<List<Dipendente>>()
    val dipendenti: LiveData<List<Dipendente>> = _dipendenti

    // LiveData per il profilo dell'utente corrente (per ProfileFragment)
    private val _currentUserProfile = MutableLiveData<Dipendente?>()
    val currentUserProfile: LiveData<Dipendente?> = _currentUserProfile

    // LiveData per lo stato di loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per gli errori
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData per conferma salvataggio
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // LiveData per indicare se non ci sono dipendenti
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Carica tutti i dipendenti (per TeamFragment)
    fun caricaDipendenti() {
        _isLoading.value = true
        _error.value = null

        db.collection("Profili")
            .get()
            .addOnSuccessListener { documents ->
                val listaDipendenti = mutableListOf<Dipendente>()

                for (document in documents) {
                    try {
                        val userId = document.getString("userId") ?: ""
                        val namelastname = document.getString("namelastname") ?: ""
                        val email = document.getString("email") ?: ""
                        val dataNascita = document.getString("dataNascita") ?: ""
                        val settoreOccupazione = document.getString("settoreOccupazione") ?: ""

                        // Log per debug
                        Log.d("UsersViewModel", "Documento ${document.id}:")
                        Log.d("UsersViewModel", "  - namelastname: '$namelastname'")
                        Log.d("UsersViewModel", "  - email: '$email'")
                        Log.d("UsersViewModel", "  - dataNascita: '$dataNascita'")
                        Log.d("UsersViewModel", "  - settoreOccupazione: '$settoreOccupazione'")

                        val dipendente = Dipendente(
                            userId = userId,
                            namelastname = namelastname,
                            email = email,
                            dataNascita = dataNascita,
                            settoreOccupazione = settoreOccupazione
                        )

                        listaDipendenti.add(dipendente)

                    } catch (e: Exception) {
                        Log.w("UsersViewModel", "Errore nel processare documento: ${document.id}", e)
                    }
                }

                _dipendenti.value = listaDipendenti
                _isEmpty.value = listaDipendenti.isEmpty()
                _isLoading.value = false

                Log.d("UsersViewModel", "Caricati ${listaDipendenti.size} dipendenti")
            }
            .addOnFailureListener { exception ->
                Log.w("UsersViewModel", "Errore nel caricamento dipendenti", exception)
                _error.value = "Errore nel caricamento dei dipendenti: ${exception.message}"
                _isLoading.value = false
            }
    }

    // Carica il profilo dell'utente corrente (per ProfileFragment)
    fun caricaProfiloUtente() {
        val user = auth.currentUser
        if (user != null) {
            _isLoading.value = true
            _error.value = null

            db.collection("Profili").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val dipendente = Dipendente(
                            userId = document.getString("userId") ?: user.uid,
                            namelastname = document.getString("namelastname") ?: (user.displayName ?: ""),
                            email = user.email ?: "",
                            dataNascita = document.getString("dataNascita") ?: "",
                            settoreOccupazione = document.getString("settoreOccupazione") ?: ""
                        )
                        _currentUserProfile.value = dipendente
                        Log.d("UsersViewModel", "Profilo caricato per: ${dipendente.namelastname}")
                    } else {
                        // Se non esiste il documento, crea un profilo vuoto con i dati base
                        val dipendente = Dipendente(
                            userId = user.uid,
                            namelastname = user.displayName ?: "",
                            email = user.email ?: "",
                            dataNascita = "",
                            settoreOccupazione = ""
                        )
                        _currentUserProfile.value = dipendente
                        Log.d("UsersViewModel", "Nessun profilo trovato, creato profilo base")
                    }
                    _isLoading.value = false
                }
                .addOnFailureListener { exception ->
                    Log.w("UsersViewModel", "Errore nel caricamento profilo", exception)
                    _error.value = "Errore nel caricamento del profilo: ${exception.message}"
                    _isLoading.value = false
                }
        }
    }

    // Salva il profilo dell'utente corrente (per ProfileFragment)
    fun salvaProfilo(dataNascita: String, password: String, settoreOccupazione: String) {
        val user = auth.currentUser
        if (user != null) {
            _isLoading.value = true
            _error.value = null

            val profiloDipendente = hashMapOf(
                "namelastname" to (user.displayName ?: ""),
                "dataNascita" to dataNascita,
                "email" to user.email,
                "password" to password,
                "settoreOccupazione" to settoreOccupazione,
                "userId" to user.uid
            )

            db.collection("Profili").document(user.uid)
                .set(profiloDipendente)
                .addOnSuccessListener {
                    Log.d("UsersViewModel", "Profilo salvato con successo!")
                    _saveSuccess.value = true
                    _isLoading.value = false

                    // Aggiorna anche il profilo corrente
                    val dipendenteAggiornato = Dipendente(
                        userId = user.uid,
                        namelastname = user.displayName ?: "",
                        email = user.email ?: "",
                        dataNascita = dataNascita,
                        settoreOccupazione = settoreOccupazione
                    )
                    _currentUserProfile.value = dipendenteAggiornato
                }
                .addOnFailureListener { e ->
                    Log.w("UsersViewModel", "Errore nel salvataggio", e)
                    _error.value = "Errore nel salvataggio: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    // Funzione per incrementare un punteggio (esempio dalle slide)
    fun incrementScore(isTeamA: Boolean) {
        // Esempio di implementazione per mantenere coerenza con le slide
        // Questa funzione può essere utilizzata per altre funzionalità
    }

    // Resetta il flag di successo del salvataggio
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // Resetta gli errori
    fun resetError() {
        _error.value = null
    }
}