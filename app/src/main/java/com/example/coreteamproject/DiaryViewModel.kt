package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Data class che rappresenta una singola valutazione mensile
data class ValutazioneMensile(
    val id: String = "",                  // ID documento Firestore
    val meseAnno: String = "",            // Mese e anno della valutazione
    val stress: Int = 0,                  // Valore stress
    val rapportoColleghi: Int = 0,       // Valore rapporto con i colleghi
    val soddisfazioneLavoro: Int = 0,     // Valore soddisfazione sul lavoro
    val commento: String = "",            // Commento libero inserito dall’utente
    val userId: String = "",              // ID utente Firebase
    val timestamp: Long = 0               // Timestamp in millisecondi della creazione
)

// Data class per i dati aggregati del grafico
data class ChartData(
    val monthlyAverages: List<MonthlyAverage> // Lista di medie mensili
)

data class MonthlyAverage(
    val monthYear: String, // Formato "MM/yyyy"
    val avgStress: Float,
    val avgRapportoColleghi: Float,
    val avgSoddisfazioneLavoro: Float
)

// Data class per contenere i risultati delle statistiche aggregate
data class DiaryStats(
    val overallStrength: String, // Punto di forza generale
    val overallWeakness: String,  // Area di miglioramento generale
    val participation: String, // Partecipazione nell'ultimo mese
    val bestMonth: String // Mese con la media generale più alta
)

class DiaryViewModel : ViewModel() {

    // LiveData privata per mostra/nascondi form inserimento valutazione
    private val _showForm = MutableLiveData<Boolean>()
    val showForm: LiveData<Boolean> = _showForm

    // LiveData privata per indicare stato di caricamento o salvataggio dati
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData privata con lista delle valutazioni mensili caricate
    private val _valutazioni = MutableLiveData<List<ValutazioneMensile>>()
    val valutazioni: LiveData<List<ValutazioneMensile>> = _valutazioni

    // LiveData privata per messaggi di errore da mostrare all’utente
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData privata che segnala successo nel salvataggio di una valutazione
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // LiveData privata per indicare se la lista valutazioni è vuota
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // LiveData per i dati del grafico (solo per admin)
    private val _chartData = MutableLiveData<ChartData?>()
    val chartData: LiveData<ChartData?> = _chartData

    // LiveData per le statistiche aggregate (solo per admin)
    private val _diaryStats = MutableLiveData<DiaryStats?>()
    val diaryStats: LiveData<DiaryStats?> = _diaryStats

    // Riferimenti a Firebase Firestore e Authentication
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Memorizza il ruolo dell'utente per le operazioni di ricarica
    private var isAdminUser: Boolean = false

    init {
        // Inizializza valori LiveData di default
        _showForm.value = false
        _isLoading.value = false
        _valutazioni.value = emptyList()
        _saveSuccess.value = false
        _isEmpty.value = true

        // Il caricamento dei dati viene ora avviato dal Fragment, non più automaticamente
        // caricaValutazioni()
    }

    // Mostra il form per inserire una nuova valutazione
    fun mostraFormValutazione() {
        _showForm.value = true
    }

    // Nasconde il form di inserimento valutazione
    fun nascondiFormValutazione() {
        _showForm.value = false
    }

    // Salva una nuova valutazione mensile su Firestore
    fun salvaValutazione(stress: Int, colleghi: Int, soddisfazione: Int, commento: String) {
        _isLoading.value = true

        // Recupera l’ID dell’utente autenticato, se presente
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Se non è autenticato, segnala errore e termina
            _error.value = "Utente non autenticato. Effettua il login."
            _isLoading.value = false
            return
        }

