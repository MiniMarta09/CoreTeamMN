package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

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

        // Imposta i listener per i bottoni di navigazione principali.
        // Ogni bottone naviga verso il rispettivo fragment utilizzando un'azione definita in nav_graph.xml.

        view.findViewById<Button>(R.id.buttonDiary).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_diaryFragment)
        }

        view.findViewById<Button>(R.id.buttonEvent).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_eventFragment)
        }

        view.findViewById<Button>(R.id.buttonShift).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_shiftFragment)
        }

        view.findViewById<Button>(R.id.buttonTeam).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_teamFragment)
        }

        // Listener per il bottone "Bacheca".
        // Naviga al BoardFragment, che mostra la bacheca degli annunci.
        view.findViewById<Button>(R.id.buttonBacheca).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_boardFragment)
        }

        view.findViewById<Button>(R.id.buttonRequests).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_requestsFragment)
        }
    }
}