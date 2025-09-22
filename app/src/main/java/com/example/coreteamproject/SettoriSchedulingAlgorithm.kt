package com.example.coreteamproject

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

// Algoritmo per generare turni basandosi sui contratti individuali dei dipendenti
class SettoriSchedulingAlgorithm {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN)
    private val contratti = ContrattiPredefiniti.getContratti()

    /**
     * Genera i turni per una lista di dipendenti in un dato intervallo di date.
     * L'algoritmo cicla su ogni giorno e, per ogni giorno, su ogni dipendente,
     * creando un turno se il contratto del dipendente lo prevede.
     */
    fun generaTurniDaContratti(
        dipendenti: List<DisponibilitaDipendente>,
        parametri: ParametriScheduling,
        richiesteApprovate: List<Richiesta> = emptyList() // Aggiunto parametro per le richieste
    ): List<TurnoSettore> {

        val turniGenerati = mutableListOf<TurnoSettore>()
        val calendar = Calendar.getInstance()

        // 1. Pre-processa le richieste per un accesso super efficiente
        val ferieMap = mutableMapOf<String, MutableSet<String>>()
        richiesteApprovate.filter { it.tipo == "FERIE" }.forEach { richiesta ->
            val calendar = Calendar.getInstance()
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALIAN)

            val startDate = richiesta.startDate?.toDate() ?: return@forEach
            val endDate = richiesta.endDate?.toDate() ?: startDate

            calendar.time = startDate
            while (!calendar.time.after(endDate)) {
                val dataString = formatter.format(calendar.time)
                ferieMap.getOrPut(richiesta.userId) { mutableSetOf() }.add(dataString)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val permessiMap = richiesteApprovate
            .filter { it.tipo == "PERMESSO_ENTRATA" || it.tipo == "PERMESSO_USCITA" } // Corretto in maiuscolo
            .associateBy { "${it.userId}_${it.data}" }

        Log.d("SchedulingDebug", "--- Mappe Costruite dall'Algoritmo ---")
        Log.d("SchedulingDebug", "Mappa Ferie: $ferieMap")
        Log.d("SchedulingDebug", "Mappa Permessi: $permessiMap")

        try {
            calendar.time = dateFormat.parse(parametri.dataInizio) ?: return emptyList()
        } catch (e: Exception) {
            Log.e("SchedulingAlgorithm", "Data di inizio non valida: ${parametri.dataInizio}", e)
            return emptyList()
        }

        val dataFine = try {
            dateFormat.parse(parametri.dataFine) ?: return emptyList()
        } catch (e: Exception) {
            Log.e("SchedulingAlgorithm", "Data di fine non valida: ${parametri.dataFine}", e)
            return emptyList()
        }

        // Scorre ogni giorno nell'intervallo selezionato
        while (!calendar.time.after(dataFine)) {
            val giornoCorrente = getDayOfWeekAsEnum(calendar.get(Calendar.DAY_OF_WEEK))
            val dataString = dateFormat.format(calendar.time)

            // Salta il weekend se non è incluso nei parametri
            if (!parametri.includiWeekend && (giornoCorrente == GiornoSettimana.SABATO || giornoCorrente == GiornoSettimana.DOMENICA)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                continue
            }

            // Se il giorno non è valido, salta al successivo
            if (giornoCorrente == null) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                continue
            }

            // Itera su ogni singolo dipendente per creare un turno individuale
            for (dipendente in dipendenti) {
                val contratto = contratti[dipendente.settore]

                if (contratto == null) {
                    Log.w(
                        "SchedulingAlgorithm",
                        "Nessun contratto trovato per ${dipendente.nomeCompleto} nel settore '${dipendente.settore.nomeVisualizzato}'"
                    )
                    continue
                }

                // 2. Controlla se il dipendente è in ferie in questo giorno (controllo super veloce)
                val idDipendente = dipendente.userId.trim()
                val dataCorrente = dataString.trim()
                val ferieUtente = ferieMap[idDipendente]

                Log.d("SchedulingDebug", "Controllo Ferie per: UserID='${idDipendente}', Data='${dataCorrente}'. Ferie trovate per l'utente: ${ferieUtente ?: "Nessuna"}")

                if (ferieUtente?.contains(dataCorrente) == true) {
                    Log.i("SchedulingAlgorithm", "SALTATO TURNO per ${dipendente.nomeCompleto} il $dataCorrente causa Ferie.")
                    continue // Passa al prossimo dipendente
                }

                // 3. Trova l'orario di lavoro standard dal contratto
                contratto.orari.find { it.giorno == giornoCorrente }?.let { orarioLavoro ->
                    var orarioInizio = orarioLavoro.orarioInizio
                    var orarioFine = orarioLavoro.orarioFine

                    // 4. Controlla se c'è un permesso per questo giorno (controllo super veloce)
                    val permessoDelGiorno = permessiMap["${dipendente.userId}_${dataString}"]
                    if (permessoDelGiorno != null) {
                        when (permessoDelGiorno.tipo) {
                            "PERMESSO_ENTRATA" -> { // Corretto in maiuscolo
                                orarioInizio = permessoDelGiorno.orario ?: orarioInizio
                                Log.i("SchedulingAlgorithm", "Modificato orario inizio per ${dipendente.nomeCompleto} a $orarioInizio")
                            }
                            "PERMESSO_USCITA" -> { // Corretto in maiuscolo
                                orarioFine = permessoDelGiorno.orario ?: orarioFine
                                Log.i("SchedulingAlgorithm", "Modificato orario fine per ${dipendente.nomeCompleto} a $orarioFine")
                            }
                        }
                    }

                    // 5. Crea il turno con gli orari (potenzialmente modificati)
                    val turnoId = "${dipendente.userId}_${dataString}"
                    turniGenerati.add(
                        TurnoSettore(
                            id = turnoId,
                            data = dataString,
                            orarioInizio = orarioInizio,
                            orarioFine = orarioFine,
                            settore = contratto.settore.nomeVisualizzato,
                            dipendentiAssegnati = listOf(dipendente.nomeCompleto),
                            modalita = "presenza"
                        )
                    )
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.i("SchedulingAlgorithm", "Generazione completata. Creati ${turniGenerati.size} turni.")
        return turniGenerati
    }

    /**
     * Converte il valore di Calendar.DAY_OF_WEEK nell'enum GiornoSettimana.
     */
    private fun getDayOfWeekAsEnum(dayOfWeek: Int): GiornoSettimana? {
        return when (dayOfWeek) {
            Calendar.MONDAY -> GiornoSettimana.LUNEDI
            Calendar.TUESDAY -> GiornoSettimana.MARTEDI
            Calendar.WEDNESDAY -> GiornoSettimana.MERCOLEDI
            Calendar.THURSDAY -> GiornoSettimana.GIOVEDI
            Calendar.FRIDAY -> GiornoSettimana.VENERDI
            Calendar.SATURDAY -> GiornoSettimana.SABATO
            Calendar.SUNDAY -> GiornoSettimana.DOMENICA
            else -> null
        }
    }
}
