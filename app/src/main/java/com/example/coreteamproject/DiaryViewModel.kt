package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Data class per rappresentare la valutazione mensile
data class ValutazioneMensile(
    val id: String = "",
    val meseAnno: String = "",
    val stress: Int = 0,
    val rapportoColleghi: Int = 0,
    val soddisfazioneLavoro: Int = 0,
    val commento: String = "",
    val userId: String = "",
    val timestamp: Long = 0
)

class DiaryViewModel : ViewModel() {

    // LiveData esposte
    private val _showForm = MutableLiveData<Boolean>()
    val showForm: LiveData<Boolean> = _showForm

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _valutazioni = MutableLiveData<List<ValutazioneMensile>>()
    val valutazioni: LiveData<List<ValutazioneMensile>> = _valutazioni

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        _showForm.value = false
        _isLoading.value = false
        _valutazioni.value = emptyList()
        _saveSuccess.value = false
        _isEmpty.value = true

        // Carica all'avvio
        caricaValutazioni()
    }

    fun mostraFormValutazione() {
        _showForm.value = true
    }

    fun nascondiFormValutazione() {
        _showForm.value = false
    }

    fun salvaValutazione(stress: Int, colleghi: Int, soddisfazione: Int, commento: String) {
        _isLoading.value = true

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "Utente non autenticato. Effettua il login."
            _isLoading.value = false
            return
        }

        val dataCorrente = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())

        // Controllo se esiste già una valutazione per lo stesso mese
        val valutazioneEsistente = _valutazioni.value?.any { it.meseAnno == dataCorrente } == true
        if (valutazioneEsistente) {
            _error.value = "Hai già inserito una valutazione per questo mese."
            _isLoading.value = false
            return
        }

        val valutazioneMap = hashMapOf(
            "meseAnno" to dataCorrente,
            "stress" to stress,
            "rapportoColleghi" to colleghi,
            "soddisfazioneLavoro" to soddisfazione,
            "commento" to commento,
            "userId" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        Log.d("DiaryViewModel", "Salvataggio valutazione: $valutazioneMap")

        db.collection("diaryEntries")
            .add(valutazioneMap)
            .addOnSuccessListener {
                Log.d("DiaryViewModel", "Salvataggio riuscito con ID: ${it.id}")
                caricaValutazioni()
                _saveSuccess.value = true
                _showForm.value = false
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Errore nel salvataggio: ${e.message}"
                _isLoading.value = false
            }
    }

    fun deleteValutazione(id: String) {
        _isLoading.value = true

        db.collection("diaryEntries")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Log.d("DiaryViewModel", "Valutazione eliminata: $id")
                caricaValutazioni()
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Errore durante l'eliminazione: ${e.message}"
                _isLoading.value = false
            }
    }

    private fun caricaValutazioni() {
        _isLoading.value = true
        _error.value = null

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _valutazioni.value = emptyList()
            _isEmpty.value = true
            _isLoading.value = false
            _error.value = "Utente non autenticato. Impossibile caricare le valutazioni."
            return
        }

        db.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.map { doc ->
                    ValutazioneMensile(
                        id = doc.id,
                        meseAnno = doc.getString("meseAnno") ?: "",
                        stress = (doc.getLong("stress") ?: 0).toInt(),
                        rapportoColleghi = (doc.getLong("rapportoColleghi") ?: 0).toInt(),
                        soddisfazioneLavoro = (doc.getLong("soddisfazioneLavoro") ?: 0).toInt(),
                        commento = doc.getString("commento") ?: "",
                        userId = doc.getString("userId") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }

                Log.d("DiaryViewModel", "Valutazioni caricate: ${lista.size}")
                _valutazioni.value = lista
                _isEmpty.value = lista.isEmpty()
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                Log.e("DiaryViewModel", "Errore nel caricamento: ${e.message}")
                _valutazioni.value = emptyList()
                _isEmpty.value = true
                _isLoading.value = false
                _error.value = "Errore nel caricamento delle valutazioni: ${e.message}"
            }
    }

    fun resetError() {
        _error.value = null
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun updateStressValue(value: Int) { /* opzionale */ }
    fun updateColleghiValue(value: Int) { /* opzionale */ }
    fun updateSoddisfazioneValue(value: Int) { /* opzionale */ }

    fun ricarica() {
        caricaValutazioni()
    }
}
