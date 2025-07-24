package com.example.coreteamproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Data class che rappresenta un turno di lavoro
data class Turno(
    val id: String = "",             // ID del documento Firestore
    val title: String = "",          // Titolo del turno
    val time: String = "",           // Orario del turno (es. "08:00 - 12:00")
    val description: String = "",    // Descrizione del turno
    val date: String = "",           // Data del turno (formato "yyyy-MM-dd")
    val userId: String = ""          // ID utente associato al turno
)

class ShiftViewModel : ViewModel() {

    // Data da mostrare nell'interfaccia (formattata)
    private val _displayDate = MutableLiveData<String>()
    val displayDate: LiveData<String> = _displayDate
    // Stato di caricamento per mostrare progress bar
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    // Ore totali lavorate nel mese
    private val _monthlyHours = MutableLiveData<String>()
    val monthlyHours: LiveData<String> = _monthlyHours
    // Lista di turni per la data selezionata
    private val _turni = MutableLiveData<List<Turno>>()
    val turni: LiveData<List<Turno>> = _turni
    // Flag per indicare lista turni vuota
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    // Messaggi di errore
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    // Flag per indicare salvataggio avvenuto con successo
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    // Flag per indicare eliminazione avvenuta con successo
    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess
    // Indica se è possibile aggiungere un turno per la data selezionata
    private val _canAddShift = MutableLiveData<Boolean>()
    val canAddShift: LiveData<Boolean> = _canAddShift

    // Formattatori per date
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Riferimenti a Firebase Firestore e Firebase Authentication
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Variabili interne per data e mese correnti
    private var currentDate = ""
    private var currentMonth = ""

    init {
        // Inizializza con la data di oggi
        val today = Calendar.getInstance()
        val todayString = dbDateFormat.format(today.time)
        updateSelectedDate(todayString)  // Carica i dati per la data di oggi
    }

    // Aggiorna la data selezionata dall'utente e aggiorna UI e dati di conseguenza
    fun updateSelectedDate(date: String) {
        currentDate = date

        // Prova a convertire la data per mostrarla in un formato leggibile
        try {
            val dateObj = dbDateFormat.parse(date)
            if (dateObj != null) {
                _displayDate.value = "Turni per: ${dateFormat.format(dateObj)}"
            }
        } catch (e: Exception) {
            // Se il parsing fallisce, mostra la data originale
            _displayDate.value = "Turni per: $date"
        }

        // Aggiorna il mese corrente se è cambiato
        val newMonth = date.substring(0, 7) // "yyyy-MM"
        if (newMonth != currentMonth) {
            currentMonth = newMonth
        }

        // Controlla se l'utente può aggiungere un turno in questa data
        checkCanAddShift(date)

        // Carica i turni per la data selezionata
        loadShifts(date)
    }

    // Controlla se è consentito aggiungere un turno: solo oggi o date passate
    private fun checkCanAddShift(selectedDate: String) {
        try {
            val today = dbDateFormat.format(Date())
            val selectedDateObj = dbDateFormat.parse(selectedDate)
            val todayObj = dbDateFormat.parse(today)

            // Permette aggiunta se data precedente a pggi
            _canAddShift.value = selectedDateObj != null && todayObj != null &&
                    !selectedDateObj.after(todayObj)
        } catch (e: Exception) {
            _canAddShift.value = false
        }
    }

    // Salva un nuovo turno nel database per l'utente corrente
    fun saveShift(title: String, time: String, description: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Errore: utente non autenticato"
            return
        }

        _isLoading.value = true

        // Crea una mappa con i dati del turno
        val shift = hashMapOf(
            "title" to title,
            "time" to time,
            "description" to description,
            "date" to currentDate,
            "userId" to currentUser.uid
        )

