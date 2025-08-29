package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.coreteamproject.databinding.FragmentDiaryBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button

// Fragment per gestire il diario
class DiaryFragment : Fragment() {

    // Variabili per databinding e viewmodel
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var viewModel: DiaryViewModel

    // Metodo chiamato alla creazione del fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_diary, container, false
        )

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[DiaryViewModel::class.java]

        // Imposta il ViewModel nel binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Configura gli observer
        setupObservers()

        // Imposta i listener per i pulsanti
        setupListeners()

        return binding.root
    }

    // Configura gli observer per i LiveData del ViewModel
    private fun setupObservers() {
        // Observer per la lista di valutazioni
        viewModel.valutazioni.observe(viewLifecycleOwner, Observer { valutazioni ->
            aggiornaListaValutazioni(valutazioni)
        })

        // Observer per gli errori
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.resetError()
            }
        })

        // Observer per il successo del salvataggio
        viewModel.saveSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireContext(), "Valutazione salvata!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveSuccess()
            }
        })

        // Observer per lo stato vuoto
        viewModel.isEmpty.observe(viewLifecycleOwner, Observer { isEmpty ->
            if (isEmpty) {
                mostraMessaggioVuoto()
            }
        })
    }

    // Configura il listener per il pulsante di aggiunta
    private fun setupListeners() {
        binding.btnAggiungiValutazione.setOnClickListener {
            showValutazioneDialog()
        }
    }

    // Mostra il dialog per inserire una nuova valutazione
    private fun showValutazioneDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_diary_entry, null)
        dialog.setContentView(view)

        val seekbarStress = view.findViewById<SeekBar>(R.id.seekbar_stress)
        val seekbarColleghi = view.findViewById<SeekBar>(R.id.seekbar_colleghi)
        val seekbarSoddisfazione = view.findViewById<SeekBar>(R.id.seekbar_soddisfazione)
        val editCommento = view.findViewById<TextInputEditText>(R.id.edit_commento)
        val btnSalva = view.findViewById<Button>(R.id.btn_salva)
        val btnAnnulla = view.findViewById<Button>(R.id.btn_annulla)

        btnSalva.setOnClickListener {
            val stress = seekbarStress.progress + 1
            val colleghi = seekbarColleghi.progress + 1
            val soddisfazione = seekbarSoddisfazione.progress + 1
            val commento = editCommento.text.toString()

            viewModel.salvaValutazione(stress, colleghi, soddisfazione, commento)
            dialog.dismiss()
        }

        btnAnnulla.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Aggiorna la UI con la lista di valutazioni
    private fun aggiornaListaValutazioni(valutazioni: List<ValutazioneMensile>) {
        binding.recyclerValutazioni.removeAllViews()
        if (valutazioni.isEmpty()) {
            mostraMessaggioVuoto()
        } else {
            valutazioni.forEach { valutazione ->
                val card = creaValutazioneCard(valutazione)
                binding.recyclerValutazioni.addView(card)
            }
        }
    }

    // Mostra un messaggio se la lista di valutazioni è vuota
    private fun mostraMessaggioVuoto() {
        binding.recyclerValutazioni.removeAllViews()
        val textNoData = TextView(requireContext()).apply {
            text = "Nessuna valutazione trovata. Aggiungi la prima!"
            textSize = 16f
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        binding.recyclerValutazioni.addView(textNoData)
    }

    // Crea una singola card di valutazione inflatando il layout XML
    private fun creaValutazioneCard(valutazione: ValutazioneMensile): View {
        val cardView = layoutInflater.inflate(R.layout.card_valutazione, binding.recyclerValutazioni, false)

        // Trova le view nel layout della card
        val meseAnnoTextView = cardView.findViewById<TextView>(R.id.textViewMeseAnno)
        val stressTextView = cardView.findViewById<TextView>(R.id.textViewStress)
        val colleghiTextView = cardView.findViewById<TextView>(R.id.textViewColleghi)
        val soddisfazioneTextView = cardView.findViewById<TextView>(R.id.textViewSoddisfazione)
        val commentoTextView = cardView.findViewById<TextView>(R.id.textViewCommento)

        // Popola le view con i dati della valutazione
        meseAnnoTextView.text = valutazione.meseAnno
        stressTextView.text = "Stress: ${valutazione.stress}/5"
        colleghiTextView.text = "Rapporto Colleghi: ${valutazione.rapportoColleghi}/5"
        soddisfazioneTextView.text = "Soddisfazione: ${valutazione.soddisfazioneLavoro}/5"

        // Mostra il commento solo se non è vuoto
        if (valutazione.commento.isNotEmpty()) {
            commentoTextView.text = valutazione.commento
            commentoTextView.visibility = View.VISIBLE
        } else {
            commentoTextView.visibility = View.GONE
        }

        return cardView
    }
}
