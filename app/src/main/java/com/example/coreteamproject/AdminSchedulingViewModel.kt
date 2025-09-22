package com.example.coreteamproject

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Data class per rappresentare un turno da visualizzare
data class TurnoVisualizzato(
    val id: String,
    val data: String,
    val orario: String,
    val descrizione: String,
    val settore: String? = "Non specificato"
)

// Data class per la visualizzazione a card per persona
data class TurnoAssegnato(
    val giorno: String,
    val orario: String,
    val settore: String
)

data class ProgrammazioneSettimanalePersona(
    val nomeDipendente: String,
    val turniSettimanali: Map<String, TurnoAssegnato?> // Mappa da Giorno -> Turno
)

// Data class per rappresentare un orario contrattuale
data class OrarioContratto(
    val settore: String,
    val giorniLavorativi: String,
    val orario: String,
    val tipoContratto: String,
    val pausa: String
)

// ViewModel per la logica di scheduling dell'admin
class AdminSchedulingViewModel : ViewModel() {

    // LiveData per gli orari contrattuali
    private val _orariContratto = MutableLiveData<List<OrarioContratto>>()
    val orariContratto: LiveData<List<OrarioContratto>> = _orariContratto

    // LiveData per la visualizzazione dei turni
    private val _tuttiITurni =
        MutableLiveData<List<TurnoVisualizzato>>() // Lista completa originale
    private val _turniFiltrati = MutableLiveData<List<TurnoVisualizzato>>()
    val turniFiltrati: LiveData<List<TurnoVisualizzato>> = _turniFiltrati

    private val _listaSettori = MutableLiveData<List<String>>()
    val listaSettori: LiveData<List<String>> = _listaSettori

    private val _listaDipendenti = MutableLiveData<List<String>>()
    val listaDipendenti: LiveData<List<String>> = _listaDipendenti

    private val _programmazioneSettimanale =
        MutableLiveData<List<ProgrammazioneSettimanalePersona>>()
    val programmazioneSettimanale: LiveData<List<ProgrammazioneSettimanalePersona>> =
        _programmazioneSettimanale


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val db = Firebase.firestore
    private val settoriAlgorithm = SettoriSchedulingAlgorithm()
    private val _turniGeneratiPerSalvataggio =
        MutableLiveData<List<TurnoSettore>>() // Dati grezzi per il salvataggio

    /**
     * Carica gli orari dei contratti dall'unica fonte di verità (ContrattiPredefiniti)
     * e li trasforma nel formato necessario per la visualizzazione.
     */
    fun loadOrariContratto() {
        val contratti = ContrattiPredefiniti.getContratti()
        val orariPerUI = contratti.values.map { contratto ->
            // Raggruppa gli orari per fasce orarie (es. tutti i giorni che fanno 09-18)
            val orariRaggruppati =
                contratto.orari.groupBy { "${it.orarioInizio} - ${it.orarioFine}" }

            // Prende la fascia oraria più comune per semplificare la visualizzazione
            val orarioPrincipale = orariRaggruppati.maxByOrNull { it.value.size }?.key ?: "N/A"
            val giorniLavorativi = orariRaggruppati.values.flatten()
                .map { it.giorno.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                .distinct()
                .joinToString(", ")

            OrarioContratto(
                settore = contratto.settore.nomeVisualizzato,
                giorniLavorativi = giorniLavorativi,
                orario = orarioPrincipale,
                tipoContratto = if (giorniLavorativi.contains(",")) "Full-time" else "Part-time", // Logica semplificata
                pausa = "N/A" // Placeholder
            )
        }
        _orariContratto.value = orariPerUI
    }

    /**
     * Carica tutti i turni dalla collezione 'shifts' di Firestore.
     */
    fun loadShiftsForWeek(startDate: String, endDate: String) {
        _isLoading.value = true
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            _message.value = "Utente non autenticato, impossibile caricare i turni."
            _isLoading.value = false
            return
        }

        FirebaseFirestore.getInstance().collection("shifts")
            .whereEqualTo("userId", currentUserId) // Filtro per l'utente corrente
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .get()
            .addOnSuccessListener { documents ->
                val turniList = mutableListOf<TurnoVisualizzato>()
                val settori = mutableSetOf<String>()
                val dipendenti = mutableSetOf<String>()

                for (doc in documents) {
                    val descrizione = doc.getString("description") ?: ""

                    val settore = extractSettoreFromDescription(descrizione)
                    val dipendentiTurno = extractDipendentiFromDescription(descrizione)

                    val turno = TurnoVisualizzato(
                        id = doc.id,
                        data = doc.getString("date") ?: "",
                        orario = doc.getString("time") ?: "",
                        descrizione = descrizione,
                        settore = settore
                    )
                    turniList.add(turno)
                    settore?.let { settori.add(it) }
                    dipendenti.addAll(dipendentiTurno)
                }

                _tuttiITurni.value = turniList
                _turniFiltrati.value = turniList // All'inizio mostra tutto
                _listaSettori.value = listOf("Tutti i settori") + settori.sorted()
                _listaDipendenti.value = listOf("Tutti i dipendenti") + dipendenti.sorted()
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
                // Gestire l'errore
            }
    }

