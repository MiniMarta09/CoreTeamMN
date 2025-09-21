package com.example.coreteamproject

// Modelli per la gestione dei settori aziendali e i loro orari

// Settore aziendale con i suoi orari di funzionamento
data class SettoreAziendale(
    val id: String = "",
    val nome: String = "",
    val descrizione: String = "",
    val orariSettimana: Map<String, List<String>> = emptyMap(), // giorno -> orari (es. "lunedi" -> ["08:00-12:00", "14:00-18:00"])
    val dipendentiAssegnati: List<String> = emptyList(), // Lista nomi dipendenti del settore
    val colore: String = "#6200EA" // Colore per identificare il settore
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

// Configurazione settori predefiniti
object SettoriPredefiniti {
    
    fun getSettoriDefault(): List<SettoreAziendale> {
        return listOf(
            SettoreAziendale(
                id = "assistenza_clienti",
                nome = "Assistenza Clienti",
                descrizione = "Supporto e assistenza ai clienti",
                orariSettimana = mapOf(
                    "lunedi" to listOf("08:00-12:00", "14:00-18:00"),
                    "martedi" to listOf("08:00-12:00", "14:00-18:00"),
                    "mercoledi" to listOf("08:00-12:00", "14:00-18:00"),
                    "giovedi" to listOf("08:00-12:00", "14:00-18:00"),
                    "venerdi" to listOf("08:00-12:00", "14:00-18:00")
                ),
                colore = "#2196F3"
            ),
            SettoreAziendale(
                id = "vendite",
                nome = "Vendite",
                descrizione = "Reparto vendite e commerciale",
                orariSettimana = mapOf(
                    "lunedi" to listOf("09:00-13:00", "15:00-19:00"),
                    "martedi" to listOf("09:00-13:00", "15:00-19:00"),
                    "mercoledi" to listOf("09:00-13:00", "15:00-19:00"),
                    "giovedi" to listOf("09:00-13:00", "15:00-19:00"),
                    "venerdi" to listOf("09:00-13:00", "15:00-19:00"),
                    "sabato" to listOf("09:00-13:00")
                ),
                colore = "#4CAF50"
            ),
            SettoreAziendale(
                id = "amministrazione",
                nome = "Amministrazione",
                descrizione = "Ufficio amministrativo e contabilità",
                orariSettimana = mapOf(
                    "lunedi" to listOf("08:30-12:30", "13:30-17:30"),
                    "martedi" to listOf("08:30-12:30", "13:30-17:30"),
                    "mercoledi" to listOf("08:30-12:30", "13:30-17:30"),
                    "giovedi" to listOf("08:30-12:30", "13:30-17:30"),
                    "venerdi" to listOf("08:30-12:30", "13:30-17:30")
                ),
                colore = "#FF9800"
            ),
            SettoreAziendale(
                id = "tecnico",
                nome = "Tecnico",
                descrizione = "Supporto tecnico e manutenzione",
                orariSettimana = mapOf(
                    "lunedi" to listOf("07:00-15:00"),
                    "martedi" to listOf("07:00-15:00"),
                    "mercoledi" to listOf("07:00-15:00"),
                    "giovedi" to listOf("07:00-15:00"),
                    "venerdi" to listOf("07:00-15:00"),
                    "sabato" to listOf("08:00-12:00")
                ),
                colore = "#9C27B0"
            ),
            SettoreAziendale(
                id = "logistica",
                nome = "Logistica",
                descrizione = "Magazzino e spedizioni",
                orariSettimana = mapOf(
                    "lunedi" to listOf("06:00-14:00"),
                    "martedi" to listOf("06:00-14:00"),
                    "mercoledi" to listOf("06:00-14:00"),
                    "giovedi" to listOf("06:00-14:00"),
                    "venerdi" to listOf("06:00-14:00")
                ),
                colore = "#795548"
            )
        )
    }
}
