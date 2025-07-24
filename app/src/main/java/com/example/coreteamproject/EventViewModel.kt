package com.example.coreteamproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Data class che rappresenta un singolo evento con id, titolo, orario e descrizione
data class Evento(
    val id: String = "",         // ID univoco dell'evento (es. Firestore document ID)
    val title: String = "",      // Titolo dell'evento
    val time: String = "",       // Orario dell'evento (es. "14:30")
    val description: String = "" // Descrizione aggiuntiva dell'evento
)

// ViewModel per gestire la logica di eventi e comunicazione con Firebase
class EventViewModel : ViewModel() {

    // LiveData privata che contiene la data da mostrare in UI (formattata)
    private val _displayDate = MutableLiveData<String>()
    val displayDate: LiveData<String> = _displayDate // LiveData pubblica per osservare la data

    // LiveData privata che indica se è in corso un caricamento (show spinner)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData privata con la lista degli eventi caricati
    private val _eventi = MutableLiveData<List<Evento>>()
    val eventi: LiveData<List<Evento>> = _eventi

    // LiveData privata che indica se la lista eventi è vuota
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // LiveData privata per messaggi di errore da mostrare all'utente
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData privata che segnala il successo del salvataggio di un evento
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // LiveData privata che segnala il successo nell'eliminazione di un evento
    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    // Formattatore per le date, formato gg/MM/yyyy, basato sulla locale di sistema
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Riferimenti a Firebase Firestore e Authentication per operazioni DB e utente
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Stringa che memorizza la data corrente selezionata, formattata
    private var currentDate = ""

    init {
        // All'inizializzazione imposta la data corrente come data selezionata di default
        val today = Calendar.getInstance()
        updateSelectedDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

        // Inizializza le LiveData con valori di default
        _eventi.value = emptyList()
        _isEmpty.value = true
        _isLoading.value = false
        _saveSuccess.value = false
        _deleteSuccess.value = false
    }

    // Aggiorna la data selezionata, formato e carica eventi per quella data
    fun updateSelectedDate(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)            // Imposta la data nel Calendar
        currentDate = dateFormat.format(calendar.time)   // Formatto la data come stringa
        _displayDate.value = currentDate                  // Aggiorno LiveData della data visualizzata

        // Carica gli eventi corrispondenti alla data selezionata
        caricaEventi()
    }

    // Funzione per salvare un nuovo evento (titolo, orario, descrizione)
    fun salvaEvento(title: String, time: String, description: String) {
        _isLoading.value = true // Indica che si sta caricando/salvando

        // Creo un ID temporaneo unico per l'evento prima del salvataggio su Firestore
        val tempId = UUID.randomUUID().toString()

        // Creo un oggetto Evento con i dati forniti e ID temporaneo
        val nuovoEvento = Evento(
            id = tempId,
            title = title,
            time = time,
            description = description
        )

        // Aggiungo il nuovo evento alla lista corrente in LiveData
        val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
        currentEvents.add(nuovoEvento)
        _eventi.value = currentEvents
        _isEmpty.value = false   // Lista non è più vuota
        _saveSuccess.value = true // Segnalo successo temporaneo di salvataggio

        // Preparo i dati da salvare in Firestore, inclusi userId e timestamp
        val eventoMap = hashMapOf(
            "title" to title,
            "time" to time,
            "description" to description,
            "date" to currentDate,
            "userId" to (auth.currentUser?.uid ?: "utente_sconosciuto"),
            "timestamp" to Calendar.getInstance().timeInMillis
        )

        // Salvo l'evento nella collezione "events" su Firestore
        db.collection("events")
            .add(eventoMap)
            .addOnSuccessListener { documentReference ->
                // Quando Firestore risponde con l'ID reale documento, aggiorno l'evento in LiveData
                val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
                val index = currentEvents.indexOfFirst { it.id == tempId }
                if (index != -1) {
                    // Creo copia evento con ID reale di Firestore e aggiorno lista
                    val eventoConIdReale = currentEvents[index].copy(id = documentReference.id)
                    currentEvents[index] = eventoConIdReale
                    _eventi.value = currentEvents
                }
                _isLoading.value = false // Termino stato di caricamento
            }
            .addOnFailureListener {
                _isLoading.value = false
                // Se il salvataggio fallisce, l'evento resta comunque nella UI (non rimuovo)
            }
    }

    // Funzione per eliminare un evento dato il suo ID
    fun eliminaEvento(eventoId: String) {
        _isLoading.value = true // Indico che sto caricando

        val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
        val eventoToRemove = currentEvents.find { it.id == eventoId }
        if (eventoToRemove != null) {
            // Rimuovo evento dalla lista locale
            currentEvents.remove(eventoToRemove)
            _eventi.value = currentEvents
            _isEmpty.value = currentEvents.isEmpty() // Aggiorno flag lista vuota
            _deleteSuccess.value = true              // Segnala successo eliminazione

            // Rimuovo evento da Firestore
            db.collection("events")
                .document(eventoId)
                .delete()
                .addOnSuccessListener {
                    _isLoading.value = false // Termino stato caricamento
                }
                .addOnFailureListener {
                    _isLoading.value = false
                    // Se fallisce eliminazione da Firestore, evento è già stato rimosso dall'UI
                }
        }
        _isLoading.value = false // In ogni caso termino caricamento
    }

    // Metodi legacy per aggiungere evento con sola stringa
    fun addEvent(event: String) {
        salvaEvento(event, "", "")
    }

    // Metodo legacy per rimuovere evento per indice
    fun removeEvent(index: Int) {
        val currentEvents = _eventi.value ?: emptyList()
        if (index in 0 until currentEvents.size) {
            eliminaEvento(currentEvents[index].id)
        }
    }

    // Funzione privata che carica eventi da Firestore per la data corrente selezionata
    private fun caricaEventi() {
        _isLoading.value = true    // Inizio caricamento
        _error.value = null        // Reset errore

        try {
            // Recupero ID utente autenticato
            val userId = auth.currentUser?.uid
            if (userId == null) {
                // Se non autenticato, azzero lista e segnalo lista vuota
                _eventi.value = emptyList()
                _isEmpty.value = true
                _isLoading.value = false
                return
            }

            // Eseguo query su Firestore filtrando per la data corrente
            db.collection("events")
                .whereEqualTo("date", currentDate)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val eventiList = mutableListOf<Evento>()

                    // Per ogni documento trovato creo un oggetto Evento e lo aggiungo alla lista
                    for (document in querySnapshot) {
                        val evento = Evento(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            time = document.getString("time") ?: "",
                            description = document.getString("description") ?: ""
                        )
                        eventiList.add(evento)
                    }

                    _eventi.value = eventiList          // Aggiorno LiveData con lista eventi
                    _isEmpty.value = eventiList.isEmpty() // Aggiorno flag lista vuota
                    _isLoading.value = false            // Termino caricamento
                }
                .addOnFailureListener {
                    // In caso di errore pulisco lista eventi e segnalo lista vuota
                    _eventi.value = emptyList()
                    _isEmpty.value = true
                    _isLoading.value = false
                }
        } catch (Exception: Exception) {
            // Gestione generale eccezioni: resetto lista eventi e flag lista vuota
            _eventi.value = emptyList()
            _isEmpty.value = true
            _isLoading.value = false
        }
    }

    // Resetta il messaggio di errore a null
    fun resetError() {
        _error.value = null
    }

    // Resetta il flag di successo del salvataggio a false
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // Resetta il flag di successo dell'eliminazione a false
    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
