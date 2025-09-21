package com.example.coreteamproject

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

// Algoritmo intelligente che rispetta le disponibilità REALI dei dipendenti
class SmartSchedulingAlgorithm {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
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
     * Genera turni rispettando le disponibilità REALI dei dipendenti
     */
    fun generaTurniIntelligenti(
        disponibilita: List<DisponibilitaDipendente>,
        parametri: ParametriScheduling
    ): List<TurnoGenerato> {
        
        val turniGenerati = mutableListOf<TurnoGenerato>()
        
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
                
                // Genera turni per questo giorno
                val turniGiorno = generaTurniPerGiorno(dataString, giornoSettimana, disponibilita)
                turniGenerati.addAll(turniGiorno)
                
                currentDate.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            Log.d("SmartScheduling", "Generati ${turniGenerati.size} turni intelligenti")
            
        } catch (e: Exception) {
            Log.e("SmartScheduling", "Errore generazione turni", e)
        }
        
        return turniGenerati
    }
    
    /**
     * Genera turni per un singolo giorno rispettando le disponibilità
     */
    private fun generaTurniPerGiorno(
        data: String,
        giornoSettimana: Int,
        disponibilita: List<DisponibilitaDipendente>
    ): List<TurnoGenerato> {
        
        val turni = mutableListOf<TurnoGenerato>()
        val nomeGiorno = giorniSettimana[giornoSettimana] ?: return emptyList()
        
        // Trova dipendenti disponibili per questo giorno
        val dipendentiDisponibili = disponibilita.filter { dipendente ->
            dipendente.disponibilita.containsKey(nomeGiorno) && 
            dipendente.disponibilita[nomeGiorno]?.isNotEmpty() == true
        }
        
        if (dipendentiDisponibili.isEmpty()) {
            Log.d("SmartScheduling", "Nessun dipendente disponibile per $data ($nomeGiorno)")
            return emptyList()
        }
        
        // Raggruppa dipendenti per fasce orarie
        val fasceOrarie = raggruppaPerFasceOrarie(dipendentiDisponibili, nomeGiorno)
        
        // Crea turni per ogni fascia oraria
        for ((fasciaOraria, dipendentiFascia) in fasceOrarie) {
            if (dipendentiFascia.isNotEmpty()) {
                val turno = creaTurnoPerFascia(data, fasciaOraria, dipendentiFascia)
                if (turno != null) {
                    turni.add(turno)
                }
            }
        }
        
        return turni
    }
    
    /**
     * Raggruppa dipendenti per fasce orarie compatibili
     */
    private fun raggruppaPerFasceOrarie(
        dipendenti: List<DisponibilitaDipendente>,
        nomeGiorno: String
    ): Map<String, List<String>> {
        
        val fasceOrarie = mutableMapOf<String, MutableList<String>>()
        
        for (dipendente in dipendenti) {
            val orariDisponibili = dipendente.disponibilita[nomeGiorno] ?: continue
            
            for (orario in orariDisponibili) {
                // Normalizza la fascia oraria
                val fasciaNormalizzata = normalizzaFasciaOraria(orario)
                if (fasciaNormalizzata.isNotEmpty()) {
                    fasceOrarie.getOrPut(fasciaNormalizzata) { mutableListOf() }
                        .add(dipendente.nomeCompleto)
                }
            }
        }
        
        return fasceOrarie
    }
    
    /**
     * Normalizza una fascia oraria (es. "08:00-12:00" -> "08:00-12:00")
     */
    private fun normalizzaFasciaOraria(orario: String): String {
        return when {
            orario.contains("-") -> {
                val parti = orario.split("-")
                if (parti.size == 2) {
                    val inizio = parti[0].trim()
                    val fine = parti[1].trim()
                    "$inizio-$fine"
                } else {
                    ""
                }
            }
            else -> {
                // Se è un orario singolo, crea una fascia di 4 ore
                try {
                    val ora = timeFormat.parse(orario.trim())
                    if (ora != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = ora
                        val inizio = timeFormat.format(calendar.time)
                        calendar.add(Calendar.HOUR_OF_DAY, 4)
                        val fine = timeFormat.format(calendar.time)
                        "$inizio-$fine"
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    ""
                }
            }
        }
    }
    
    /**
     * Crea un turno per una specifica fascia oraria
     */
    private fun creaTurnoPerFascia(
        data: String,
        fasciaOraria: String,
        dipendentiFascia: List<String>
    ): TurnoGenerato? {
        
        if (fasciaOraria.isEmpty() || dipendentiFascia.isEmpty()) return null
        
        val parti = fasciaOraria.split("-")
        if (parti.size != 2) return null
        
        val orarioInizio = parti[0].trim()
        val orarioFine = parti[1].trim()
        
        // Seleziona massimo 2-3 dipendenti per turno
        val dipendentiSelezionati = dipendentiFascia.shuffled().take(
            minOf(3, dipendentiFascia.size)
        )
        
        // Determina modalità (70% presenza, 30% smartworking)
        val modalita = if (kotlin.random.Random.nextDouble() < 0.7) "presenza" else "smartworking"
        
        return TurnoGenerato(
            id = UUID.randomUUID().toString(),
            data = data,
            orarioInizio = orarioInizio,
            orarioFine = orarioFine,
            dipendenti = dipendentiSelezionati,
            modalita = modalita
        )
    }
    
    /**
     * Valida e ottimizza i turni generati
     */
    fun validaTurni(turni: List<TurnoGenerato>): List<TurnoGenerato> {
        val turniValidati = mutableListOf<TurnoGenerato>()
        val dipendentiOccupati = mutableMapOf<String, MutableSet<String>>() // Data -> Dipendenti
        
        // Ordina per data e orario
        val turniOrdinati = turni.sortedWith(compareBy({ it.data }, { it.orarioInizio }))
        
        for (turno in turniOrdinati) {
            val occupatiGiorno = dipendentiOccupati.getOrPut(turno.data) { mutableSetOf() }
            
            // Filtra dipendenti non ancora occupati
            val dipendentiLiberi = turno.dipendenti.filter { dipendente ->
                !occupatiGiorno.contains(dipendente)
            }
            
            // Se ci sono almeno 1 dipendente libero, crea il turno
            if (dipendentiLiberi.isNotEmpty()) {
                val turnoValidato = turno.copy(dipendenti = dipendentiLiberi)
                turniValidati.add(turnoValidato)
                
                // Marca dipendenti come occupati
                occupatiGiorno.addAll(dipendentiLiberi)
            }
        }
        
        Log.d("SmartScheduling", "Validati ${turniValidati.size} turni su ${turni.size}")
        return turniValidati
    }
}