    /**
     * Applica i filtri per settore e/o dipendente alla lista dei turni.
     */
    fun applyFilters(settore: String, dipendente: String) {
        var turniDaFiltrare = _tuttiITurni.value ?: emptyList()

        if (settore != "Tutti i settori") {
            turniDaFiltrare = turniDaFiltrare.filter { it.settore == settore }
        }

        if (dipendente != "Tutti i dipendenti") {
            turniDaFiltrare = turniDaFiltrare.filter { it.descrizione.contains(dipendente) }
        }

        _turniFiltrati.value = turniDaFiltrare
    }

    fun generaTurniPerSettimana(dataInizio: String, dataFine: String) {
        _isLoading.value = true
        val parametri = ParametriScheduling(
            dataInizio = dataInizio,
            dataFine = dataFine,
            includiWeekend = false
        )

        // 1. Recupera TUTTE le richieste approvate
        db.collection("requests")
            .whereEqualTo("status", "ACCETTATA") // Campo e valore corretti
            .get()
            .addOnSuccessListener { documentiRichieste ->

                // 2. Normalizza le date delle richieste e filtra per il periodo corretto
                Log.d("SchedulingDebug", "--- Inizio Analisi Richieste ---")
                val richiesteFiltrate = documentiRichieste.mapNotNull { doc ->
                    val startTimestamp = doc.getTimestamp("startDate")
                    val endTimestamp = doc.getTimestamp("endDate") // Legge anche la data di fine
                    val userIdOriginale = doc.getString("userId") ?: ""

                    if (startTimestamp != null) {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALIAN)
                        val dataNormalizzata = formatter.format(startTimestamp.toDate())

                        // Filtra in base all'intervallo richiesto dall'utente
                        val fineRichiesta = endTimestamp?.toDate() ?: startTimestamp.toDate()
                        val inizioScheduling = formatter.parse(dataInizio)
                        val fineScheduling = formatter.parse(dataFine)

                        if (fineRichiesta >= inizioScheduling && startTimestamp.toDate() <= fineScheduling) {
                            val tipoRichiesta = doc.getString("type") ?: ""
                            Richiesta(
                                userId = userIdOriginale,
                                data = dataNormalizzata, // Manteniamo la data di inizio per riferimento
                                tipo = tipoRichiesta,
                                stato = doc.getString("status") ?: "",
                                orario = if (tipoRichiesta != "FERIE") doc.getString("startTime") else null,
                                startDate = startTimestamp,
                                endDate = endTimestamp
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                // 3. Recupera i profili dei dipendenti
                db.collection("Profili").get().addOnSuccessListener { documentiProfili ->
                    val dipendenti = documentiProfili.mapNotNull { doc ->
                        DisponibilitaDipendente(
                            userId = doc.getString("userId") ?: "",
                            nomeCompleto = doc.getString("namelastname") ?: "",
                            email = doc.getString("email") ?: "",
                            settore = stringToSettoreLavorativo(doc.getString("settoreOccupazione")),
                            disponibilita = emptyMap()
                        )
                    }

                    if (dipendenti.isEmpty()) {
                        _message.value = "Nessun dipendente trovato per la generazione dei turni."
                        _isLoading.value = false
                        return@addOnSuccessListener
                    }

                    // 4. Chiama l'algoritmo passando le richieste filtrate e normalizzate
                    val turniGenerati = settoriAlgorithm.generaTurniDaContratti(
                        dipendenti,
                        parametri,
                        richiesteFiltrate
                    )
                    _turniGeneratiPerSalvataggio.value = turniGenerati // Salva i dati grezzi per un salvataggio futuro

                    if (turniGenerati.isEmpty()) {
                        _message.value = "Nessun turno generato per il periodo selezionato."
                    } else {
                        _message.value = "Anteprima turni generata con successo!"
                    }

                    // Converte i dati per la visualizzazione e aggiorna la UI
                    val turniPerVisualizzazione = turniGenerati.map { turno ->
                        TurnoVisualizzato(
                            id = turno.id,
                            data = turno.data,
                            orario = "${turno.orarioInizio} - ${turno.orarioFine}",
                            descrizione = "Dipendenti: ${turno.dipendentiAssegnati.joinToString(", ")}",
                            settore = turno.settore
                        )
                    }
                    aggiornaProgrammazioneSettimanale(turniPerVisualizzazione)
                    _isLoading.value = false

                }.addOnFailureListener { e -> // Corrisponde a db.collection("Profili")
                    _isLoading.value = false
                    _message.value = "Errore durante il recupero dei profili: ${e.message}"
                }
            }.addOnFailureListener { e -> // Corrisponde a db.collection("requests")
                _isLoading.value = false
                _message.value = "Errore durante il recupero delle richieste: ${e.message}"
            }
    }

    fun salvaTurniGenerati() {
        val turniDaSalvare = _turniGeneratiPerSalvataggio.value ?: return
        if (turniDaSalvare.isEmpty()) {
            _message.value = "Nessun turno da salvare."
            return
        }

        _isLoading.value = true
        val adminId = FirebaseAuth.getInstance().currentUser?.uid
        if (adminId == null) {
            _message.value = "Errore: utente non autenticato. Impossibile salvare."
            _isLoading.value = false
            return
        }

        val batch = db.batch()
        turniDaSalvare.forEach { turno ->
            val turnoMap = hashMapOf(
                "title" to "Turno ${turno.data}",
                "time" to "${turno.orarioInizio} - ${turno.orarioFine}",
                "description" to "Settore: ${turno.settore}, Dipendenti: ${turno.dipendentiAssegnati.joinToString(", ")}",
                "date" to turno.data,
                "userId" to adminId, // Usa l'ID dell'admin corretto
                "workMode" to turno.modalita
            )
            val docRef = db.collection("shifts").document(turno.id)
            batch.set(docRef, turnoMap)
        }

        batch.commit().addOnCompleteListener {
            if (it.isSuccessful) {
                _message.value = "✅ Turni salvati con successo su database!"
                _turniGeneratiPerSalvataggio.value =
                    emptyList() // Pulisce i turni in memoria dopo il salvataggio
            } else {
                _message.value =
                    "Errore durante il salvataggio dei turni: ${it.exception?.message}"
            }
            _isLoading.value = false
        }
    }

    // Funzioni di supporto per estrarre dati dalla descrizione
    private fun extractSettoreFromDescription(description: String): String? {
        // Esempio: "Settore: Magazzino, Dipendenti: ..."
        return description.substringAfter("Settore: ").substringBefore(",")
    }

    private fun extractDipendentiFromDescription(description: String): List<String> {
        // Esempio: "Dipendenti: Mario Rossi, Luca Bianchi"
        return description.substringAfter("Dipendenti: ").split(", ")
    }

    fun clearMessage() {
        _message.value = null
    }

    // Funzione di utilità per convertire una stringa in un enum SettoreLavorativo
    private fun stringToSettoreLavorativo(settoreStr: String?): SettoreLavorativo {
        val settoreNormalizzato = settoreStr?.trim() ?: return SettoreLavorativo.NON_SPECIFICATO

        return when {
            settoreNormalizzato.equals("Vendite", ignoreCase = true) -> SettoreLavorativo.VENDITE
            settoreNormalizzato.equals("Assistenza Clienti", ignoreCase = true) -> SettoreLavorativo.ASSISTENZA_CLIENTI
            settoreNormalizzato.equals("Amministrazione", ignoreCase = true) ||
            settoreNormalizzato.equals("Contabilità", ignoreCase = true) ||
            settoreNormalizzato.equals("Contabilità e amministrazione", ignoreCase = true) -> SettoreLavorativo.AMMINISTRAZIONE
            settoreNormalizzato.equals("Magazzino e Logistica", ignoreCase = true) ||
            settoreNormalizzato.equals("Logistica", ignoreCase = true) -> SettoreLavorativo.LOGISTICA
            settoreNormalizzato.equals("Risorse Umane", ignoreCase = true) -> SettoreLavorativo.RISORSE_UMANE
            settoreNormalizzato.equals("Titolare", ignoreCase = true) -> SettoreLavorativo.TITOLARE
            else -> SettoreLavorativo.NON_SPECIFICATO
        }
    }

    private fun aggiornaProgrammazioneSettimanale(turni: List<TurnoVisualizzato>) {
        val turniPerDipendente = mutableMapOf<String, MutableList<TurnoAssegnato>>()

        // Raggruppa i turni per ogni dipendente
        for (turno in turni) {
            val dipendenti = extractDipendentiFromDescription(turno.descrizione)
            val giornoSettimana = getDayNameFromDate(turno.data)
            if (giornoSettimana.isNotEmpty()) {
                for (dipendente in dipendenti) {
                    val turnoAssegnato =
                        TurnoAssegnato(giornoSettimana, turno.orario, turno.settore ?: "N/D")
                    turniPerDipendente.getOrPut(dipendente) { mutableListOf() }
                        .add(turnoAssegnato)
                }
            }
        }

        val giorniSettimana =
            listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")

        // Crea la lista di programmazioni settimanali
        val programmazioneCompleta = turniPerDipendente.map { (nome, turniAssegnati) ->
            val mappaTurni = giorniSettimana.associateWith { giorno ->
                turniAssegnati.find { it.giorno == giorno }
            }
            ProgrammazioneSettimanalePersona(nome, mappaTurni)
        }.sortedBy { it.nomeDipendente }

        _programmazioneSettimanale.value = programmazioneCompleta
    }

    private fun getDayNameFromDate(dateString: String): String {
        return try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALIAN)
            val date = parser.parse(dateString)
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> "Lunedì"
                java.util.Calendar.TUESDAY -> "Martedì"
                java.util.Calendar.WEDNESDAY -> "Mercoledì"
                java.util.Calendar.THURSDAY -> "Giovedì"
                java.util.Calendar.FRIDAY -> "Venerdì"
                java.util.Calendar.SATURDAY -> "Sabato"
                java.util.Calendar.SUNDAY -> "Domenica"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
