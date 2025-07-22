package com.example.coreteamproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Evento(
    val id: String = "",
    val title: String = "",
    val time: String = "",
    val description: String = ""
)

class EventViewModel : ViewModel() {
    
    // Proprietà richieste dai layout XML
    private val _displayDate = MutableLiveData<String>()
    val displayDate: LiveData<String> = _displayDate
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Proprietà aggiuntive per la funzionalità del Fragment
    private val _eventi = MutableLiveData<List<Evento>>()
    val eventi: LiveData<List<Evento>> = _eventi
    
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentDate = ""
    
    init {
        // Inizializza con la data corrente
        val today = Calendar.getInstance()
        updateSelectedDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
        _eventi.value = emptyList()
        _isEmpty.value = true
        _isLoading.value = false
        _saveSuccess.value = false
        _deleteSuccess.value = false
    }
    
    fun updateSelectedDate(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        currentDate = dateFormat.format(calendar.time)
        _displayDate.value = currentDate
        
        // Carica gli eventi per la data selezionata
        caricaEventi()
    }
    
    fun salvaEvento(title: String, time: String, description: String) {
        _isLoading.value = true
        val tempId = UUID.randomUUID().toString()
        val nuovoEvento = Evento(
            id = tempId,
            title = title,
            time = time,
            description = description
        )
        
        val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
        currentEvents.add(nuovoEvento)
        _eventi.value = currentEvents
        _isEmpty.value = false
        _saveSuccess.value = true
        
        // Prepara i dati per Firestore usando la collezione events
        val eventoMap = hashMapOf(
            "title" to title,
            "time" to time,
            "description" to description,
            "date" to currentDate,
            "userId" to (auth.currentUser?.uid ?: "utente_sconosciuto"),
            "timestamp" to Calendar.getInstance().timeInMillis
        )
        
        // Salva nella collezione events
        db.collection("events")
            .add(eventoMap)
            .addOnSuccessListener { documentReference ->
                // Aggiorna l'ID con quello reale di Firestore
                val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
                val index = currentEvents.indexOfFirst { it.id == tempId }
                if (index != -1) {
                    val eventoConIdReale = currentEvents[index].copy(id = documentReference.id)
                    currentEvents[index] = eventoConIdReale
                    _eventi.value = currentEvents
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
                // Lascia l'evento in UI anche se fallisce il salvataggio su Firestore
            }
    }
    
    fun eliminaEvento(eventoId: String) {
        _isLoading.value = true
        val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
        val eventoToRemove = currentEvents.find { it.id == eventoId }
        if (eventoToRemove != null) {
            currentEvents.remove(eventoToRemove)
            _eventi.value = currentEvents
            _isEmpty.value = currentEvents.isEmpty()
            _deleteSuccess.value = true
            
            // Rimuovi da Firestore
            db.collection("events")
                .document(eventoId)
                .delete()
                .addOnSuccessListener {
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _isLoading.value = false
                    // L'evento è già stato rimosso dall'UI
                }
        }
        _isLoading.value = false
    }
    
    // Metodi legacy per compatibilità
    fun addEvent(event: String) {
        salvaEvento(event, "", "")
    }
    
    fun removeEvent(index: Int) {
        val currentEvents = _eventi.value ?: emptyList()
        if (index in 0 until currentEvents.size) {
            eliminaEvento(currentEvents[index].id)
        }
    }
    
    // Carica eventi per la data corrente
    private fun caricaEventi() {
        _isLoading.value = true
        _error.value = null
        
        try {
            // Ottieni l'utente
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _eventi.value = emptyList()
                _isEmpty.value = true
                _isLoading.value = false
                return
            }
            
            // Query sulla collezione events filtrando per data (visibili a tutti)
            db.collection("events")
                .whereEqualTo("date", currentDate)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val eventiList = mutableListOf<Evento>()
                    
                    for (document in querySnapshot) {
                        val evento = Evento(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            time = document.getString("time") ?: "",
                            description = document.getString("description") ?: ""
                        )
                        eventiList.add(evento)
                    }
                    
                    _eventi.value = eventiList
                    _isEmpty.value = eventiList.isEmpty()
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _eventi.value = emptyList()
                    _isEmpty.value = true
                    _isLoading.value = false
                }
        } catch (Exception: Exception) {
            _eventi.value = emptyList()
            _isEmpty.value = true
            _isLoading.value = false
        }
    }
    
    fun resetError() {
        _error.value = null
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
    
    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
