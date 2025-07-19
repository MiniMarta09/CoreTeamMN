package com.example.coreteamproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
        _displayDate.value = dateFormat.format(calendar.time)
    }
    
    fun salvaEvento(title: String, time: String, description: String) {
        _isLoading.value = true
        val nuovoEvento = Evento(
            id = UUID.randomUUID().toString(),
            title = title,
            time = time,
            description = description
        )
        val currentEvents = _eventi.value?.toMutableList() ?: mutableListOf()
        currentEvents.add(nuovoEvento)
        _eventi.value = currentEvents
        _isEmpty.value = currentEvents.isEmpty()
        _saveSuccess.value = true
        _isLoading.value = false
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
