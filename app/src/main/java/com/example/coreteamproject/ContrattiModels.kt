package com.example.coreteamproject

// Enum per standardizzare i nomi dei settori ed evitare errori di battitura
enum class SettoreLavorativo(val nomeVisualizzato: String) {
    VENDITE("Vendite"),
    ASSISTENZA_CLIENTI("Assistenza Clienti"),
    AMMINISTRAZIONE("Contabilità e amministrazione"),
    LOGISTICA("Magazzino e Logistica"),
    RISORSE_UMANE("Risorse Umane"),
    TITOLARE("Titolare"),
    NON_SPECIFICATO("Non Specificato")
}

// Enum per i giorni della settimana, per evitare errori di battitura
enum class GiornoSettimana {
    LUNEDI, MARTEDI, MERCOLEDI, GIOVEDI, VENERDI, SABATO, DOMENICA
}

data class OrarioLavoro(
    val giorno: GiornoSettimana,
    val orarioInizio: String,
    val orarioFine: String
)

data class ContrattoTipo(
    val settore: SettoreLavorativo,
    val orari: List<OrarioLavoro>
)

// Turno generato per un settore specifico
data class TurnoSettore(
    val id: String = "",
    val data: String = "",
    val orarioInizio: String = "",
    val orarioFine: String = "",
    val settore: String = "",
    val dipendentiAssegnati: List<String> = emptyList(),
    val modalita: String = "presenza"
)

// Data class per rappresentare una richiesta di ferie/permesso
data class Richiesta(
    val userId: String = "",
    val data: String = "",
    val tipo: String = "", // Es. "Ferie", "Permesso Entrata", "Permesso Uscita"
    val stato: String = "", // Es. "In attesa", "Accettata", "Rifiutata"
    val orario: String? = null, // Per permessi orari
    val startDate: com.google.firebase.Timestamp? = null,
    val endDate: com.google.firebase.Timestamp? = null
)

object ContrattiPredefiniti {
    fun getContratti(): Map<SettoreLavorativo, ContrattoTipo> {
        return mapOf(
            SettoreLavorativo.VENDITE to ContrattoTipo(
                settore = SettoreLavorativo.VENDITE,
                orari = listOf(
                    OrarioLavoro(GiornoSettimana.LUNEDI, "09:00", "18:00"),
                    OrarioLavoro(GiornoSettimana.MARTEDI, "09:00", "18:00"),
                    OrarioLavoro(GiornoSettimana.MERCOLEDI, "09:00", "18:00"),
                    OrarioLavoro(GiornoSettimana.GIOVEDI, "09:00", "18:00"),
                    OrarioLavoro(GiornoSettimana.VENERDI, "09:00", "18:00")
                )
            ),
            SettoreLavorativo.ASSISTENZA_CLIENTI to ContrattoTipo(
                settore = SettoreLavorativo.ASSISTENZA_CLIENTI,
                orari = listOf(
                    OrarioLavoro(GiornoSettimana.LUNEDI, "08:00", "12:00"),
                    OrarioLavoro(GiornoSettimana.MARTEDI, "08:00", "12:00"),
                    OrarioLavoro(GiornoSettimana.MERCOLEDI, "08:00", "12:00")
                )
            ),
            SettoreLavorativo.AMMINISTRAZIONE to ContrattoTipo(
                settore = SettoreLavorativo.AMMINISTRAZIONE,
                orari = listOf(
                    OrarioLavoro(GiornoSettimana.LUNEDI, "09:00", "17:30"),
                    OrarioLavoro(GiornoSettimana.MARTEDI, "09:00", "17:30"),
                    OrarioLavoro(GiornoSettimana.MERCOLEDI, "09:00", "17:30"),
                    OrarioLavoro(GiornoSettimana.GIOVEDI, "09:00", "17:30"),
                    OrarioLavoro(GiornoSettimana.VENERDI, "09:00", "17:30")
                )
            ),
            SettoreLavorativo.LOGISTICA to ContrattoTipo(
                settore = SettoreLavorativo.LOGISTICA,
                orari = listOf(
                    OrarioLavoro(GiornoSettimana.LUNEDI, "08:30", "17:30"),
                    OrarioLavoro(GiornoSettimana.MARTEDI, "08:30", "17:30"),
                    OrarioLavoro(GiornoSettimana.MERCOLEDI, "08:30", "17:30"),
                    OrarioLavoro(GiornoSettimana.GIOVEDI, "08:30", "17:30"),
                    OrarioLavoro(GiornoSettimana.VENERDI, "08:30", "17:30")
                )
            ),
            SettoreLavorativo.RISORSE_UMANE to ContrattoTipo(
                settore = SettoreLavorativo.RISORSE_UMANE,
                orari = listOf(
                    OrarioLavoro(GiornoSettimana.MARTEDI, "14:00", "18:00"),
                    OrarioLavoro(GiornoSettimana.GIOVEDI, "14:00", "18:00")
                )
            ),
            SettoreLavorativo.TITOLARE to ContrattoTipo(
                settore = SettoreLavorativo.TITOLARE,
                orari = listOf(
                    OrarioLavoro(GiornoSettimana.LUNEDI, "09:00", "19:00"),
                    OrarioLavoro(GiornoSettimana.MARTEDI, "09:00", "19:00"),
                    OrarioLavoro(GiornoSettimana.MERCOLEDI, "09:00", "19:00"),
                    OrarioLavoro(GiornoSettimana.GIOVEDI, "09:00", "19:00"),
                    OrarioLavoro(GiornoSettimana.VENERDI, "09:00", "19:00"),
                    OrarioLavoro(GiornoSettimana.SABATO, "09:00", "13:00")
                )
            )
        )
    }
}
