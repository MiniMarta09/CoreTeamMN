package com.example.coreteamproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Turno(
    val id: String = "",
    val title: String = "",
    val time: String = "",
    val description: String = "",
    val date: String = "",
    val userId: String = ""
)

class ShiftViewModel : ViewModel() {
    
    // Proprietà richieste dai layout XML
    private val _displayDate = MutableLiveData<String>()
    val displayDate: LiveData<String> = _displayDate
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _monthlyHours = MutableLiveData<String>()
    val monthlyHours: LiveData<String> = _monthlyHours
    
    // Proprietà aggiuntive per la funzionalità del Fragment
    private val _turni = MutableLiveData<List<Turno>>()
    val turni: LiveData<List<Turno>> = _turni
    
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess
    
    private val _canAddShift = MutableLiveData<Boolean>()
    val canAddShift: LiveData<Boolean> = _canAddShift
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentDate = ""
    private var currentMonth = ""
    
    init {
        // Inizializza con la data corrente
        val today = Calendar.getInstance()
        val todayString = dbDateFormat.format(today.time)
        updateSelectedDate(todayString)
    }
    
    fun updateSelectedDate(date: String) {
        currentDate = date
        
        // Aggiorna la data di visualizzazione
        try {
            val dateObj = dbDateFormat.parse(date)
            if (dateObj != null) {
                _displayDate.value = "Turni per: ${dateFormat.format(dateObj)}"
            }
        } catch (e: Exception) {
            _displayDate.value = "Turni per: $date"
        }
        
        // Verifica se è cambiato il mese per aggiornare il calcolo delle ore mensili
        val newMonth = date.substring(0, 7)
        if (newMonth != currentMonth) {
            currentMonth = newMonth
        }
        
        // Verifica se si può aggiungere un turno (solo per oggi o date passate)
        checkCanAddShift(date)
        
        // Carica i turni per la data selezionata
        loadShifts(date)
    }
    
    private fun checkCanAddShift(selectedDate: String) {
        try {
            val today = dbDateFormat.format(Date())
            val selectedDateObj = dbDateFormat.parse(selectedDate)
            val todayObj = dbDateFormat.parse(today)
            
            _canAddShift.value = selectedDateObj != null && todayObj != null && 
                                !selectedDateObj.after(todayObj)
        } catch (e: Exception) {
            _canAddShift.value = false
        }
    }
    
    fun saveShift(title: String, time: String, description: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Errore: utente non autenticato"
            return
        }
        
        _isLoading.value = true
        
        val shift = hashMapOf(
            "title" to title,
            "time" to time,
            "description" to description,
            "date" to currentDate,
            "userId" to currentUser.uid
        )
        
        db.collection("shifts")
            .add(shift)
            .addOnSuccessListener {
                _isLoading.value = false
                _saveSuccess.value = true
                loadShifts(currentDate)
                loadMonthlyHours()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Errore salvataggio turno: ${exception.message}"
            }
    }
    
    fun deleteShift(shiftId: String) {
        _isLoading.value = true
        
        db.collection("shifts")
            .document(shiftId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                _deleteSuccess.value = true
                loadShifts(currentDate)
                loadMonthlyHours()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Errore eliminazione: ${exception.message}"
            }
    }
    
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
                
                // Aggiorna il calcolo del monte ore mensile
                loadMonthlyHours()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Errore nel caricamento dei turni: ${exception.message}"
            }
    }
    
    private fun loadMonthlyHours() {
        try {
            // Otteniamo il mese dalla data selezionata (yyyy-MM)
            val selectedMonth = currentDate.substring(0, 7)
            currentMonth = selectedMonth
            
            // Costruiamo le date di inizio e fine mese correttamente
            val startOfMonth = "$selectedMonth-01"
            val endOfMonth = "$selectedMonth-31"
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _monthlyHours.value = "Ore $selectedMonth: Errore utente"
                return
            }
            
            // Approccio semplificato: carichiamo tutti i turni del mese e filtriamo
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
                            
                            // Include il turno SOLO se ha userId e corrisponde all'utente corrente
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
                            // Ignora turni con dati malformati
                            continue
                        }
                    }
                    
                    // Aggiorniamo il display
                    updateMonthlyHoursDisplay(totalMinutes, selectedMonth)
                }
                .addOnFailureListener { exception ->
                    _monthlyHours.value = "Ore $selectedMonth: Errore caricamento"
                }
        } catch (e: Exception) {
            _monthlyHours.value = "Ore: Errore calcolo"
        }
    }
    
    private fun updateMonthlyHoursDisplay(totalMinutes: Int, selectedMonth: String) {
        // Convertiamo i minuti totali in ore e minuti
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        // Aggiorniamo la TextView
        val monthName = getMonthName(selectedMonth.substring(5, 7).toInt() - 1)
        _monthlyHours.value = "Ore $monthName: ${hours}h ${minutes}m"
    }
    
    private fun calculateMinutesFromTimeRange(timeRange: String): Int {
        try {
            // Formato atteso: "HH:MM - HH:MM"
            val parts = timeRange.split(" - ")
            if (parts.size != 2) return 0
            
            val startTime = parts[0] // HH:MM
            val endTime = parts[1]   // HH:MM
            
            val startHour = startTime.split(":")[0].toInt()
            val startMinute = startTime.split(":")[1].toInt()
            val endHour = endTime.split(":")[0].toInt()
            val endMinute = endTime.split(":")[1].toInt()
            
            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute
            
            // Gestiamo anche il caso in cui l'orario di fine sia il giorno dopo
            return if (endTotalMinutes > startTotalMinutes) {
                endTotalMinutes - startTotalMinutes
            } else {
                (24 * 60 - startTotalMinutes) + endTotalMinutes // Assume turno notturno
            }
        } catch (e: Exception) {
            return 0
        }
    }
    
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
