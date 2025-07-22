package com.example.coreteamproject

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

        for ((index, valutazione) in valutazioni.withIndex()) {
            aggiungiValutazioneAllaLista(valutazione)

            // Aggiungi spazio tra le card (tranne dopo l'ultima)
            if (index < valutazioni.size - 1) {
                val space = Space(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        24 // Spazio visivo tra una valutazione e l'altra
                    )
                }
                binding.recyclerValutazioni.addView(space)
            }
        }
    }

    //Mostra messaggio se non ci sono valutazioni
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
        // Creiamo il FrameLayout esterno che farà da bordo nero
        val frameLayout = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(3, 3, 3, 3)
            }

            background = GradientDrawable().apply {
                setColor(resources.getColor(android.R.color.black, null))
                cornerRadius = 18f
            }

            setPadding(2, 2, 2, 2)
        }

        // Creiamo la CardView interna che ospiterà i contenuti
        val cardView = CardView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(3, 3, 3, 3)
            }
            radius = 12f
            cardElevation = 6f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }

        // Crea il container principale all'interno della CardView
        val containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        // Titolo con mese/anno
        val textMeseAnno = TextView(requireContext()).apply {
            text = "Mese: ${valutazione.meseAnno}"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.purple_500))
            setPadding(0, 0, 0, 16)
        }

        // Stress
        val textStress = TextView(requireContext()).apply {
            text = "Stress: ${valutazione.stress}/5"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(0, 0, 0, 8)
        }

        // Colleghi
        val textColleghi = TextView(requireContext()).apply {
            text = "Colleghi: ${valutazione.rapportoColleghi}/5"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(0, 0, 0, 8)
        }

        // Soddisfazione
        val textSoddisfazione = TextView(requireContext()).apply {
            text = "Soddisfazione: ${valutazione.soddisfazioneLavoro}/5"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(0, 0, 0, 8)
        }

        // Commento
        if (valutazione.commento.isNotEmpty()) {
            val textCommento = TextView(requireContext()).apply {
                text = "Commento: ${valutazione.commento}"
                textSize = 14f
                setTypeface(null, Typeface.ITALIC)
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }
            containerLayout.addView(textCommento)
        }

        // Aggiungi tutti i TextView al container
        containerLayout.addView(textMeseAnno)
        containerLayout.addView(textStress)
        containerLayout.addView(textColleghi)
        containerLayout.addView(textSoddisfazione)

        // Aggiungi il container alla CardView
        cardView.addView(containerLayout)

        // Aggiungi la CardView al FrameLayout
        frameLayout.addView(cardView)

        // Aggiungi il FrameLayout alla vista
        binding.recyclerValutazioni.addView(frameLayout)
    }
}
