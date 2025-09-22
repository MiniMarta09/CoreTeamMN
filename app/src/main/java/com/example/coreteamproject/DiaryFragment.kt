package com.example.coreteamproject

import android.util.Log
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
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Fragment per gestire il diario
class DiaryFragment : Fragment() {

    // Variabili per databinding e viewmodel
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var viewModel: DiaryViewModel
    private var isAdmin: Boolean = false

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carica il profilo utente per determinare il ruolo e avviare il caricamento dei dati
        loadUserProfile()

        // Configura gli observer
        setupObservers(view) // Passa la vista agli observer

        // Imposta i listener per i pulsanti
        setupListeners()
    }

    // Configura gli observer per i LiveData del ViewModel
    private fun setupObservers(view: View) {
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

        // Observer per lo stato vuoto (solo per la vista utente)
        viewModel.isEmpty.observe(viewLifecycleOwner, Observer { isEmpty ->
            if (!isAdmin && isEmpty) {
                mostraMessaggioVuoto()
            }
        })

        // Observer per i dati del grafico (sia admin che utente)
        viewModel.chartData.observe(viewLifecycleOwner, Observer { chartData ->
            if (chartData != null) {
                setupChart(chartData)
            } else {
                // Se non ci sono dati, nascondi il grafico
                binding.barChart.visibility = View.GONE
            }
        })

        // Observer per le statistiche aggregate (sia admin che utente)
        viewModel.diaryStats.observe(viewLifecycleOwner, Observer { stats ->
            if (stats != null) {
                binding.strengthTextView.text = stats.overallStrength
                binding.weaknessTextView.text = stats.overallWeakness
                binding.participationTextView.text = stats.participation
                binding.bestMonthTextView.text = stats.bestMonth
            } else {
                // Se non ci sono statistiche, potresti voler nascondere la sezione
                binding.statsLayout.visibility = View.GONE
            }
        })

        // Observer per cambiare il testo del pulsante della dashboard
        viewModel.showDashboard.observe(viewLifecycleOwner, Observer { isVisible ->
            binding.btnVisualizzaDashboard.text = if (isVisible) "Nascondi Dashboard" else "Visualizza la tua Dashboard"
        })

    }

    // Configura i listener per i pulsanti
    private fun setupListeners() {
        // Listener per il pulsante di aggiunta valutazione
        binding.btnAggiungiValutazione.setOnClickListener {
            showValutazioneDialog()
        }

        // Listener per il pulsante che mostra/nasconde la dashboard
        binding.btnVisualizzaDashboard.setOnClickListener {
            viewModel.toggleDashboardVisibility()
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

    // Carica il profilo utente per determinare il ruolo
    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("Profili").document(userId).get()
            .addOnSuccessListener { document ->
                val userRole = document.getString("RUOLO")
                isAdmin = userRole == "ADMIN"

                // Aggiorna la UI in base al ruolo
                updateUiForRole()

                // Avvia il caricamento dei dati specifici per il ruolo
                viewModel.loadDiaryData(isAdmin)
            }
            .addOnFailureListener {
                isAdmin = false
                updateUiForRole()
                viewModel.loadDiaryData(isAdmin)
            }
    }

    // Aggiorna la UI per mostrare la vista corretta (admin o utente) in base al ruolo
    private fun updateUiForRole() {
        if (isAdmin) {
            // Colori per la vista Admin
            val adminPrimaryColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
            val adminVariantColor = ContextCompat.getColor(requireContext(), R.color.admin_primary_variant)

            // Vista Admin: mostra sempre la dashboard e nasconde la lista valutazioni
            viewModel.toggleDashboardVisibility() // Assicura che la dashboard sia visibile
            binding.recyclerValutazioni.visibility = View.GONE
            binding.btnAggiungiValutazione.visibility = View.GONE
            binding.btnVisualizzaDashboard.visibility = View.GONE // L'admin non ha bisogno di alternare la vista

            // Applica i colori Admin ai titoli principali
            binding.textViewDiary.text = "Dashboard Diario"
            binding.textViewDiary.setTextColor(adminPrimaryColor)
            binding.textDescriptionDiary.text = "Andamento mensile del team"
            binding.textDescriptionDiary.setTextColor(adminVariantColor)

            // Applica i colori ai titoli delle statistiche
            binding.strengthTitleTextView.setTextColor(adminPrimaryColor)
            binding.weaknessTitleTextView.setTextColor(adminPrimaryColor)
            binding.participationTitleTextView.setTextColor(adminPrimaryColor)
            binding.bestMonthTitleTextView.setTextColor(adminPrimaryColor)

            // Applica i colori ai valori delle statistiche
            binding.strengthTextView.setTextColor(adminVariantColor)
            binding.weaknessTextView.setTextColor(adminVariantColor)
            binding.participationTextView.setTextColor(adminVariantColor)
            binding.bestMonthTextView.setTextColor(adminVariantColor)

        } else {
            // Vista Utente: mostra la lista valutazioni e il pulsante per la dashboard
            binding.recyclerValutazioni.visibility = View.VISIBLE
            binding.btnAggiungiValutazione.visibility = View.VISIBLE
            binding.btnVisualizzaDashboard.visibility = View.VISIBLE
        }
    }

    // Configura l'aspetto e i dati del grafico a barre per la dashboard dell'admin
    private fun setupChart(chartData: ChartData) {
        val entriesStress = mutableListOf<BarEntry>()
        val entriesColleghi = mutableListOf<BarEntry>()
        val entriesSoddisfazione = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        chartData.monthlyAverages.forEachIndexed { index, data ->
            entriesStress.add(BarEntry(index.toFloat(), data.avgStress))
            entriesColleghi.add(BarEntry(index.toFloat(), data.avgRapportoColleghi))
            entriesSoddisfazione.add(BarEntry(index.toFloat(), data.avgSoddisfazioneLavoro))
            labels.add(data.monthYear)
        }

        val stressDataSet = BarDataSet(entriesStress, "Stress").apply {
            color = ContextCompat.getColor(requireContext(), R.color.red)
        }
        val colleghiDataSet = BarDataSet(entriesColleghi, "Rapporto Colleghi").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_500)
        }
        val soddisfazioneDataSet = BarDataSet(entriesSoddisfazione, "Soddisfazione Lavoro").apply {
            color = ContextCompat.getColor(requireContext(), R.color.green)
        }

        val barData = BarData(stressDataSet, colleghiDataSet, soddisfazioneDataSet)
        binding.barChart.data = barData

        // Raggruppamento delle barre
        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.15f
        barData.barWidth = barWidth
        binding.barChart.groupBars(0f, groupSpace, barSpace)

        // Configurazione asse X
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        binding.barChart.xAxis.granularity = 1f
        binding.barChart.xAxis.setCenterAxisLabels(true)
        binding.barChart.xAxis.axisMinimum = 0f
        binding.barChart.xAxis.axisMaximum = labels.size.toFloat()

        // Rimuove i valori numerici sopra le barre
        barData.setDrawValues(false)

        // --- Stile e personalizzazione del grafico ---

        // Disabilita la leggenda automatica, useremo quella personalizzata
        binding.barChart.legend.isEnabled = false

        // Assi e griglia
        binding.barChart.axisRight.isEnabled = false // Nasconde l'asse Y di destra
        binding.barChart.axisLeft.apply {
            axisMinimum = 0f // Il valore minimo parte da 0
            axisMaximum = 5.5f // Il massimo è 5, lasciamo un po' di spazio sopra
            setDrawGridLines(false) // Nasconde le linee della griglia orizzontale
            textSize = 12f
        }
        binding.barChart.xAxis.apply {
            setDrawGridLines(false) // Nasconde le linee della griglia verticale
            textSize = 12f
        }

        // Altre opzioni estetiche
        binding.barChart.description.isEnabled = false // Nasconde la descrizione di default
        binding.barChart.setDrawGridBackground(false) // Rimuove lo sfondo della griglia
        binding.barChart.animateY(1200) // Animazione di ingresso più fluida

        // Aggiunge spazio extra sotto il grafico per la leggenda
        binding.barChart.extraBottomOffset = 20f

        // Aggiorna il grafico per applicare tutte le modifiche
        binding.barChart.invalidate()
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
