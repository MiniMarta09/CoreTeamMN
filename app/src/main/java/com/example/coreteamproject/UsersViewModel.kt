package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Enumerazione per i ruoli utente
enum class UserRole {
    USER,      // Utente standard
    ADMIN      // Amministratore
}

// Data class che rappresenta un dipendente con i relativi dati personali e lavorativi
data class Dipendente(
    val userId: String = "",            // ID univoco utente (da Firebase Auth)
    val namelastname: String = "",      // Nome e cognome del dipendente
    val email: String = "",             // Email dell'utente
    val dataNascita: String = "",       // Data di nascita in formato stringa
    val settoreOccupazione: String = "", // Settore lavorativo del dipendente
    val ruolo: UserRole = UserRole.USER  // Ruolo dell'utente (default: USER)
)

// ViewModel per gestire i dati degli utenti (dipendenti) e la logica di interazione con Firestore
class UsersViewModel : ViewModel() {

    // Riferimenti ai servizi Firebase
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // LiveData per la lista di tutti i dipendenti (usato nella schermata Team)
    private val _dipendenti = MutableLiveData<List<Dipendente>>()
    val dipendenti: LiveData<List<Dipendente>> = _dipendenti

    // LiveData per il profilo dell'utente attualmente loggato
    private val _currentUserProfile = MutableLiveData<Dipendente?>()
    val currentUserProfile: LiveData<Dipendente?> = _currentUserProfile

    // LiveData per gestire lo stato di caricamento (mostra/nasconde la progress bar)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per comunicare eventuali errori alla UI
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData per segnalare un salvataggio avvenuto con successo
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // LiveData per indicare se la lista dei dipendenti è vuota
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    
    // LiveData per esporre il ruolo dell'utente corrente (USER o ADMIN)
    private val _userRole = MutableLiveData<UserRole>(UserRole.USER)
    val userRole: LiveData<UserRole> = _userRole

    // Carica la lista di tutti i dipendenti dalla collezione 'Profili'
    fun caricaDipendenti() {
        _isLoading.value = true // Segnala l'inizio del caricamento
        _error.value = null // Resetta eventuali errori precedenti

        db.collection("Profili")
            .get()
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
                        
                        // Gestione del ruolo con valore di default
                        val ruolo = try {
                            val ruoloStr = document.getString("RUOLO") ?: "USER"
                            UserRole.valueOf(ruoloStr.uppercase())
                        } catch (e: Exception) {
                            UserRole.USER
                        }

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
                            settoreOccupazione = settoreOccupazione,
                            ruolo = ruolo
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

    // Carica il profilo specifico dell'utente attualmente autenticato
    fun caricaProfiloUtente() {
        val user = auth.currentUser       // Prende l'utente autenticato
        if (user != null) {
            _isLoading.value = true       // Indica caricamento in corso
            _error.value = null           // Resetta errori precedenti
            _userRole.value = UserRole.USER // Imposta ruolo di default

            // Recupera il documento profilo corrispondente all'UID dell'utente
            db.collection("Profili").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Se il documento esiste, costruisce il Dipendente dai dati Firestore
                        val ruolo = try {
                            val ruoloStr = document.getString("RUOLO") ?: "USER"
                            UserRole.valueOf(ruoloStr.uppercase())
                        } catch (e: Exception) {
                            UserRole.USER
                        }
                        
                        val dipendente = Dipendente(
                            userId = document.getString("userId") ?: user.uid,
                            namelastname = document.getString("namelastname") ?: (user.displayName ?: ""),
                            email = user.email ?: "",
                            dataNascita = document.getString("dataNascita") ?: "",
                            settoreOccupazione = document.getString("settoreOccupazione") ?: "",
                            ruolo = ruolo
                        )
                        _currentUserProfile.value = dipendente
                        _userRole.value = ruolo // Imposta il ruolo dell'utente
                        Log.d("UsersViewModel", "Profilo caricato per: ${dipendente.namelastname}")
                    } else {
                        // Se non esiste il documento, crea un profilo base con dati dell'utente autenticato
                        val dipendente = Dipendente(
                            userId = user.uid,
                            namelastname = user.displayName ?: "",
                            email = user.email ?: "",
                            dataNascita = "",
                            settoreOccupazione = "",
                            ruolo = UserRole.USER
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

    // Salva o aggiorna il profilo dell'utente (usato nella schermata Profilo)
    fun salvaProfilo(dataNascita: String, password: String, settoreOccupazione: String) {
        val user = auth.currentUser       // Prende l'utente autenticato
        if (user != null) {
            _isLoading.value = true       // Indica caricamento in corso
            _error.value = null           // Resetta errori precedenti

            // Prima controlla se il documento esiste per decidere se creare o aggiornare
            db.collection("Profili").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Il documento esiste, aggiorna solo i campi necessari (preserva il ruolo)
                        val aggiornamenti = hashMapOf<String, Any>(
                            "namelastname" to (user.displayName ?: ""),
                            "dataNascita" to dataNascita,
                            "email" to (user.email ?: ""),
                            "password" to password,
                            "settoreOccupazione" to settoreOccupazione,
                            "userId" to user.uid
                        )
                        
                        db.collection("Profili").document(user.uid)
                            .update(aggiornamenti)
                            .addOnSuccessListener {
                                Log.d("UsersViewModel", "Profilo aggiornato con successo!")
                                _saveSuccess.value = true
                                _isLoading.value = false
                                caricaProfiloUtente() // Ricarica il profilo aggiornato
                            }
                            .addOnFailureListener { e ->
                                Log.w("UsersViewModel", "Errore nell'aggiornamento", e)
                                _error.value = "Errore nel salvataggio: ${e.message}"
                                _isLoading.value = false
                            }
                    } else {
                        // Il documento non esiste, crealo con il ruolo USER di default
                        val profiloDipendente = hashMapOf(
                            "namelastname" to (user.displayName ?: ""),
                            "dataNascita" to dataNascita,
                            "email" to (user.email ?: ""),
                            "password" to password,
                            "settoreOccupazione" to settoreOccupazione,
                            "userId" to user.uid,
                            "RUOLO" to "USER"  // Imposta il ruolo di default per i nuovi utenti
                        )
                        
                        db.collection("Profili").document(user.uid)
                            .set(profiloDipendente)
                            .addOnSuccessListener {
                                Log.d("UsersViewModel", "Profilo creato con successo!")
                                _saveSuccess.value = true
                                _isLoading.value = false
                                caricaProfiloUtente() // Ricarica il profilo aggiornato
                            }
                            .addOnFailureListener { e ->
                                Log.w("UsersViewModel", "Errore nella creazione", e)
                                _error.value = "Errore nel salvataggio: ${e.message}"
                                _isLoading.value = false
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("UsersViewModel", "Errore nel controllo documento", e)
                    _error.value = "Errore nel salvataggio: ${e.message}"
                    _isLoading.value = false
                }
        }
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
