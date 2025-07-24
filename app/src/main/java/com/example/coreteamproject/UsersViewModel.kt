package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Data class che rappresenta un dipendente con i relativi dati personali e lavorativi
data class Dipendente(
    val userId: String = "",            // ID univoco utente (da Firebase Auth)
    val namelastname: String = "",      // Nome e cognome del dipendente
    val email: String = "",             // Email dell'utente
    val dataNascita: String = "",       // Data di nascita in formato stringa
    val settoreOccupazione: String = "" // Settore lavorativo del dipendente
)

// ViewModel per gestire dati e stato relativi agli utenti
class UsersViewModel : ViewModel() {

    private val db = Firebase.firestore                    // Riferimento a Firestore
    private val auth = FirebaseAuth.getInstance()          // Riferimento all'autenticazione Firebase

    // LiveData che espone la lista di tutti i dipendenti
    private val _dipendenti = MutableLiveData<List<Dipendente>>()
    val dipendenti: LiveData<List<Dipendente>> = _dipendenti

    // LiveData che espone il profilo dell'utente autenticato
    private val _currentUserProfile = MutableLiveData<Dipendente?>()
    val currentUserProfile: LiveData<Dipendente?> = _currentUserProfile

    // LiveData che indica se è in corso un caricamento dati
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per comunicare eventuali errori all'interfaccia utente
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData per segnalare che il salvataggio è andato a buon fine
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // LiveData che indica se la lista dipendenti è vuota
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Funzione per caricare tutti i dipendenti dal database Firestore
    fun caricaDipendenti() {
        _isLoading.value = true           // Inizio caricamento
        _error.value = null               // Resetta errori precedenti

        db.collection("Profili")          // Accede alla collezione "Profili" su Firestore
            .get()                       // Recupera tutti i documenti
            .addOnSuccessListener { documents ->
                val listaDipendenti = mutableListOf<Dipendente>()  // Lista temporanea per salvare i dati

                for (document in documents) {
                    try {
                        // Estrae i campi dal documento Firestore con valori di default
                        val userId = document.getString("userId") ?: ""
                        val namelastname = document.getString("namelastname") ?: ""
                        val email = document.getString("email") ?: ""
                        val dataNascita = document.getString("dataNascita") ?: ""
                        val settoreOccupazione = document.getString("settoreOccupazione") ?: ""

                        // Log di debug per verificare i dati estratti
                        Log.d("UsersViewModel", "Documento ${document.id}:")
                        Log.d("UsersViewModel", "  - namelastname: '$namelastname'")
                        Log.d("UsersViewModel", "  - email: '$email'")
                        Log.d("UsersViewModel", "  - dataNascita: '$dataNascita'")
                        Log.d("UsersViewModel", "  - settoreOccupazione: '$settoreOccupazione'")

                        // Crea un oggetto Dipendente con i dati estratti
                        val dipendente = Dipendente(
                            userId = userId,
                            namelastname = namelastname,
                            email = email,
                            dataNascita = dataNascita,
                            settoreOccupazione = settoreOccupazione
                        )

                        // Aggiunge il dipendente alla lista
                        listaDipendenti.add(dipendente)

                    } catch (e: Exception) {
                        // Gestisce eventuali errori di parsing documento senza bloccare il caricamento
                        Log.w("UsersViewModel", "Errore nel processare documento: ${document.id}", e)
                    }
                }

                // Aggiorna la LiveData con la lista dei dipendenti
                _dipendenti.value = listaDipendenti
                // Indica se la lista è vuota
                _isEmpty.value = listaDipendenti.isEmpty()
                _isLoading.value = false      // Fine caricamento

                Log.d("UsersViewModel", "Caricati ${listaDipendenti.size} dipendenti")
            }
            .addOnFailureListener { exception ->
                // Gestisce errori nel caricamento dati
                Log.w("UsersViewModel", "Errore nel caricamento dipendenti", exception)
                _error.value = "Errore nel caricamento dei dipendenti: ${exception.message}"
                _isLoading.value = false
            }
    }

    // Funzione per caricare il profilo dell'utente autenticato
    fun caricaProfiloUtente() {
        val user = auth.currentUser       // Prende l'utente autenticato
        if (user != null) {
            _isLoading.value = true       // Indica caricamento in corso
            _error.value = null           // Resetta errori precedenti

            // Recupera il documento profilo corrispondente all'UID dell'utente
            db.collection("Profili").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Se il documento esiste, costruisce il Dipendente dai dati Firestore
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
                        // Se non esiste il documento, crea un profilo base con dati dell'utente autenticato
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
                    _isLoading.value = false   // Fine caricamento
                }
                .addOnFailureListener { exception ->
                    // Gestisce errori nella lettura del profilo
                    Log.w("UsersViewModel", "Errore nel caricamento profilo", exception)
                    _error.value = "Errore nel caricamento del profilo: ${exception.message}"
                    _isLoading.value = false
                }
        }
    }

    // Funzione per salvare o aggiornare il profilo dell'utente corrente
    fun salvaProfilo(dataNascita: String, password: String, settoreOccupazione: String) {
        val user = auth.currentUser       // Prende l'utente autenticato
        if (user != null) {
            _isLoading.value = true       // Indica caricamento in corso
            _error.value = null           // Resetta errori precedenti

            // Crea una mappa con i dati del profilo da salvare su Firestore
            val profiloDipendente = hashMapOf(
                "namelastname" to (user.displayName ?: ""),
                "dataNascita" to dataNascita,
                "email" to user.email,
                "password" to password,
                "settoreOccupazione" to settoreOccupazione,
                "userId" to user.uid
            )

            // Salva o aggiorna il documento corrispondente all'utente
            db.collection("Profili").document(user.uid)
                .set(profiloDipendente)
                .addOnSuccessListener {
                    Log.d("UsersViewModel", "Profilo salvato con successo!")
                    _saveSuccess.value = true
                    _isLoading.value = false

                    // Aggiorna anche la LiveData del profilo corrente con i dati nuovi
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

    fun incrementScore(isTeamA: Boolean) {
            }

    // Resetta il flag di successo del salvataggio
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // Resetta la LiveData degli errori
    fun resetError() {
        _error.value = null
    }
}
