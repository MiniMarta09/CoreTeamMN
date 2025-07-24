package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

// Fragment per la gestione schermata home
class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurazione pulsante diario
        val buttonDiary: Button = view.findViewById(R.id.buttonDiary)
        buttonDiary.setOnClickListener {
            // Avvia una transazione di fragment per navigare al DiaryFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DiaryFragment())
                .addToBackStack(null)
                .commit()
        }

        // Configurazione pulsanti eventi
        val buttonEvent: Button = view.findViewById(R.id.buttonEvent)
        buttonEvent.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EventFragment())
                .addToBackStack(null)
                .commit()
        }

       // Configurazione pulsante turni
        val buttonShift: Button = view.findViewById(R.id.buttonShift)
        buttonShift.setOnClickListener {
            // Navigazione verso fragment dei turni
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ShiftFragment())
                .addToBackStack(null)
                .commit()
        }

        // Bottone team
        val buttonTeam: Button = view.findViewById(R.id.buttonTeam)
        buttonTeam.setOnClickListener {
            // Navigazione al fragment del team
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TeamFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}