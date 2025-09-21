package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// ViewModel per scheduling admin che usa settori aziendali
class AdminSchedulingViewModel : ViewModel() {
    
    private val db = Firebase.firestore
    private val settoriAlgorithm = SettoriSchedulingAlgorithm()
    
    // LiveData per i settori aziendali
    private val _settori = MutableLiveData<List<SettoreAziendale>>()
    val settori: LiveData<List<SettoreAziendale>> = _settori
    
    // LiveData per i turni generati (per mostrare la scheda)
    private val _turniGenerati = MutableLiveData<List<TurnoGenerato>>()
    val turniGenerati: LiveData<List<TurnoGenerato>> = _turniGenerati
    
    // LiveData per messaggi
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    // LiveData per loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Carica i settori aziendali predefiniti
     */
    fun caricaSettori() {
        _settori.value = SettoriPredefiniti.getSettoriDefault()
        _message.value = "Settori aziendali caricati"
    }
    
    /**
     * Genera turni basandosi sui settori aziendali
     */
    fun generaTurniConSettori(parametri: ParametriScheduling) {
        _isLoading.value = true
        _message.value = null
        
        // Carica prima i dipendenti
        db.collection("Profili")
            .get()
            .addOnSuccessListener { documentiProfili ->
                val dipendenti = mutableListOf<DisponibilitaDipendente>()
                
                for (documento in documentiProfili) {
                    val userId = documento.getString("userId") ?: ""
                    val nomeCompleto = documento.getString("namelastname") ?: ""
                    val email = documento.getString("email") ?: ""
                    val settore = documento.getString("settore") ?: ""
                    val ruolo = documento.getString("ruolo") ?: "USER"
                    
                    // Escludi admin/titolari dalla generazione turni
                    if (ruolo == "ADMIN") {
                        continue
                    }
                    
                    if (nomeCompleto.isNotEmpty() && userId.isNotEmpty()) {
                        dipendenti.add(
                            DisponibilitaDipendente(
                                userId = userId,
                                nomeCompleto = nomeCompleto,
                                email = email,
                                settore = settore, // Leggo il settore dal profilo
                                disponibilita = emptyMap() // Non serve più per i settori
                            )
                        )
                    }
                }
                
                if (dipendenti.isEmpty()) {
                    _message.value = "Nessun dipendente USER trovato (solo ADMIN esclusi)"
                    _isLoading.value = false
                    return@addOnSuccessListener
                }
                
                // Carica settori predefiniti (i dipendenti hanno già il settore nel profilo)
                val settoriAziendali = SettoriPredefiniti.getSettoriDefault()
                val turniSettori = settoriAlgorithm.generaTurniPerSettori(settoriAziendali, dipendenti, parametri)
                val turniPerVisualizzazione = settoriAlgorithm.convertiPerVisualizzazione(turniSettori)
                
                _turniGenerati.value = turniPerVisualizzazione
                _isLoading.value = false
                
                if (turniPerVisualizzazione.isNotEmpty()) {
                    _message.value = "✅ Generati ${turniPerVisualizzazione.size} turni per settori!"
                } else {
                    _message.value = "⚠️ Nessun turno generato per i settori."
                }
                
            }
            .addOnFailureListener { e ->
                Log.e("AdminScheduling", "Errore caricamento dipendenti", e)
                _message.value = "Errore: ${e.message}"
                _isLoading.value = false
            }
    }
    
    
    /**
     * Salva i turni generati in Firebase
     */
    fun salvaTurniGenerati() {
        val turni = _turniGenerati.value
        if (turni.isNullOrEmpty()) {
            _message.value = "Nessun turno da salvare"
            return
        }
        
        _isLoading.value = true
        var salvati = 0
        val totale = turni.size
        
        for (turnoGenerato in turni) {
            // Converte in formato Turno per Firebase
            val turnoMap = hashMapOf(
                "title" to "Turno ${turnoGenerato.data}",
                "time" to "${turnoGenerato.orarioInizio} - ${turnoGenerato.orarioFine}",
                "description" to "Dipendenti: ${turnoGenerato.dipendenti.joinToString(", ")}",
                "date" to turnoGenerato.data,
                "userId" to "", // Turno admin
                "workMode" to turnoGenerato.modalita
            )
            
            db.collection("shifts").document(turnoGenerato.id)
                .set(turnoMap)
                .addOnSuccessListener {
                    salvati++
                    if (salvati == totale) {
                        _message.value = "💾 Salvati tutti i $totale turni!"
                        _isLoading.value = false
                        _turniGenerati.value = emptyList() // Pulisce dopo salvataggio
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AdminScheduling", "Errore salvataggio", e)
                    _message.value = "Errore salvataggio: ${e.message}"
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Pulisce i turni generati
     */
    fun clearTurniGenerati() {
        _turniGenerati.value = emptyList()
    }
    
    /**
     * Pulisce i messaggi
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * Assegna i dipendenti ai settori basandosi sui loro profili
     */
    private fun assegnaDipendentiDaiProfili(
        settori: List<SettoreAziendale>, 
        dipendenti: List<DisponibilitaDipendente>
    ): List<SettoreAziendale> {
        
        if (dipendenti.isEmpty()) return settori
        
        val settoriConDipendenti = mutableListOf<SettoreAziendale>()
        
        // Mappa per raggruppare dipendenti per settore
        val dipendentiPerSettore = dipendenti.groupBy { it.settore.trim().lowercase() }
        
        for (settore in settori) {
            // Cerca dipendenti per questo settore con matching flessibile
            val possibiliChiavi = listOf(
                settore.id.lowercase(),
                settore.nome.lowercase(),
                settore.id.replace("_", " ").lowercase(),
                settore.id.replace("_", "").lowercase()
            )
            
            val dipendentiDelSettore = possibiliChiavi
                .firstNotNullOfOrNull { chiave -> dipendentiPerSettore[chiave] }
                ?: emptyList()
            
            val nomiDipendenti = dipendentiDelSettore.map { it.nomeCompleto }
            
            settoriConDipendenti.add(
                settore.copy(dipendentiAssegnati = nomiDipendenti)
            )
        }
        
        // Se nessun settore ha dipendenti, distribuiscili automaticamente
        val totaleDipendentiAssegnati = settoriConDipendenti.sumOf { it.dipendentiAssegnati.size }
        if (totaleDipendentiAssegnati == 0) {
            return distribuisciDipendentiAutomaticamente(settori, dipendenti)
        }
        
        return settoriConDipendenti
    }
    
    /**
     * Distribuisce automaticamente i dipendenti tra i settori se non trova corrispondenze
     */
    private fun distribuisciDipendentiAutomaticamente(
        settori: List<SettoreAziendale>,
        dipendenti: List<DisponibilitaDipendente>
    ): List<SettoreAziendale> {
        
        val nomiDipendenti = dipendenti.map { it.nomeCompleto }
        val settoriConDipendenti = mutableListOf<SettoreAziendale>()
        
        val dipendentiPerSettore = (nomiDipendenti.size + settori.size - 1) / settori.size
        var indiceDipendente = 0
        
        for (settore in settori) {
            val dipendentiAssegnati = mutableListOf<String>()
            
            repeat(dipendentiPerSettore) {
                if (indiceDipendente < nomiDipendenti.size) {
                    dipendentiAssegnati.add(nomiDipendenti[indiceDipendente])
                    indiceDipendente++
                }
            }
            
            settoriConDipendenti.add(
                settore.copy(dipendentiAssegnati = dipendentiAssegnati)
            )
        }
        
        return settoriConDipendenti
    }
}
