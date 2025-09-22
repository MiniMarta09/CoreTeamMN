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
        parametri: ParametriScheduling
    ): List<TurnoSettore> {

        val turniGenerati = mutableListOf<TurnoSettore>()
        val calendar = Calendar.getInstance()

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

                // Trova l'orario di lavoro per il giorno corrente in base al contratto del dipendente
                contratto.orari.find { it.giorno == giornoCorrente }?.let { orarioLavoro ->
                    // Crea un turno specifico per questo dipendente
                    val turnoId = "${dipendente.userId}_${dataString}" // ID univoco per dipendente e giorno

                    Log.d(
                        "SchedulingAlgorithm",
                        "Creato turno per ${dipendente.nomeCompleto} nel settore '${contratto.settore.nomeVisualizzato}' il $giornoCorrente"
                    )

                    turniGenerati.add(
                        TurnoSettore(
                            id = turnoId,
                            data = dataString,
                            orarioInizio = orarioLavoro.orarioInizio,
                            orarioFine = orarioLavoro.orarioFine,
                            settore = contratto.settore.nomeVisualizzato,
                            dipendentiAssegnati = listOf(dipendente.nomeCompleto), // Assegna solo il singolo dipendente
                            modalita = "presenza" // Default
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
