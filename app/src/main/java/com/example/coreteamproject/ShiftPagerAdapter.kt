package com.example.coreteamproject

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter per gestire i fragment delle schede nella sezione Turni.
 * Supporta sia la modalità admin che la modalità dipendente.
 */
class ShiftPagerAdapter(fragment: Fragment, private val isUserMode: Boolean = false) : FragmentStateAdapter(fragment) {

    // Il numero totale di schede da visualizzare.
    override fun getItemCount(): Int = if (isUserMode) 2 else 3

    // Crea e restituisce il fragment appropriato per la posizione data.
    override fun createFragment(position: Int): Fragment {
        return if (isUserMode) {
            // Modalità dipendente: solo 2 schede
            when (position) {
                0 -> OrariContrattoFragment() // Orari Settori (consultazione)
                1 -> VisualizzaTurniFragment() // I Miei Turni (solo i suoi turni)
                else -> throw IllegalStateException("Posizione non valida per la scheda dipendente: $position")
            }
        } else {
            // Modalità admin: tutte e 3 le schede
            when (position) {
                0 -> OrariContrattoFragment() // La prima scheda mostra gli orari da contratto.
                1 -> GeneraTurniFragment()    // La seconda scheda permette di generare i turni.
                2 -> VisualizzaTurniFragment() // La terza scheda visualizza i turni generati.
                else -> throw IllegalStateException("Posizione non valida per la scheda admin: $position")
            }
        }
    }
}
