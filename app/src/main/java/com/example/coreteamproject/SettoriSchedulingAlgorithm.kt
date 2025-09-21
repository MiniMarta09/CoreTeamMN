package com.example.coreteamproject

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

// Algoritmo per generare turni basandosi sui settori aziendali
class SettoriSchedulingAlgorithm {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Mappa giorni settimana
    private val giorniSettimana = mapOf(
        Calendar.MONDAY to "lunedi",
        Calendar.TUESDAY to "martedi", 
        Calendar.WEDNESDAY to "mercoledi",
        Calendar.THURSDAY to "giovedi",
        Calendar.FRIDAY to "venerdi",
        Calendar.SATURDAY to "sabato",
        Calendar.SUNDAY to "domenica"
    )
    
    /**
     * Genera turni basandosi sui settori aziendali
     */
    fun generaTurniPerSettori(
        settori: List<SettoreAziendale>,
        dipendenti: List<DisponibilitaDipendente>,
        parametri: ParametriScheduling
    ): List<TurnoSettore> {
        
        val turniGenerati = mutableListOf<TurnoSettore>()
        
        try {
            val dataInizio = dateFormat.parse(parametri.dataInizio) ?: return emptyList()
            val dataFine = dateFormat.parse(parametri.dataFine) ?: return emptyList()
            
            val calendarInizio = Calendar.getInstance().apply { time = dataInizio }
            val calendarFine = Calendar.getInstance().apply { time = dataFine }
            
            val currentDate = calendarInizio.clone() as Calendar
            
            while (currentDate.before(calendarFine) || currentDate.equals(calendarFine)) {
                val giornoSettimana = currentDate.get(Calendar.DAY_OF_WEEK)
                val dataString = dateFormat.format(currentDate.time)
                
                // Salta weekend se non inclusi
                if (!parametri.includiWeekend && 
                    (giornoSettimana == Calendar.SATURDAY || giornoSettimana == Calendar.SUNDAY)) {
                    currentDate.add(Calendar.DAY_OF_MONTH, 1)
                    continue
                }
                
                // Genera turni per ogni settore in questo giorno
                val turniGiorno = generaTurniPerGiornoSettori(dataString, giornoSettimana, settori, dipendenti)
                turniGenerati.addAll(turniGiorno)
                
                currentDate.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            Log.d("SettoriScheduling", "Generati ${turniGenerati.size} turni per settori")
            
        } catch (e: Exception) {
            Log.e("SettoriScheduling", "Errore generazione turni settori", e)
        }
        
        return turniGenerati
    }
    
    /**
     * Genera turni per un singolo giorno per tutti i settori
     */
    private fun generaTurniPerGiornoSettori(
        data: String,
        giornoSettimana: Int,
        settori: List<SettoreAziendale>,
        dipendenti: List<DisponibilitaDipendente>
    ): List<TurnoSettore> {
        
        val turni = mutableListOf<TurnoSettore>()
        val nomeGiorno = giorniSettimana[giornoSettimana] ?: return emptyList()
        val orariDipendenti = mutableMapOf<String, MutableList<Pair<String, String>>>() // dipendente -> lista (inizio, fine)
        
        for (settore in settori) {
            // Controlla se il settore lavora in questo giorno
            val orariSettore = settore.orariSettimana[nomeGiorno]
            if (orariSettore.isNullOrEmpty()) continue
            
            // Filtra dipendenti di questo settore specifico con matching flessibile
            val dipendentiDelSettore = dipendenti.filter { dipendente ->
                val settoreDipendente = dipendente.settore.trim().lowercase()
                val settoreId = settore.id.lowercase()
                val settoreNome = settore.nome.lowercase()
                
                // Prova varie combinazioni
                settoreDipendente == settoreId || 
                settoreDipendente == settoreNome ||
                settoreDipendente.contains(settoreId.replace("_", "")) ||
                settoreId.contains(settoreDipendente) ||
                settoreNome.contains(settoreDipendente) ||
                settoreDipendente.contains(settoreNome.split(" ").first())
            }
            
            // Se non trova dipendenti per questo settore, usa fallback
            val dipendentiFinali = if (dipendentiDelSettore.isEmpty()) {
                // Fallback: usa alcuni dipendenti casuali per questo settore
                dipendenti.shuffled().take(2)
            } else {
                dipendentiDelSettore
            }
            
            // Genera turni per ogni fascia oraria del settore (senza sovrapposizioni)
            for (orario in orariSettore) {
                val turno = creaTurnoPerSettoreSenzaSovrapposizioni(data, settore, orario, dipendentiFinali, orariDipendenti)
                if (turno != null) {
                    turni.add(turno)
                    // Aggiorna gli orari occupati dai dipendenti
                    for (dipendente in turno.dipendentiAssegnati) {
                        if (!orariDipendenti.containsKey(dipendente)) {
                            orariDipendenti[dipendente] = mutableListOf()
                        }
                        orariDipendenti[dipendente]?.add(Pair(turno.orarioInizio, turno.orarioFine))
                    }
                }
            }
        }
        
        return turni
    }
    
    /**
     * Crea un turno per un settore controllando le sovrapposizioni orarie
     */
    private fun creaTurnoPerSettoreSenzaSovrapposizioni(
        data: String,
        settore: SettoreAziendale,
        orario: String,
        dipendenti: List<DisponibilitaDipendente>,
        orariDipendenti: Map<String, List<Pair<String, String>>>
    ): TurnoSettore? {
        
        // Parsing dell'orario (es. "08:00-12:00")
        val parti = orario.split("-")
        if (parti.size != 2) return null
        
        val orarioInizio = parti[0].trim()
        val orarioFine = parti[1].trim()
        
        // Filtra dipendenti che NON hanno sovrapposizioni con questo orario
        val dipendentiDisponibili = dipendenti.filter { dipendente ->
            val orariOccupati = orariDipendenti[dipendente.nomeCompleto] ?: emptyList()
            !hasSovrapposizione(orarioInizio, orarioFine, orariOccupati)
        }
        
        if (dipendentiDisponibili.isEmpty()) return null
        
        // Seleziona dipendenti (massimo 2-3 per turno)
        val nomiDipendenti = dipendentiDisponibili.map { it.nomeCompleto }
        val dipendentiSelezionati = nomiDipendenti.shuffled().take(
            when {
                nomiDipendenti.size == 1 -> 1
                nomiDipendenti.size <= 3 -> 2
                else -> 3
            }
        )
        
        // Determina modalità
        val probabilitaPresenza = when (settore.id) {
            "assistenza_clienti", "vendite", "tecnico", "logistica" -> 0.8
            "amministrazione" -> 0.5
            else -> 0.7
        }
        
        val modalita = if (kotlin.random.Random.nextDouble() < probabilitaPresenza) "presenza" else "smartworking"
        
        return TurnoSettore(
            id = UUID.randomUUID().toString(),
            data = data,
            orarioInizio = orarioInizio,
            orarioFine = orarioFine,
            settore = settore.nome,
            dipendentiAssegnati = dipendentiSelezionati,
            modalita = modalita
        )
    }
    
    /**
     * Controlla se un nuovo orario si sovrappone con orari già assegnati
     */
    private fun hasSovrapposizione(
        nuovoInizio: String,
        nuovaFine: String,
        orariEsistenti: List<Pair<String, String>>
    ): Boolean {
        for ((inizioEsistente, fineEsistente) in orariEsistenti) {
            // Controlla sovrapposizione: nuovo inizia prima che finisca l'esistente E nuovo finisce dopo che inizia l'esistente
            if (nuovoInizio < fineEsistente && nuovaFine > inizioEsistente) {
                return true
            }
        }
        return false
    }
    
    /**
     * Crea un turno per un settore specifico usando SOLO i dipendenti di quel settore (VECCHIO)
     */
    private fun creaTurnoPerSettore(
        data: String,
        settore: SettoreAziendale,
        orario: String,
        dipendenti: List<DisponibilitaDipendente>
    ): TurnoSettore? {
        
        // Parsing dell'orario (es. "08:00-12:00")
        val parti = orario.split("-")
        if (parti.size != 2) return null
        
        val orarioInizio = parti[0].trim()
        val orarioFine = parti[1].trim()
        
        // Usa i dipendenti del settore (già filtrati)
        val dipendentiDisponibili = if (dipendenti.isNotEmpty()) {
            // Usa dipendenti del settore (massimo 2-3 per turno)
            val nomiDipendenti = dipendenti.map { it.nomeCompleto }
            nomiDipendenti.shuffled().take(
                when {
                    nomiDipendenti.size == 1 -> 1
                    nomiDipendenti.size <= 3 -> 2
                    else -> 3
                }
            )
        } else {
            // Se il settore non ha dipendenti, non creare turni
            emptyList()
        }
        
        if (dipendentiDisponibili.isEmpty()) return null
        
        // Determina modalità (80% presenza per settori operativi, 50% per amministrativi)
        val probabilitaPresenza = when (settore.id) {
            "assistenza_clienti", "vendite", "tecnico", "logistica" -> 0.8
            "amministrazione" -> 0.5
            else -> 0.7
        }
        
        val modalita = if (kotlin.random.Random.nextDouble() < probabilitaPresenza) "presenza" else "smartworking"
        
        val turno = TurnoSettore(
            id = UUID.randomUUID().toString(),
            data = data,
            orarioInizio = orarioInizio,
            orarioFine = orarioFine,
            settore = settore.nome,
            dipendentiAssegnati = dipendentiDisponibili,
            modalita = modalita
        )
        
        return turno
    }
    
    /**
     * Crea un turno unico per settore unendo tutte le fasce orarie
     */
    private fun creaTurnoUnicoPerSettore(
        data: String,
        settore: SettoreAziendale,
        orariSettore: List<String>,
        dipendenti: List<DisponibilitaDipendente>
    ): TurnoSettore? {
        
        if (orariSettore.isEmpty() || dipendenti.isEmpty()) return null
        
        // Trova l'orario più ampio (dal primo inizio all'ultima fine)
        val orariParsed = orariSettore.mapNotNull { orario ->
            val parti = orario.split("-")
            if (parti.size == 2) {
                Pair(parti[0].trim(), parti[1].trim())
            } else null
        }
        
        if (orariParsed.isEmpty()) return null
        
        val orarioInizio = orariParsed.minByOrNull { it.first }?.first ?: return null
        val orarioFine = orariParsed.maxByOrNull { it.second }?.second ?: return null
        
        // Seleziona dipendenti (massimo 2-3 per turno)
        val nomiDipendenti = dipendenti.map { it.nomeCompleto }
        val dipendentiSelezionati = nomiDipendenti.shuffled().take(
            when {
                nomiDipendenti.size == 1 -> 1
                nomiDipendenti.size <= 3 -> 2
                else -> 3
            }
        )
        
        // Determina modalità
        val probabilitaPresenza = when (settore.id) {
            "assistenza_clienti", "vendite", "tecnico", "logistica" -> 0.8
            "amministrazione" -> 0.5
            else -> 0.7
        }
        
        val modalita = if (kotlin.random.Random.nextDouble() < probabilitaPresenza) "presenza" else "smartworking"
        
        return TurnoSettore(
            id = UUID.randomUUID().toString(),
            data = data,
            orarioInizio = orarioInizio,
            orarioFine = orarioFine,
            settore = settore.nome,
            dipendentiAssegnati = dipendentiSelezionati,
            modalita = modalita
        )
    }
    
    /**
     * Converte TurnoSettore in TurnoGenerato per compatibilità
     */
    fun convertiPerVisualizzazione(turniSettori: List<TurnoSettore>): List<TurnoGenerato> {
        return turniSettori.map { turnoSettore ->
            TurnoGenerato(
                id = turnoSettore.id,
                data = turnoSettore.data,
                orarioInizio = turnoSettore.orarioInizio,
                orarioFine = turnoSettore.orarioFine,
                dipendenti = turnoSettore.dipendentiAssegnati,
                modalita = turnoSettore.modalita
            )
        }
    }
    
    /**
     * Raggruppa turni per settore e giorno per una visualizzazione migliore
     */
    fun raggruppaTurniPerSettoreEGiorno(turni: List<TurnoSettore>): Map<String, Map<String, List<TurnoSettore>>> {
        return turni.groupBy { it.data }
            .mapValues { (_, turniGiorno) ->
                turniGiorno.groupBy { it.settore }
            }
    }
}
