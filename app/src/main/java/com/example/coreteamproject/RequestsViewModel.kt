package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

// ViewModel per la gestione delle richieste (ferie, permessi, etc.)
class RequestsViewModel : ViewModel() {

    // Riferimenti ai servizi Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Enum per i tipi di richiesta
    enum class RequestType {
        FERIE, PERMESSO_ENTRATA, PERMESSO_USCITA, SMARTWORKING
    }

    // Enum per lo stato di approvazione
    enum class RequestStatus {
        IN_ATTESA, ACCETTATA, RIFIUTATA
    }

    // Classe che rappresenta una richiesta
    class Request(
        val id: String,
        val userId: String,
        val authorName: String,
        val settore: String,
        val type: RequestType,
        val startDate: Date,
        val endDate: Date? = null,
        val startTime: String? = null,
        val endTime: String? = null,
        val reason: String,
        val timestamp: Date,
        val status: RequestStatus // Aggiunto il campo stato
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

        // Funzione helper per il nome del tipo di richiesta
        fun getTypeDisplayName(): String {
            return when (type) {
                RequestType.FERIE -> "Ferie"
                RequestType.PERMESSO_ENTRATA -> "Permesso Entrata"
                RequestType.PERMESSO_USCITA -> "Permesso Uscita"
                RequestType.SMARTWORKING -> "Smartworking"
            }
        }

        // Funzione helper per il nome dello stato
        fun getStatusDisplayName(): String {
            return when (status) {
                RequestStatus.IN_ATTESA -> "In Attesa"
                RequestStatus.ACCETTATA -> "Accettata"
                RequestStatus.RIFIUTATA -> "Rifiutata"
            }
        }
    }

    // LiveData per la lista delle richieste
    private val _requests = MutableLiveData<List<Request>>()
    val requests: LiveData<List<Request>> = _requests


    // LiveData per lo stato di caricamento
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per eventuali errori
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Non caricare qui, la chiamata viene fatta da RequestsFragment dopo aver ottenuto il ruolo
    }

    // Carica le richieste da Firestore, differenziando la logica per admin e utenti standard
    fun loadRequests(isAdmin: Boolean) {
        _isLoading.value = true
        val collection = db.collection("requests")

        val query = if (isAdmin) {
            // L'admin legge tutte le richieste
            collection.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        } else {
            // L'utente standard legge solo le proprie richieste
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _error.value = "Utente non autenticato."
                _isLoading.value = false
                return
            }
            collection.whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }

        if (isAdmin) {
            // ADMIN: usa .get() per evitare errori di permesso. La lista non si aggiorna in tempo reale.
            query.get()
                .addOnSuccessListener { snapshots ->
                    _isLoading.value = false
                    _requests.value = parseRequests(snapshots)
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _error.value = "Errore caricamento richieste: ${e.message}"
                }
        } else {
            // UTENTE STANDARD: usa snapshot listener per aggiornamenti in tempo reale.
            query.addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    _error.value = "Errore caricamento richieste: ${e.message}"
                    return@addSnapshotListener
                }
                _requests.value = parseRequests(snapshots)
            }
        }
    }

    // Funzione helper per convertire i documenti Firestore in una lista di oggetti Request
    private fun parseRequests(snapshots: com.google.firebase.firestore.QuerySnapshot?): List<Request> {
        return snapshots?.mapNotNull { document ->
            try {
                Request(
                    id = document.id,
                    userId = document.getString("userId") ?: "",
                    authorName = document.getString("authorName") ?: "",
                    settore = document.getString("settore") ?: "",
                    type = RequestType.valueOf(document.getString("type") ?: "FERIE"),
                    startDate = document.getTimestamp("startDate")?.toDate() ?: Date(),
                    endDate = document.getTimestamp("endDate")?.toDate(),
                    startTime = document.getString("startTime"),
                    endTime = document.getString("endTime"),
                    reason = document.getString("reason") ?: "",
                    timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date(),
                    status = RequestStatus.valueOf(document.getString("status") ?: "IN_ATTESA")
                )
            } catch (e: Exception) {
                Log.w("RequestsViewModel", "Errore parsing documento ${document.id}: ${e.message}")
                null
            }
        } ?: emptyList()
    }


    // Salva una nuova richiesta nel database
    fun saveRequest(
        type: RequestType,
        startDate: Date,
        endDate: Date? = null,
        startTime: String? = null,
        endTime: String? = null,
        reason: String
    ) {
        val userId = auth.currentUser?.uid ?: return

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
                    "timestamp" to Date(),
                    "status" to RequestStatus.IN_ATTESA.name
                )

                endDate?.let { requestData["endDate"] = it }
                startTime?.let { requestData["startTime"] = it }
                endTime?.let { requestData["endTime"] = it }

                db.collection("requests").add(requestData)
                    .addOnSuccessListener {
                        Log.d("RequestsViewModel", "Richiesta salvata con successo")
                    }
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

    // Elimina una richiesta specifica
    fun deleteRequest(requestId: String) {
        db.collection("requests").document(requestId).delete()
            .addOnFailureListener { e ->
                _error.value = "Errore durante l'eliminazione: ${e.message}"
                Log.e("RequestsViewModel", "Errore eliminazione richiesta", e)
            }
    }

    // Aggiorna lo stato di una richiesta (es. da IN_ATTESA a ACCETTATA)
    fun updateRequestStatus(requestId: String, newStatus: RequestStatus) {
        db.collection("requests").document(requestId)
            .update("status", newStatus.name)
            .addOnFailureListener { e ->
                _error.value = "Errore nell'aggiornamento dello stato: ${e.message}"
                Log.e("RequestsViewModel", "Errore aggiornamento stato richiesta", e)
            }
    }

    // Pulisce il messaggio di errore una volta gestito
    fun clearError() {
        _error.value = null
    }

    // Genera una lista di orari a intervalli di 15 minuti per i dialoghi di selezione
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