package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.coreteamproject.databinding.FragmentShiftBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ShiftFragment : Fragment() {

    // Variabili per il data binding, ViewModel e formato data
    private lateinit var binding: FragmentShiftBinding
    private lateinit var viewModel: ShiftViewModel
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate = ""  // Data selezionata dall'utente

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding con il layout fragment_shift.xml
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shift, container, false)

        // Inizializza il ViewModel associato a questo fragment
        viewModel = ViewModelProvider(this)[ShiftViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Imposta la data odierna come data selezionata di default
        val today = dbDateFormat.format(Date())
        selectedDate = today

        // Configura il calendario per la selezione delle date
        setupCalendar()

        // Configura il pulsante per aggiungere un turno
        setupAddButton()

        // Configura gli observer per aggiornare l'interfaccia in base ai dati
        setupObservers()

        // Aggiorna il ViewModel con la data odierna per caricare i turni
        viewModel.updateSelectedDate(today)

        // Ritorna la root view legata al fragment
        return binding.root
    }

    // Metodo per gestire la selezione della data nel calendario
    private fun setupCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = dbDateFormat.format(calendar.time)  // Formatto la data selezionata
            viewModel.updateSelectedDate(selectedDate)  // Notifico il ViewModel
        }
    }

    // Metodo per configurare il bottone di aggiunta turno
    private fun setupAddButton() {
        binding.btnAddShift.setOnClickListener {
            viewModel.canAddShift.value?.let { canAdd ->
                if (canAdd) {
                    showAddDialog()  // Mostra dialog per inserire nuovo turno
                } else {
                    // Messaggio se non si possono aggiungere turni per date future
                    Toast.makeText(requireContext(), "Non puoi inserire turni per date future", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Imposta gli observer per aggiornare UI e notifiche da ViewModel
    private fun setupObservers() {
        // Osserva la lista di turni per aggiornare l'interfaccia
        viewModel.turni.observe(viewLifecycleOwner) { turni ->
            updateShiftsUI(turni)
        }

        // Osserva gli errori e mostra un Toast se presente
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Osserva successo salvataggio e mostra notifica
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Turno salvato!", Toast.LENGTH_SHORT).show()
                viewModel.clearSaveSuccess()
            }
        }

        // Osserva successo eliminazione e mostra notifica
        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Turno eliminato!", Toast.LENGTH_SHORT).show()
                viewModel.clearDeleteSuccess()
            }
        }

        // Osserva se la lista dei turni è vuota per mostrare messaggio
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            if (isEmpty) {
                showEmptyState()
            }
        }
    }

    // Aggiorna la UI dei turni cancellando e ricreando le viste
    private fun updateShiftsUI(turni: List<Turno>) {
        binding.shiftsLayout.removeAllViews()
        for (turno in turni) {
            val shiftView = createShiftView(turno)
            binding.shiftsLayout.addView(shiftView)
        }
    }

    // Mostra un messaggio quando non ci sono turni per la data selezionata
    private fun showEmptyState() {
        binding.shiftsLayout.removeAllViews()
        val textView = TextView(requireContext())
        textView.text = "Nessun turno per questa data"
        textView.textSize = 16f
        textView.setPadding(20, 20, 20, 20)
        binding.shiftsLayout.addView(textView)
    }

    // Mostra un dialog per aggiungere un nuovo turno con selezione orari di inizio e fine
    private fun showAddDialog() {
        // Genera la lista degli orari ogni 15 minuti tramite ViewModel
        val timeList = viewModel.generateTimeList()
        var selectedStartTime = "08:00" // Valore predefinito per inizio turno
        var selectedEndTime = "17:00"   // Valore predefinito per fine turno

        // Layout verticale per contenere gli spinner e label
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

        // Riga per orario inizio: label + spinner
        val startRow = LinearLayout(requireContext())
        startRow.orientation = LinearLayout.HORIZONTAL

        val tvStartTime = TextView(requireContext())
        tvStartTime.text = "Orario inizio:"
        tvStartTime.textSize = 16f
        tvStartTime.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1f
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        startRow.addView(tvStartTime)

        val startSpinner = Spinner(requireContext())
        val startAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, timeList)
        startSpinner.adapter = startAdapter
        startSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStartTime = timeList[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        startSpinner.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        startRow.addView(startSpinner)
        layout.addView(startRow)

        // Spaziatore per separare gli elementi
        val spacer1 = View(requireContext())
        spacer1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20
        )
        layout.addView(spacer1)

        // Riga per orario fine
        val endRow = LinearLayout(requireContext())
        endRow.orientation = LinearLayout.HORIZONTAL

        val tvEndTime = TextView(requireContext())
        tvEndTime.text = "Orario fine:"
        tvEndTime.textSize = 16f
        tvEndTime.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1f
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        endRow.addView(tvEndTime)

        val endSpinner = Spinner(requireContext())
        val endAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, timeList)
        endSpinner.adapter = endAdapter
        endSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEndTime = timeList[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        endSpinner.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Imposta di default l'orario fine a "17:00"
        val defaultEndPosition = timeList.indexOf("17:00")
        if (defaultEndPosition >= 0) {
            endSpinner.setSelection(defaultEndPosition)
        }

        endRow.addView(endSpinner)
        layout.addView(endRow)

        // Costruzione e visualizzazione del dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Turno")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val timeRange = "$selectedStartTime - $selectedEndTime"
                val title = "Turno $selectedDate"
                // Salva il nuovo turno tramite ViewModel
                viewModel.saveShift(title, timeRange, "")
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Crea dinamicamente una View che rappresenta un turno con titolo, orario e descrizione
    private fun createShiftView(turno: Turno): View {
        val shiftLayout = LinearLayout(requireContext())
        shiftLayout.orientation = LinearLayout.VERTICAL
        shiftLayout.setPadding(32, 32, 32, 32)
        shiftLayout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 16, 0, 16)
        shiftLayout.layoutParams = layoutParams

        // Estrae le informazioni dal turno
        val title = turno.title
        val time = turno.time
        val description = turno.description

        // Titolo in grassetto e grande
        val titleText = TextView(requireContext())
        titleText.text = title
        titleText.textSize = 18f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        shiftLayout.addView(titleText)

        // Mostra orario se presente
        if (time.isNotEmpty()) {
            val timeText = TextView(requireContext())
            timeText.text = "Orario: $time"
            timeText.textSize = 14f
            shiftLayout.addView(timeText)
        }

        // Mostra descrizione se presente
        if (description.isNotEmpty()) {
            val descText = TextView(requireContext())
            descText.text = description
            descText.textSize = 14f
            shiftLayout.addView(descText)
        }

        // Aggiunge click listener per cancellare il turno con conferma dialog
        shiftLayout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Elimina Turno")
                .setMessage("Vuoi eliminare '$title'?")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.deleteShift(turno.id)  // Chiamata per eliminare tramite ViewModel
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        return shiftLayout
    }
}
