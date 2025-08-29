package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

class RequestsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Enum per i tipi di richiesta
    enum class RequestType {
        FERIE, PERMESSO_ENTRATA, PERMESSO_USCITA, SMARTWORKING
    }



    // Classe che rappresenta una richiesta
    class Request(
        val id: String,
        val userId: String,
        val authorName: String,
        val settore: String,
        val type: RequestType,
        val startDate: Date,
        val endDate: Date? = null, // Null per permessi di entrata/uscita
        val startTime: String? = null, // Per permessi orario
        val endTime: String? = null,   // Per permessi orario
        val reason: String,
        val timestamp: Date
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Request
            return id == other.id
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        // Funzione helper per ottenere il testo del tipo di richiesta
        fun getTypeDisplayName(): String {
            return when (type) {
                RequestType.FERIE -> "Ferie"
                RequestType.PERMESSO_ENTRATA -> "Permesso Entrata"
                RequestType.PERMESSO_USCITA -> "Permesso Uscita"
                RequestType.SMARTWORKING -> "Smartworking"
            }
        }
    }

    // LiveData per le richieste
    private val _requests = MutableLiveData<List<Request>>()
    val requests: LiveData<List<Request>> = _requests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadRequests()
    }

    // Carica tutte le richieste dell'utente corrente
    fun loadRequests() {
        val currentUserId = auth.currentUser?.uid ?: return

        _isLoading.value = true
        db.collection("requests")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    _error.value = "Errore nel caricamento delle richieste: ${e.message}"
                    Log.e("RequestsViewModel", "Errore nel caricamento delle richieste", e)
                    return@addSnapshotListener
                }

                val requestsList = snapshots?.map { document ->
                    Request(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        authorName = document.getString("authorName") ?: "",
                        settore = document.getString("settore") ?: "",
                        type = try {
                            RequestType.valueOf(document.getString("type") ?: "FERIE")
                        } catch (e: IllegalArgumentException) {
                            Log.w("RequestsViewModel", "Invalid request type found in document ${document.id}, defaulting to FERIE.")
                            RequestType.FERIE // Valore di default in caso di errore
                        },
                        startDate = document.getTimestamp("startDate")?.toDate() ?: Date(),
                        endDate = document.getTimestamp("endDate")?.toDate(),
                        startTime = document.getString("startTime"),
                        endTime = document.getString("endTime"),
                        reason = document.getString("reason") ?: "",
                        timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date()
                    )
                } ?: emptyList()

                // Ordina la lista sul client
                _requests.value = requestsList.sortedByDescending { it.timestamp }
            }
    }

    // Salva una nuova richiesta
    fun saveRequest(
        type: RequestType,
        startDate: Date,
        endDate: Date? = null,
        startTime: String? = null,
        endTime: String? = null,
        reason: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        // Recupera i dati del profilo utente
        db.collection("Profili").document(userId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("namelastname") ?: "Utente Sconosciuto"
                val sector = document.getString("settoreOccupazione") ?: "N/A"

                val requestData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "authorName" to name,
                    "settore" to sector,
                    "type" to type.name,
                    "startDate" to startDate,
                    "reason" to reason,
                    "timestamp" to Date()
                )

                // Aggiungi campi opzionali solo se non nulli
                endDate?.let { requestData["endDate"] = it }
                startTime?.let { requestData["startTime"] = it }
                endTime?.let { requestData["endTime"] = it }

                db.collection("requests").add(requestData)
                    .addOnFailureListener { e ->
                        _error.value = "Errore nel salvataggio della richiesta: ${e.message}"
                        Log.e("RequestsViewModel", "Errore salvataggio richiesta", e)
                    }
            }
            .addOnFailureListener { e ->
                _error.value = "Errore nel recupero del profilo: ${e.message}"
                Log.e("RequestsViewModel", "Errore recupero profilo", e)
            }
    }

    // Elimina una richiesta
    fun deleteRequest(requestId: String) {
        db.collection("requests").document(requestId).delete()
            .addOnFailureListener { e ->
                _error.value = "Errore durante l'eliminazione: ${e.message}"
                Log.e("RequestsViewModel", "Errore eliminazione richiesta", e)
            }
    }

    // Cancella il messaggio di errore
    fun clearError() {
        _error.value = null
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
}