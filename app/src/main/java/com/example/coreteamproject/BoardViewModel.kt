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

    // Classe che rappresenta un singolo annuncio sulla bacheca
    class Annuncio(
        val id: String,
        val content: String,
        val userId: String,
        val authorName: String,
        val settore: String,
        val timestamp: Date,
        var likes: Long = 0,
        var dislikes: Long = 0,
        val likedBy: List<String> = emptyList(),
        val dislikedBy: List<String> = emptyList()
    ) {
        // Override di equals e hashCode per garantire che DiffUtil funzioni correttamente
        // Confrontiamo solo gli ID perché sono unici per ogni annuncio.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Annuncio

            return id == other.id
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

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

                // Converte i documenti in oggetti Annuncio manualmente per evitare errori di tipo
                val annunciList = snapshots?.map { document ->
                    Annuncio(
                        id = document.id,
                        content = document.getString("content") ?: "",
                        userId = document.getString("userId") ?: "",
                        authorName = document.getString("authorName") ?: "",
                        settore = document.getString("settore") ?: "",
                        timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date(),
                        likes = document.getLong("likes") ?: 0,
                        dislikes = document.getLong("dislikes") ?: 0,
                        likedBy = document.get("likedBy") as? List<String> ?: emptyList(),
                        dislikedBy = document.get("dislikedBy") as? List<String> ?: emptyList()
                    )
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
                val timestamp = document.get("timestamp", com.google.firebase.Timestamp::class.java)?.toDate() ?: Date()

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

    fun toggleLike(annuncioId: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("bacheca").document(annuncioId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            // Converti correttamente le liste da Firestore
            val currentLikedBy = (snapshot.get("likedBy") as? List<String>) ?: emptyList()
            val currentDislikedBy = (snapshot.get("dislikedBy") as? List<String>) ?: emptyList()

            val likedBy = currentLikedBy.toMutableList()
            val dislikedBy = currentDislikedBy.toMutableList()

            if (likedBy.contains(userId)) {
                // L'utente ha già messo like, quindi lo rimuove
                likedBy.remove(userId)
            } else {
                // L'utente non ha messo like, quindi lo aggiunge e rimuove il dislike se presente
                likedBy.add(userId)
                dislikedBy.remove(userId)
            }

            transaction.update(docRef, "likedBy", likedBy)
            transaction.update(docRef, "dislikedBy", dislikedBy)
            transaction.update(docRef, "likes", likedBy.size.toLong())
            transaction.update(docRef, "dislikes", dislikedBy.size.toLong())

            null
        }.addOnSuccessListener {
            Log.d("BoardViewModel", "Like aggiornato con successo")
        }.addOnFailureListener { e ->
            _error.value = "Errore nell'aggiornamento del like: ${e.message}"
            Log.e("BoardViewModel", "Errore toggle like", e)
        }
    }

    fun toggleDislike(annuncioId: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("bacheca").document(annuncioId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            // Converti correttamente le liste da Firestore
            val currentLikedBy = (snapshot.get("likedBy") as? List<String>) ?: emptyList()
            val currentDislikedBy = (snapshot.get("dislikedBy") as? List<String>) ?: emptyList()

            val likedBy = currentLikedBy.toMutableList()
            val dislikedBy = currentDislikedBy.toMutableList()

            if (dislikedBy.contains(userId)) {
                // L'utente ha già messo dislike, quindi lo rimuove
                dislikedBy.remove(userId)
            } else {
                // L'utente non ha messo dislike, quindi lo aggiunge e rimuove il like se presente
                dislikedBy.add(userId)
                likedBy.remove(userId)
            }

            transaction.update(docRef, "dislikedBy", dislikedBy)
            transaction.update(docRef, "likedBy", likedBy)
            transaction.update(docRef, "dislikes", dislikedBy.size.toLong())
            transaction.update(docRef, "likes", likedBy.size.toLong())

            null
        }.addOnSuccessListener {
            Log.d("BoardViewModel", "Dislike aggiornato con successo")
        }.addOnFailureListener { e ->
            _error.value = "Errore nell'aggiornamento del dislike: ${e.message}"
            Log.e("BoardViewModel", "Errore toggle dislike", e)
        }
    }
}