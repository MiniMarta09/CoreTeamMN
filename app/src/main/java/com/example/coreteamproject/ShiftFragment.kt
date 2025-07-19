package com.example.coreteamproject

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ShiftFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var btnAddShift: Button
    private lateinit var shiftsLayout: LinearLayout
    private lateinit var textSelectedDate: TextView
    private lateinit var textMonthlyHours: TextView

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate = ""
    private var currentMonth = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shift, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        btnAddShift = view.findViewById(R.id.btnAddShift)
        shiftsLayout = view.findViewById(R.id.shiftsLayout)
        textSelectedDate = view.findViewById(R.id.textSelectedDate)
        textMonthlyHours = view.findViewById(R.id.textMonthlyHours)

        // Data di oggi
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        selectedDate = today
        textSelectedDate.text = "Turni per: $today"
        
        // Inizializziamo il mese corrente (formato yyyy-MM)
        currentMonth = today.substring(0, 7)

        // Calendario
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(calendar.time)

            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            textSelectedDate.text = "Turni per: ${displayFormat.format(calendar.time)}"

            // Verifichiamo se è cambiato il mese per aggiornare il calcolo delle ore mensili
            val newMonth = selectedDate.substring(0, 7)
            if (newMonth != currentMonth) {
                currentMonth = newMonth
            }
            
            loadShifts(selectedDate)
        }

        // Bottone aggiungi
        btnAddShift.setOnClickListener {
            // Verifica se la data selezionata è oggi o nel passato
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val selectedDateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            val todayObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
            
            if (selectedDateObj != null && todayObj != null && selectedDateObj.after(todayObj)) {
                // La data selezionata è nel futuro
                Toast.makeText(requireContext(), "Non puoi inserire turni per date future", Toast.LENGTH_LONG).show()
            } else {
                // La data è oggi o nel passato, permetti l'inserimento
                showAddDialog()
            }
        }

        // Carica turni di oggi
        loadShifts(today)

        return view
    }

    private fun showAddDialog() {
        // Generiamo la lista di orari ogni 15 minuti
        val timeList = generateTimeList()
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
                
                saveShift(title, timeRange, "")
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    // Genera una lista di orari ogni 15 minuti (00:00, 00:15, 00:30, ecc.)
    private fun generateTimeList(): List<String> {
        val timeList = mutableListOf<String>()
        
        for (hour in 0..23) {
            for (minute in listOf(0, 15, 30, 45)) {
                val formattedHour = hour.toString().padStart(2, '0')
                val formattedMinute = minute.toString().padStart(2, '0')
                timeList.add("$formattedHour:$formattedMinute")
            }
        }
        
        return timeList
    }
    
    // Formatta l'orario in formato leggibile (HH:mm)
    private fun formatTime(calendar: Calendar): String {
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$hour:$minute"
    }

    private fun saveShift(title: String, time: String, description: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Errore: utente non autenticato", Toast.LENGTH_SHORT).show()
            return
        }
        
        val shift = hashMapOf(
            "title" to title,
            "time" to time,
            "description" to description,
            "date" to selectedDate,
            "userId" to currentUser.uid
        )

        db.collection("shifts")
            .add(shift)
            .addOnSuccessListener {
                Toast.makeText(context, "Turno salvato!", Toast.LENGTH_SHORT).show()
                loadShifts(selectedDate)
                loadMonthlyHours()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Errore salvataggio turno", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadShifts(date: String) {
        shiftsLayout.removeAllViews()
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val textView = TextView(requireContext())
            textView.text = "Errore: utente non autenticato"
            textView.textSize = 16f
            textView.setPadding(20, 20, 20, 20)
            shiftsLayout.addView(textView)
            return
        }

        db.collection("shifts")
            .whereEqualTo("date", date)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val textView = TextView(requireContext())
                    textView.text = "Nessun turno per questa data"
                    textView.textSize = 16f
                    textView.setPadding(20, 20, 20, 20)
                    shiftsLayout.addView(textView)
                } else {
                    for (document in documents) {
                        val shift = document.data
                        val shiftView = createShiftView(document.id, shift)
                        shiftsLayout.addView(shiftView)
                    }
                }
                
                // Aggiorniamo il calcolo del monte ore mensile
                loadMonthlyHours()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel caricamento dei turni", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadMonthlyHours() {
        try {
            // Otteniamo il mese dalla data selezionata (yyyy-MM)
            val selectedMonth = selectedDate.substring(0, 7)
            currentMonth = selectedMonth
            
            // Costruiamo le date di inizio e fine mese correttamente
            val startOfMonth = "$selectedMonth-01"
            val endOfMonth = "$selectedMonth-31"
            
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                textMonthlyHours.text = "Ore $selectedMonth: Errore utente"
                return
            }
            
            // Approccio semplificato: carichiamo tutti i turni del mese e filtriamo
            db.collection("shifts")
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .get()
                .addOnSuccessListener { documents ->
                    var totalMinutes = 0
                    
                    for (document in documents) {
                        try {
                            val shift = document.data
                            val shiftUserId = shift["userId"] as? String
                            
                            // Include il turno SOLO se ha userId e corrisponde all'utente corrente
                            if (shiftUserId == currentUser.uid) {
                                val timeRange = shift["time"] as? String
                                if (timeRange != null && timeRange.isNotEmpty()) {
                                    val shiftMinutes = calculateMinutesFromTimeRange(timeRange)
                                    if (shiftMinutes > 0) {
                                        totalMinutes += shiftMinutes
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignora turni con dati malformati
                            continue
                        }
                    }
                    
                    // Aggiorniamo il display
                    updateMonthlyHoursDisplay(totalMinutes, selectedMonth)
                }
                .addOnFailureListener { exception ->
                    textMonthlyHours.text = "Ore $selectedMonth: Errore caricamento"
                }
        } catch (e: Exception) {
            textMonthlyHours.text = "Ore: Errore calcolo"
        }
    }
    
    private fun updateMonthlyHoursDisplay(totalMinutes: Int, selectedMonth: String) {
        // Convertiamo i minuti totali in ore e minuti
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        // Aggiorniamo la TextView
        val monthName = getMonthName(selectedMonth.substring(5, 7).toInt() - 1)
        textMonthlyHours.text = "Ore $monthName: ${hours}h ${minutes}m"
    }
    
    private fun calculateMinutesFromTimeRange(timeRange: String): Int {
        try {
            // Formato atteso: "HH:MM - HH:MM"
            val parts = timeRange.split(" - ")
            if (parts.size != 2) return 0
            
            val startTime = parts[0] // HH:MM
            val endTime = parts[1]   // HH:MM
            
            val startHour = startTime.split(":")[0].toInt()
            val startMinute = startTime.split(":")[1].toInt()
            val endHour = endTime.split(":")[0].toInt()
            val endMinute = endTime.split(":")[1].toInt()
            
            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute
            
            // Gestiamo anche il caso in cui l'orario di fine sia il giorno dopo
            return if (endTotalMinutes > startTotalMinutes) {
                endTotalMinutes - startTotalMinutes
            } else {
                (24 * 60 - startTotalMinutes) + endTotalMinutes // Assume turno notturno
            }
        } catch (e: Exception) {
            return 0
        }
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            0 -> "Gennaio"
            1 -> "Febbraio"
            2 -> "Marzo"
            3 -> "Aprile"
            4 -> "Maggio"
            5 -> "Giugno"
            6 -> "Luglio"
            7 -> "Agosto"
            8 -> "Settembre"
            9 -> "Ottobre"
            10 -> "Novembre"
            11 -> "Dicembre"
            else -> ""
        }
    }

    private fun createShiftView(shiftId: String, shift: MutableMap<String, Any>): View {
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

        // Estrai i dati dalla mappa
        val title = shift["title"] as? String ?: ""
        val time = shift["time"] as? String ?: ""
        val description = shift["description"] as? String ?: ""
        
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
                    deleteShift(shiftId)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        return shiftLayout
    }

    private fun deleteShift(shiftId: String) {
        db.collection("shifts")
            .document(shiftId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Turno eliminato!", Toast.LENGTH_SHORT).show()
                loadShifts(selectedDate)
                // Aggiorniamo il calcolo delle ore mensili
                loadMonthlyHours()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Errore eliminazione", Toast.LENGTH_SHORT).show()
            }
    }
}