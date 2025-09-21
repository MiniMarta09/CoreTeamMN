package com.example.coreteamproject

// Parametri per generare turni automatici
data class ParametriScheduling(
    val dataInizio: String = "",
    val dataFine: String = "",
    val includiWeekend: Boolean = false
)

// Turno generato dall'algoritmo
data class TurnoGenerato(
    val id: String = "",
    val data: String = "",
    val orarioInizio: String = "",
    val orarioFine: String = "",
    val dipendenti: List<String> = emptyList(),
    val modalita: String = "presenza"
)
