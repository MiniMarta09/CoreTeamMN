package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

// ViewModel per la bacheca
class BoardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data class che rappresenta un singolo annuncio sulla bacheca
    data class Annuncio(
        val id: String = "",
        val content: String = "",
        val userId: String = "",
        val authorName: String = "",
        val settore: String = "",
        val timestamp: Date = Date()
    )

    // LiveData che espone la lista di annunci caricati da Firestore
    private val _annunci = MutableLiveData<List<Annuncio>>()
    val annunci: LiveData<List<Annuncio>> = _annunci

    // LiveData per notificare l'UI dello stato di caricamento
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData per comunicare eventuali errori all'UI
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadAnnunci() // Carica gli annunci all'avvio
    }

    // Carica tutti gli annunci da Firestore, ordinati per data
    fun loadAnnunci() {
        _isLoading.value = true
        db.collection("bacheca").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    _error.value = "Errore nel caricamento degli annunci: ${e.message}"
                    return@addSnapshotListener
                }

                // Converte i documenti in oggetti Annuncio
                val annunciList = snapshots?.map { doc ->
                    doc.toObject(Annuncio::class.java).copy(id = doc.id)
                } ?: emptyList()
                _annunci.value = annunciList
            }
    }

    // Salva un annuncio su Firestore. Recupera i dati del profilo utente prima di salvare.
    fun saveAnnuncio(content: String, annuncioId: String? = null) {
        val userId = auth.currentUser?.uid ?: return

        // 1. Recupera i dati dal profilo utente
        db.collection("Profili").document(userId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("namelastname") ?: "Utente Sconosciuto"
                val sector = document.getString("settoreOccupazione") ?: "N/A"

                // 2. Prepara i dati dell'annuncio
                val annuncioData = hashMapOf(
                    "content" to content,
                    "userId" to userId,
                    "authorName" to name,
                    "settore" to sector,
                    "timestamp" to Date()
                )

                // 3. Salva l'annuncio (nuovo o aggiornato)
                val task = if (annuncioId == null) {
                    db.collection("bacheca").add(annuncioData)
                } else {
                    db.collection("bacheca").document(annuncioId).update(annuncioData as Map<String, Any>)
                }

                task.addOnFailureListener { e ->
                    _error.value = "Errore nel salvataggio: ${e.message}"
                    Log.e("BoardViewModel", "Errore salvataggio annuncio", e)
                }
            }
            .addOnFailureListener { e ->
                _error.value = "Errore nel recupero del profilo: ${e.message}"
                Log.e("BoardViewModel", "Errore recupero profilo", e)
            }
    }

    // Elimina un annuncio dalla bacheca
    fun deleteAnnuncio(annuncioId: String) {
        db.collection("bacheca").document(annuncioId).delete()
            .addOnFailureListener { e ->
                _error.value = "Errore durante l'eliminazione: ${e.message}"
                Log.e("BoardViewModel", "Errore eliminazione annuncio", e)
            }
    }
}