        // Aggiunge il turno alla collezione "shifts" di Firestore
        db.collection("shifts")
            .add(shift)
            .addOnSuccessListener {
                _isLoading.value = false
                _saveSuccess.value = true
                loadShifts(currentDate)    // Ricarica turni per aggiornare la lista
                loadMonthlyHours()         // Ricalcola ore mensili
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Errore salvataggio turno: ${exception.message}"
            }
    }

    // Elimina un turno dato l'ID
    fun deleteShift(shiftId: String) {
        _isLoading.value = true

        db.collection("shifts")
            .document(shiftId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                _deleteSuccess.value = true
                loadShifts(currentDate)    // Ricarica turni dopo eliminazione
                loadMonthlyHours()         // Ricalcola ore mensili
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Errore eliminazione: ${exception.message}"
            }
    }

    // Carica i turni per una data specifica dall'utente autenticato
    private fun loadShifts(date: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Errore: utente non autenticato"
            return
        }

        _isLoading.value = true

        db.collection("shifts")
            .whereEqualTo("date", date)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                _isLoading.value = false

                if (documents.isEmpty) {
                    _turni.value = emptyList()
                    _isEmpty.value = true
                } else {
                    val turniList = mutableListOf<Turno>()
                    for (document in documents) {
                        val shift = document.data
                        val turno = Turno(
                            id = document.id,
                            title = shift["title"] as? String ?: "",
                            time = shift["time"] as? String ?: "",
                            description = shift["description"] as? String ?: "",
                            date = shift["date"] as? String ?: "",
                            userId = shift["userId"] as? String ?: ""
                        )
                        turniList.add(turno)
                    }
                    _turni.value = turniList
                    _isEmpty.value = false
                }

                // Aggiorna il conteggio ore mensili
                loadMonthlyHours()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Errore nel caricamento dei turni: ${exception.message}"
            }
    }

    // Calcola le ore totali lavorate nel mese corrente per l'utente
    private fun loadMonthlyHours() {
        try {
            val selectedMonth = currentDate.substring(0, 7) // yyyy-MM
            currentMonth = selectedMonth

            val startOfMonth = "$selectedMonth-01"
            val endOfMonth = "$selectedMonth-31"  // Estremi per la query (semplificato)

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _monthlyHours.value = "Ore $selectedMonth: Errore utente"
                return
            }

            // Query per ottenere tutti i turni nel mese per l'utente
            db.collection("shifts")
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .get()
                .addOnSuccessListener { documents ->
                    var totalMinutes = 0

                    for (document in documents) {
                        try {
                            val shift = document.data
                            val shiftUserId = shift["userId"] as? String

                            // Considera solo i turni dell'utente corrente
                            if (shiftUserId == currentUser.uid) {
                                val timeRange = shift["time"] as? String
                                if (timeRange != null && timeRange.isNotEmpty()) {
                                    val shiftMinutes = calculateMinutesFromTimeRange(timeRange)
                                    if (shiftMinutes > 0) {
                                        totalMinutes += shiftMinutes
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignora dati malformati
                            continue
                        }
                    }

                    // Aggiorna la UI con le ore totali del mese
                    updateMonthlyHoursDisplay(totalMinutes, selectedMonth)
                }
                .addOnFailureListener { exception ->
                    _monthlyHours.value = "Ore $selectedMonth: Errore caricamento"
                }
        } catch (e: Exception) {
            _monthlyHours.value = "Ore: Errore calcolo"
        }
    }

    // Aggiorna la stringa visualizzata per le ore totali mensili
    private fun updateMonthlyHoursDisplay(totalMinutes: Int, selectedMonth: String) {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val monthName = getMonthName(selectedMonth.substring(5, 7).toInt() - 1)
        _monthlyHours.value = "Ore $monthName: ${hours}h ${minutes}m"
    }

    // Calcola i minuti totali da un intervallo orario
    private fun calculateMinutesFromTimeRange(timeRange: String): Int {
        try {
            val parts = timeRange.split(" - ")
            if (parts.size != 2) return 0

            val startTime = parts[0] // "HH:mm"
            val endTime = parts[1]

            val startHour = startTime.split(":")[0].toInt()
            val startMinute = startTime.split(":")[1].toInt()
            val endHour = endTime.split(":")[0].toInt()
            val endMinute = endTime.split(":")[1].toInt()

            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute

            // Gestisce turni che terminano il giorno successivo
            return if (endTotalMinutes > startTotalMinutes) {
                endTotalMinutes - startTotalMinutes
            } else {
                (24 * 60 - startTotalMinutes) + endTotalMinutes
            }
        } catch (e: Exception) {
            return 0
        }
    }

    // Converte mesi
    private fun getMonthName(month: Int): String {
        return when (month) {
            0 -> "Gennaio"
            1 -> "Febbraio"
            2 -> "Marzo"
            3 -> "Aprile"
            4 -> "Maggio"
            5 -> "Giugno"
            6 -> "Luglio"
            7 -> "Agosto"
            8 -> "Settembre"
            9 -> "Ottobre"
            10 -> "Novembre"
            11 -> "Dicembre"
            else -> ""
        }
    }
    
    // Genera una lista di orari ogni 15 minuti (00:00, 00:15, 00:30, ecc.)
    fun generateTimeList(): List<String> {
        val timeList = mutableListOf<String>()
        
        for (hour in 0..23) {
            for (minute in listOf(0, 15, 30, 45)) {
                val formattedHour = hour.toString().padStart(2, '0')
                val formattedMinute = minute.toString().padStart(2, '0')
                timeList.add("$formattedHour:$formattedMinute")
            }
        }
        
        return timeList
    }
    
    // Metodi per resettare gli stati
    fun clearError() {
        _error.value = null
    }
    
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
    
    fun clearDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