        // Data corrente
        val dataCorrente = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())

        // Controlla se per questo mese è già stata inserita una valutazione
        val valutazioneEsistente = _valutazioni.value?.any { it.meseAnno == dataCorrente } == true
        if (valutazioneEsistente) {
            // Se esiste già, segnala errore e termina
            _error.value = "Hai già inserito una valutazione per questo mese."
            _isLoading.value = false
            return
        }

        // Crea una mappa con i dati da salvare su Firestore
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

        // Aggiunge la nuova valutazione al database Firestore
        db.collection("diaryEntries")
            .add(valutazioneMap)
            .addOnSuccessListener {
                Log.d("DiaryViewModel", "Salvataggio riuscito con ID: ${it.id}")
                ricarica() // Ricarica i dati in base al ruolo
                _saveSuccess.value = true
                _showForm.value = false
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                // In caso di errore durante il salvataggio
                _error.value = "Errore nel salvataggio: ${e.message}"
                _isLoading.value = false
            }
    }

    // Elimina una valutazione esistente dato il suo ID
    fun deleteValutazione(id: String) {
        _isLoading.value = true // Indica caricamento in corso

        // Richiede l’eliminazione dal database Firestore
        db.collection("diaryEntries")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Log.d("DiaryViewModel", "Valutazione eliminata: $id")
                ricarica() // Ricarica i dati in base al ruolo
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                // Errore durante l’eliminazione
                _error.value = "Errore durante l'eliminazione: ${e.message}"
                _isLoading.value = false
            }
    }

    // Punto di ingresso per il caricamento dei dati, chiamato dal Fragment
    fun loadDiaryData(isAdmin: Boolean) {
        this.isAdminUser = isAdmin
        if (isAdmin) {
            loadAllDiariesForAdmin()
        } else {
            loadCurrentUserDiaries()
        }
    }

    // Carica i dati aggregati di tutti gli utenti per la vista admin
    private fun loadAllDiariesForAdmin() {
        _isLoading.value = true
        _error.value = null

        db.collection("diaryEntries")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val allEntries = snapshot.mapNotNull { doc ->
                    try {
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
                    } catch (e: Exception) {
                        null
                    }
                }

                // Aggrega i dati per mese
                val monthlyData = allEntries
                    .groupBy { it.meseAnno }
                    .map { (monthYear, entries) ->
                        MonthlyAverage(
                            monthYear = monthYear,
                            avgStress = entries.map { it.stress }.average().toFloat(),
                            avgRapportoColleghi = entries.map { it.rapportoColleghi }.average().toFloat(),
                            avgSoddisfazioneLavoro = entries.map { it.soddisfazioneLavoro }.average().toFloat()
                        )
                    }
                    .sortedBy { it.monthYear } // Ordina per mese

                _chartData.value = ChartData(monthlyAverages = monthlyData)

                // Calcola le statistiche generali solo se sono presenti dati
                if (allEntries.isNotEmpty()) {
                    val overallAvgStress = allEntries.map { it.stress }.average()
                    val overallAvgColleghi = allEntries.map { it.rapportoColleghi }.average()
                    val overallAvgSoddisfazione = allEntries.map { it.soddisfazioneLavoro }.average()

                    val averages = mapOf(
                        "Stress" to overallAvgStress,
                        "Rapporto Colleghi" to overallAvgColleghi,
                        "Soddisfazione" to overallAvgSoddisfazione
                    )

                    // Trova la categoria con la media più alta (punto di forza) e più bassa (debolezza)
                    val strength = averages.maxByOrNull { it.value }?.key ?: "N/D"
                    val weakness = averages.minByOrNull { it.value }?.key ?: "N/D"

                    // Calcola la partecipazione: numero di utenti unici che hanno scritto nel mese corrente
                    val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
                    val participantsLastMonth = allEntries.filter { it.meseAnno == currentMonthYear }.distinctBy { it.userId }.size
                    val totalUsers = allEntries.distinctBy { it.userId }.size.coerceAtLeast(1) // Evita divisione per zero
                    val participation = "$participantsLastMonth / $totalUsers"

                    // Calcola il mese migliore trovando quello con la media generale più alta
                    val bestMonth = monthlyData.maxByOrNull { (it.avgStress + it.avgRapportoColleghi + it.avgSoddisfazioneLavoro) / 3 }?.monthYear ?: "N/D"

                    _diaryStats.value = DiaryStats(
                        overallStrength = strength,
                        overallWeakness = weakness,
                        participation = participation,
                        bestMonth = bestMonth
                    )
                }

                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Errore nel caricamento dei dati per l'admin: ${e.message}"
                _isLoading.value = false
            }
    }

    // Carica solo le valutazioni dell’utente autenticato
    private fun loadCurrentUserDiaries() {
        _isLoading.value = true
        _error.value = null

        // Recupera ID utente autenticato
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Se non autenticato, imposta lista vuota e mostra errore
            _valutazioni.value = emptyList()
            _isEmpty.value = true
            _isLoading.value = false
            _error.value =
            return
        }

        // Query Firestore per caricare le valutazioni dell’utente ordinate per data decrescente
        db.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                // Mappa ogni documento Firestore in un oggetto ValutazioneMensile
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
                _valutazioni.value = lista     // Aggiorna LiveData con lista valutazioni
                _isEmpty.value = lista.isEmpty() // Indica se la lista è vuota
                _isLoading.value = false       // Termina caricamento
            }
            .addOnFailureListener { e ->
                Log.e("DiaryViewModel", "Errore nel caricamento: ${e.message}")
                _valutazioni.value = emptyList()
                _isEmpty.value = true
                _isLoading.value = false
                _error.value = "Errore nel caricamento delle valutazioni: ${e.message}"
            }
    }

    // Resetta il messaggio di errore
    fun resetError() {
        _error.value = null
    }

    // Resetta il flag di successo nel salvataggio a false
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // Funzioni opzionali per aggiornare valori (
    fun updateStressValue(value: Int) { /* opzionale */ }
    fun updateColleghiValue(value: Int) { /* opzionale */ }
    fun updateSoddisfazioneValue(value: Int) { /* opzionale */ }

    // Ricarica i dati in base al ruolo dell'utente (admin o standard)
    fun ricarica() {
        loadDiaryData(this.isAdminUser)
    }
}
