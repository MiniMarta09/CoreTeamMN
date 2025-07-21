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

    private lateinit var binding: FragmentShiftBinding
    private lateinit var viewModel: ShiftViewModel
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza data binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shift, container, false)
        
        // Inizializza ViewModel
        viewModel = ViewModelProvider(this)[ShiftViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        
        // Data di oggi
        val today = dbDateFormat.format(Date())
        selectedDate = today
        
        // Setup calendario
        setupCalendar()
        
        // Setup bottone aggiungi
        setupAddButton()
        
        // Setup observers
        setupObservers()
        
        // Inizializza con data corrente
        viewModel.updateSelectedDate(today)
        
        return binding.root
    }
    
    private fun setupCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = dbDateFormat.format(calendar.time)
            viewModel.updateSelectedDate(selectedDate)
        }
    }
    
    private fun setupAddButton() {
        binding.btnAddShift.setOnClickListener {
            viewModel.canAddShift.value?.let { canAdd ->
                if (canAdd) {
                    showAddDialog()
                } else {
                    Toast.makeText(requireContext(), "Non puoi inserire turni per date future", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setupObservers() {
        // Observer per i turni
        viewModel.turni.observe(viewLifecycleOwner) { turni ->
            updateShiftsUI(turni)
        }
        
        // Observer per errori
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        // Observer per successo salvataggio
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Turno salvato!", Toast.LENGTH_SHORT).show()
                viewModel.clearSaveSuccess()
            }
        }
        
        // Observer per successo eliminazione
        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Turno eliminato!", Toast.LENGTH_SHORT).show()
                viewModel.clearDeleteSuccess()
            }
        }
        
        // Observer per stato vuoto
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            if (isEmpty) {
                showEmptyState()
            }
        }
    }
    
    private fun updateShiftsUI(turni: List<Turno>) {
        binding.shiftsLayout.removeAllViews()
        
        for (turno in turni) {
            val shiftView = createShiftView(turno)
            binding.shiftsLayout.addView(shiftView)
        }
    }
    
    private fun showEmptyState() {
        binding.shiftsLayout.removeAllViews()
        val textView = TextView(requireContext())
        textView.text = "Nessun turno per questa data"
        textView.textSize = 16f
        textView.setPadding(20, 20, 20, 20)
        binding.shiftsLayout.addView(textView)
    }

    private fun showAddDialog() {
        // Generiamo la lista di orari ogni 15 minuti
        val timeList = viewModel.generateTimeList()
        var selectedStartTime = "08:00" // Valore predefinito per inizio
        var selectedEndTime = "17:00" // Valore predefinito per fine
        
        // Layout per il dialog
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        
        // Riga per l'orario di inizio
        val startRow = LinearLayout(requireContext())
        startRow.orientation = LinearLayout.HORIZONTAL
        
        // TextView per l'etichetta dell'orario di inizio
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
        
        // Spinner per l'orario di inizio
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
        
        // Spaziatore
        val spacer1 = View(requireContext())
        spacer1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20
        )
        layout.addView(spacer1)
        
        // Riga per l'orario di fine
        val endRow = LinearLayout(requireContext())
        endRow.orientation = LinearLayout.HORIZONTAL
        
        // TextView per l'etichetta dell'orario di fine
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
        
        // Spinner per l'orario di fine
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
        
        // Impostiamo il valore predefinito dello spinner di fine a 17:00
        val defaultEndPosition = timeList.indexOf("17:00")
        if (defaultEndPosition >= 0) {
            endSpinner.setSelection(defaultEndPosition)
        }
        
        endRow.addView(endSpinner)
        layout.addView(endRow)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Turno")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val timeRange = "$selectedStartTime - $selectedEndTime"
                val title = "Turno $selectedDate"
                
                viewModel.saveShift(title, timeRange, "")
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    


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

        // Estrai i dati dal turno
        val title = turno.title
        val time = turno.time
        val description = turno.description
        
        // Titolo
        val titleText = TextView(requireContext())
        titleText.text = title
        titleText.textSize = 18f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        shiftLayout.addView(titleText)

        // Ora
        if (time.isNotEmpty()) {
            val timeText = TextView(requireContext())
            timeText.text = "Orario: $time"
            timeText.textSize = 14f
            shiftLayout.addView(timeText)
        }

        // Descrizione
        if (description.isNotEmpty()) {
            val descText = TextView(requireContext())
            descText.text = description
            descText.textSize = 14f
            shiftLayout.addView(descText)
        }

        shiftLayout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Elimina Turno")
                .setMessage("Vuoi eliminare '$title'?")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.deleteShift(turno.id)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        return shiftLayout
    }
}