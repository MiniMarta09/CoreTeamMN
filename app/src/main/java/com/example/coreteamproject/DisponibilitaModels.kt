package com.example.coreteamproject

// Data class semplice per rappresentare la disponibilità di un dipendente
data class DisponibilitaDipendente(
    val userId: String = "",
    val nomeCompleto: String = "",
    val email: String = "",
    val disponibilita: Map<String, List<String>> = emptyMap()
)
