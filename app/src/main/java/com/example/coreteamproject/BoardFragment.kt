package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * BoardFragment è responsabile della visualizzazione della bacheca degli annunci.
 * Attualmente, si limita a mostrare un layout statico, ma è predisposto
 * per future implementazioni che caricheranno dati dinamici (es. da Firestore).
 */
class BoardFragment : Fragment() {

    /**
     * Chiamato per creare la gerarchia di viste del fragment.
     * In questo caso, viene "gonfiato" (inflated) il layout definito in `fragment_board.xml`.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Restituisce la vista del layout per questo fragment
        return inflater.inflate(R.layout.fragment_board, container, false)
    }
}