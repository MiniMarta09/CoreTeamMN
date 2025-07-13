package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EventFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var btnAddEvent: Button
    private lateinit var eventsLayout: LinearLayout
    private lateinit var textSelectedDate: TextView

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        eventsLayout = view.findViewById(R.id.eventsLayout)
        textSelectedDate = view.findViewById(R.id.textSelectedDate)

        // Data di oggi
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        selectedDate = today
        textSelectedDate.text = "Eventi per: $today"

        // Calendario
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(calendar.time)

            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            textSelectedDate.text = "Eventi per: ${displayFormat.format(calendar.time)}"

            loadEvents(selectedDate)
        }

        // Bottone aggiungi
        btnAddEvent.setOnClickListener {
            showAddDialog()
        }

        // Carica eventi di oggi
        loadEvents(today)

        return view
    }

    private fun showAddDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

        val editTitle = EditText(requireContext())
        editTitle.hint = "Titolo evento"
        layout.addView(editTitle)

        val editTime = EditText(requireContext())
        editTime.hint = "Ora (es. 14:30)"
        layout.addView(editTime)

        val editDescription = EditText(requireContext())
        editDescription.hint = "Descrizione"
        layout.addView(editDescription)

        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Evento")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val title = editTitle.text.toString().trim()
                val time = editTime.text.toString().trim()
                val description = editDescription.text.toString().trim()
                if (title.isNotEmpty()) {
                    saveEvent(title, time, description)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun saveEvent(title: String, time: String, description: String) {
        val event = hashMapOf(
            "title" to title,
            "time" to time,
            "description" to description,
            "date" to selectedDate,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("events")
            .add(event)
            .addOnSuccessListener {
                Toast.makeText(context, "Evento aggiunto!", Toast.LENGTH_SHORT).show()
                loadEvents(selectedDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadEvents(date: String) {
        db.collection("events")
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                eventsLayout.removeAllViews()

                if (documents.isEmpty) {
                    val noEventsText = TextView(requireContext())
                    noEventsText.text = "Nessun evento"
                    noEventsText.textSize = 16f
                    noEventsText.setPadding(16, 16, 16, 16)
                    eventsLayout.addView(noEventsText)
                } else {
                    for (document in documents) {
                        val eventTitle = document.getString("title") ?: ""
                        val eventTime = document.getString("time") ?: ""
                        val eventDescription = document.getString("description") ?: ""
                        val eventId = document.id

                        val eventView = createEventView(eventTitle, eventTime, eventDescription, eventId)
                        eventsLayout.addView(eventView)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Errore caricamento eventi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createEventView(title: String, time: String, description: String, eventId: String): View {
        val eventLayout = LinearLayout(requireContext())
        eventLayout.orientation = LinearLayout.VERTICAL
        eventLayout.setPadding(32, 32, 32, 32)
        eventLayout.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 16, 0, 16)
        eventLayout.layoutParams = layoutParams

        // Titolo
        val titleText = TextView(requireContext())
        titleText.text = title
        titleText.textSize = 18f
        titleText.setTypeface(null, android.graphics.Typeface.BOLD)
        eventLayout.addView(titleText)

        // Ora
        if (time.isNotEmpty()) {
            val timeText = TextView(requireContext())
            timeText.text = "Ora: $time"
            timeText.textSize = 14f
            eventLayout.addView(timeText)
        }

        // Descrizione
        if (description.isNotEmpty()) {
            val descText = TextView(requireContext())
            descText.text = description
            descText.textSize = 14f
            eventLayout.addView(descText)
        }

        eventLayout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Elimina Evento")
                .setMessage("Vuoi eliminare '$title'?")
                .setPositiveButton("Elimina") { _, _ ->
                    deleteEvent(eventId)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        return eventLayout
    }

    private fun deleteEvent(eventId: String) {
        db.collection("events")
            .document(eventId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Evento eliminato!", Toast.LENGTH_SHORT).show()
                loadEvents(selectedDate)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Errore eliminazione", Toast.LENGTH_SHORT).show()
            }
    }
}