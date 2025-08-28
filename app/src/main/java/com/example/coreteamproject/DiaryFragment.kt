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

    // Configura tutti i listener per i controlli dell'interfaccia
    private fun setupListeners() {
        // Pulsante + per aggiungere valutazione
        binding.btnAggiungiValutazione.setOnClickListener {
            viewModel.mostraFormValutazione()
        }

        // Pulsante Salva
        binding.btnSalva.setOnClickListener {
            val stress = binding.seekbarStress.progress + 1
            val colleghi = binding.seekbarColleghi.progress + 1
            val soddisfazione = binding.seekbarSoddisfazione.progress + 1
            val commento = binding.editCommento.text.toString()

            viewModel.salvaValutazione(stress, colleghi, soddisfazione, commento)
        }

        // Pulsante Annulla
        binding.btnAnnulla.setOnClickListener {
            viewModel.nascondiFormValutazione()
        }

        // Listener per i SeekBarStress
        binding.seekbarStress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textStressValue.text = "${progress + 1}"
                viewModel.updateStressValue(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Listener per i SeekBarColleghi
        binding.seekbarColleghi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textColleghiValue.text = "${progress + 1}"
                viewModel.updateColleghiValue(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Listener per i SeekBarSoddisfazione
        binding.seekbarSoddisfazione.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textSoddisfazioneValue.text = "${progress + 1}"
                viewModel.updateSoddisfazioneValue(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
