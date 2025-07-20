package com.example.coreteamproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.coreteamproject.databinding.FragmentDiaryBinding

//Fragment per gestire il diario
class DiaryFragment : Fragment() {

    //Variabili per databinding e viewmodel
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var viewModel: DiaryViewModel

   //Metodo chiamato alla creazione del fragment
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

        // Imposta il listener per i pulsanti
        setupListeners()

        return binding.root
    }

    //Configura gli observer per valutare i cambiamenti del viewmodel
    private fun setupObservers() {
        // Observer per le valutazioni
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

    //Configura tutti i listener per i controlli dell'interfaccia
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

        //Listener per i SeekBarColleghi
        binding.seekbarColleghi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textColleghiValue.text = "${progress + 1}"
                viewModel.updateColleghiValue(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

       //Listener per i SeekBarSoddisfazione
        binding.seekbarSoddisfazione.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textSoddisfazioneValue.text = "${progress + 1}"
                viewModel.updateSoddisfazioneValue(progress + 1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    //Aggiornamento valutazioni
    private fun aggiornaListaValutazioni(valutazioni: List<ValutazioneMensile>) {
        binding.recyclerValutazioni.removeAllViews()

        for (valutazione in valutazioni) {
            aggiungiValutazioneAllaLista(valutazione)
        }
    }

    private fun mostraMessaggioVuoto() {
        binding.recyclerValutazioni.removeAllViews()
        val textNoData = TextView(requireContext())
        textNoData.text = "Nessuna valutazione trovata. Aggiungi la prima!"
        textNoData.textSize = 16f
        textNoData.setPadding(16, 16, 16, 16)
        binding.recyclerValutazioni.addView(textNoData)
    }

   //Aggiunta valutazione alla lista
    private fun aggiungiValutazioneAllaLista(valutazione: ValutazioneMensile) {
        // Crea il container principale
        val containerLayout = LinearLayout(requireContext())
        containerLayout.orientation = LinearLayout.VERTICAL
        containerLayout.setPadding(16, 16, 16, 16)
        containerLayout.setBackgroundColor(0xFFF5F5F5.toInt()) // grigio chiaro

     //Margini e dimensioni
       val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 16) // margine sotto
        containerLayout.layoutParams = layoutParams

        // Titolo con mese/anno
        val textMeseAnno = TextView(requireContext())
        textMeseAnno.text = "Mese: ${valutazione.meseAnno}"
        textMeseAnno.textSize = 18f
        textMeseAnno.setTypeface(null, android.graphics.Typeface.BOLD)
        textMeseAnno.setTextColor(resources.getColor(R.color.purple_500, null))
        textMeseAnno.setPadding(0, 0, 0, 16)
        containerLayout.addView(textMeseAnno)

        // Stress
        val textStress = TextView(requireContext())
        textStress.text = "Stress: ${valutazione.stress}/5"
        textStress.textSize = 14f
        textStress.setPadding(0, 0, 0, 8)
        containerLayout.addView(textStress)

        // Colleghi
        val textColleghi = TextView(requireContext())
        textColleghi.text = "Colleghi: ${valutazione.rapportoColleghi}/5"
        textColleghi.textSize = 14f
        textColleghi.setPadding(0, 0, 0, 8)
        containerLayout.addView(textColleghi)

        // Soddisfazione
        val textSoddisfazione = TextView(requireContext())
        textSoddisfazione.text = "Soddisfazione: ${valutazione.soddisfazioneLavoro}/5"
        textSoddisfazione.textSize = 14f
        textSoddisfazione.setPadding(0, 0, 0, 8)
        containerLayout.addView(textSoddisfazione)

        // Commento
        if (valutazione.commento.isNotEmpty()) {
            val textCommento = TextView(requireContext())
            textCommento.text = "Commento: ${valutazione.commento}"
            textCommento.textSize = 14f
            textCommento.setTypeface(null, android.graphics.Typeface.ITALIC)
            containerLayout.addView(textCommento)
        }

        binding.recyclerValutazioni.addView(containerLayout)
    }
}