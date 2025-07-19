package com.example.coreteamproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class ValutazioneMensile(
    val meseAnno: String = "",
    val stress: Int = 0,
    val rapportoColleghi: Int = 0,
    val soddisfazioneLavoro: Int = 0,
    val commento: String = ""
)

class DiaryViewModel : ViewModel() {
    
    // Proprietà richieste dai layout XML
    private val _showForm = MutableLiveData<Boolean>()
    val showForm: LiveData<Boolean> = _showForm
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Proprietà aggiuntive per la funzionalità del Fragment
    private val _valutazioni = MutableLiveData<List<ValutazioneMensile>>()
    val valutazioni: LiveData<List<ValutazioneMensile>> = _valutazioni
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    
    init {
        _showForm.value = false
        _isLoading.value = false
        _valutazioni.value = emptyList()
        _saveSuccess.value = false
        _isEmpty.value = true
    }
    
    fun mostraFormValutazione() {
        _showForm.value = true
    }
    
    fun nascondiFormValutazione() {
        _showForm.value = false
    }
    
    fun updateStressValue(value: Int) {
        // Metodo richiesto dal Fragment per aggiornare il valore dello stress
    }
    
    fun updateColleghiValue(value: Int) {
        // Metodo richiesto dal Fragment per aggiornare il valore dei colleghi
    }
    
    fun updateSoddisfazioneValue(value: Int) {
        // Metodo richiesto dal Fragment per aggiornare il valore della soddisfazione
    }
    
    fun salvaValutazione(stress: Int, colleghi: Int, soddisfazione: Int, commento: String) {
        _isLoading.value = true
        
        val nuovaValutazione = ValutazioneMensile(
            meseAnno = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
            stress = stress,
            rapportoColleghi = colleghi,
            soddisfazioneLavoro = soddisfazione,
            commento = commento
        )
        
        val currentList = _valutazioni.value?.toMutableList() ?: mutableListOf()
        currentList.add(0, nuovaValutazione) // Aggiungi in cima alla lista
        _valutazioni.value = currentList
        
        _isEmpty.value = currentList.isEmpty()
        _saveSuccess.value = true
        _showForm.value = false
        _isLoading.value = false
    }
    
    fun deleteValutazione(index: Int) {
        _isLoading.value = true
        val currentList = _valutazioni.value?.toMutableList() ?: mutableListOf()
        if (index in 0 until currentList.size) {
            currentList.removeAt(index)
            _valutazioni.value = currentList
            _isEmpty.value = currentList.isEmpty()
        }
        _isLoading.value = false
    }
    
    fun resetError() {
        _error.value = null
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
