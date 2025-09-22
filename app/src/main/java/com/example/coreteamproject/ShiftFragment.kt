package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.coreteamproject.databinding.FragmentShiftBinding
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

// Fragment per la visualizzazione e modifica dei turni
class ShiftFragment : Fragment() {

    // Variabili per il data binding, ViewModel e formato data
    private lateinit var binding: FragmentShiftBinding
    private lateinit var viewModel: ShiftViewModel
    private lateinit var adminViewModel: AdminSchedulingViewModel
    private lateinit var usersViewModel: UsersViewModel
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate = ""  // Data selezionata dall'utente
    private var isAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding con il layout fragment_shift.xml
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shift, container, false)

        // Inizializza i ViewModel
        viewModel = ViewModelProvider(this)[ShiftViewModel::class.java]
        adminViewModel = ViewModelProvider(this)[AdminSchedulingViewModel::class.java]
        usersViewModel = ViewModelProvider(this)[UsersViewModel::class.java]
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
        
        // Carica profilo per controllare se è admin
        usersViewModel.caricaProfiloUtente()

        // Aggiorna il ViewModel con la data di oggi  per caricare i turni
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
            viewModel.updateSelectedDate(selectedDate)  // Aggiorno il ViewModel
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
                    Toast.makeText(
                        requireContext(),
                        "Non puoi inserire turni per date future",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Imposta gli observer per aggiornare UI e notifiche da ViewModel
    private fun setupObservers() {
        // Osserva i turni e aggiorna la UI
        viewModel.turni.observe(viewLifecycleOwner) { turni ->
            Log.d("ShiftFragment", "Observer ricevuto ${turni.size} turni")
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
                // Forza il ricaricamento dei turni per la data corrente
                viewModel.updateSelectedDate(selectedDate)
                viewModel.clearSaveSuccess()
            }
        }

        // Osserva successo eliminazione e mostra notifica
        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Turno eliminato!", Toast.LENGTH_SHORT).show()
                // Ricarica i turni per la data corrente per aggiornare la lista
                viewModel.updateSelectedDate(selectedDate)
                viewModel.clearDeleteSuccess()
            }
        }

        // Osserva se la lista dei turni è vuota per mostrare messaggio
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            if (isEmpty) {
                showEmptyState()
            }
        }
        
        // Observer per ruolo utente
        usersViewModel.userRole.observe(viewLifecycleOwner) { role ->
            Log.d("ShiftFragment", "Ruolo utente ricevuto: $role")
            isAdmin = (role == UserRole.ADMIN)
            Log.d("ShiftFragment", "isAdmin impostato a: $isAdmin")
            
            // Se l'utente è admin, aggiorna la UI con i colori dedicati
            if (isAdmin) {
                updateUiForAdmin()
            }
            // Forza aggiornamento UI per mostrare/nascondere bottone admin
            val currentTurni = viewModel.turni.value ?: emptyList()
            updateShiftsUI(currentTurni)
        }
        
        // Observer per messaggi admin
        adminViewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                adminViewModel.clearMessage()
            }
        }
        
        // Observer per turni generati - MOSTRA LA SCHEDA
        adminViewModel.turniGenerati.observe(viewLifecycleOwner) { turni ->
            if (turni.isNotEmpty()) {
                mostraSchedaTurniGenerati(turni)
            }
        }
        
        // Observer per settori
        adminViewModel.settori.observe(viewLifecycleOwner) { settori ->
            if (settori.isNotEmpty()) {
                mostraOrariSettori(settori)
            }
        }
    }

    // Aggiorna la UI dei turni cancellando e ricreando le viste
    private fun updateShiftsUI(turni: List<Turno>) {
        binding.shiftsLayout.removeAllViews()
        binding.buttonsLayout.removeAllViews()
        binding.bottomButtonsLayout.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (turno in turni) {
            // inflata il nuovo layout della card usando il data binding
            val cardBinding: com.example.coreteamproject.databinding.CardTurnoBinding =
                com.example.coreteamproject.databinding.CardTurnoBinding.inflate(
                    inflater,
                    binding.shiftsLayout,
                    false
                )

            // imposta la variabile 'turno' nel layout
            cardBinding.turno = turno

            // imposta il listener per l'eliminazione
            cardBinding.root.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Elimina Turno")
                    .setMessage("Sei sicuro di voler eliminare questo turno?")
                    .setPositiveButton("Elimina") { _, _ ->
                        viewModel.deleteShift(turno.id)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }

            binding.shiftsLayout.addView(cardBinding.root)
        }
        
        // Aggiungi i bottoni in fondo dopo le card
        addBottonsAtBottom()
    }

    // Mostra un messaggio quando non ci sono turni per la data selezionata
    private fun showEmptyState() {
        binding.shiftsLayout.removeAllViews()
        binding.buttonsLayout.removeAllViews()
        binding.bottomButtonsLayout.removeAllViews()
        
        val textView = TextView(requireContext())
        textView.text = "Nessun turno per questa data"
        textView.textSize = 16f
        textView.setPadding(20, 20, 20, 20)
        binding.shiftsLayout.addView(textView)
        
        // Aggiungi i bottoni in fondo anche quando non ci sono turni
        addBottonsAtBottom()
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
        val startAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, timeList)
        startSpinner.adapter = startAdapter
        startSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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
        val endAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, timeList)
        endSpinner.adapter = endAdapter
        endSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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

        // Spaziatore per separare gli elementi
        val spacer2 = View(requireContext())
        spacer2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20
        )
        layout.addView(spacer2)

        // Titolo per la sezione modalità di lavoro
        val tvWorkMode = TextView(requireContext())
        tvWorkMode.text = "Modalità di lavoro:"
        tvWorkMode.textSize = 16f
        layout.addView(tvWorkMode)

        // RadioGroup per scegliere tra presenza e smartworking
        val radioGroup = RadioGroup(requireContext())
        radioGroup.orientation = RadioGroup.HORIZONTAL

        val rbPresenza = RadioButton(requireContext())
        rbPresenza.id = View.generateViewId()
        rbPresenza.text = "Presenza"
        rbPresenza.isChecked = true // Selezionato di default

        val rbSmartworking = RadioButton(requireContext())
        rbSmartworking.id = View.generateViewId()
        rbSmartworking.text = "Smartworking"

        radioGroup.addView(rbPresenza)

        // Spaziatore tra i radio button
        val radioSpacer = View(requireContext())
        radioSpacer.layoutParams = LinearLayout.LayoutParams(
            20,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        radioGroup.addView(radioSpacer)

        radioGroup.addView(rbSmartworking)
        layout.addView(radioGroup)

        // Costruzione e visualizzazione del dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Nuovo Turno")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val timeRange = "$selectedStartTime - $selectedEndTime"
                val title = "Turno $selectedDate"

                // Determina la modalità di lavoro selezionata
                val workMode = if (rbPresenza.isChecked) "presenza" else "smartworking"

                // Salva il nuovo turno tramite ViewModel
                viewModel.saveShift(title, timeRange, "", workMode)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    /**
     * Bottone "Orari Settori" FISSO sopra il calendario - per TUTTI gli utenti
     */
    private fun addOrariSettoriButtonAboveCalendar() {
        Log.d("ShiftFragment", "addOrariSettoriButtonAboveCalendar() chiamato")
        val layout = binding.buttonsLayout
        
        val button = Button(requireContext())
        button.text = "📋 Orari Settori"
        button.setBackgroundResource(R.drawable.round_button)
        button.setTextColor(resources.getColor(android.R.color.white, null))
        button.setPadding(20, 20, 20, 20)
        button.setOnClickListener {
            adminViewModel.caricaSettori()
        }
        
        layout.addView(button)
    }
    
    /**
     * Bottone "Orari Settori" nella zona scrollabile - solo per DIPENDENTI
     */
    private fun addOrariSettoriButtonForEmployees() {
        Log.d("ShiftFragment", "addOrariSettoriButtonForEmployees() chiamato")
        val layout = binding.shiftsLayout
        
        val button = Button(requireContext())
        button.text = "📋 Orari Settori"
        button.setBackgroundResource(R.drawable.round_button)
        button.setTextColor(resources.getColor(android.R.color.white, null))
        button.setPadding(20, 20, 20, 20)
        button.setOnClickListener {
            adminViewModel.caricaSettori()
        }
        
        layout.addView(button)
        
        // Separatore
        val separator = View(requireContext())
        separator.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 8
        )
        separator.setBackgroundColor(resources.getColor(R.color.purple_500, null))
        layout.addView(separator)
    }
    
    /**
     * Aggiunge i bottoni in fondo alla schermata scrollabile
     */
    private fun addBottonsAtBottom() {
        val layout = binding.bottomButtonsLayout
        
        // Bottone Orari Settori (per tutti)
        val orariButton = Button(requireContext())
        orariButton.text = "📋 Orari Settori"
        // Cambia colore se l'utente è admin
        if (isAdmin) {
            orariButton.setBackgroundResource(R.drawable.round_button_admin)
        } else {
            orariButton.setBackgroundResource(R.drawable.round_button)
        }
        orariButton.setTextColor(resources.getColor(android.R.color.white, null))
        orariButton.setPadding(20, 20, 20, 20)
        orariButton.setOnClickListener {
            adminViewModel.caricaSettori()
        }
        layout.addView(orariButton)
        
        // Solo per admin: Bottone Genera Turni
        if (isAdmin) {
            val generaButton = Button(requireContext())
            generaButton.text = "🤖 Genera Turni Automatici"
            generaButton.setBackgroundResource(R.drawable.round_button_admin)
            generaButton.setTextColor(resources.getColor(android.R.color.white, null))
            generaButton.setPadding(20, 20, 20, 20)
            generaButton.setOnClickListener {
                mostraDialogConfigurazioneScheduling()
            }
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 16, 0, 0)
            generaButton.layoutParams = layoutParams
            
            layout.addView(generaButton)
        }
    }
    
    /**
     * Bottone "Genera Turni" FISSO sopra il calendario - solo per ADMIN
     */
    private fun addGeneraTurniButtonAboveCalendar() {
        Log.d("ShiftFragment", "addGeneraTurniButtonAboveCalendar() chiamato")
        val layout = binding.buttonsLayout
        
        val button = Button(requireContext())
        button.text = "🤖 Genera Turni"
        button.setBackgroundResource(R.drawable.round_button_admin)
        button.setTextColor(resources.getColor(android.R.color.white, null))
        button.setPadding(20, 20, 20, 20)
        button.setOnClickListener {
            mostraDialogConfigurazioneScheduling()
        }
        
        layout.addView(button)
    }
    
    /**
     * Bottone "Genera Turni" - solo per ADMIN (VECCHIO - NON USATO)
     */
    private fun addGeneraTurniButtonForAdmin() {
        Log.d("ShiftFragment", "addGeneraTurniButtonForAdmin() chiamato - isAdmin: $isAdmin")
        val layout = binding.shiftsLayout
        
        val button = Button(requireContext())
        button.text = "🤖 Genera Turni Automatici"
        button.setBackgroundResource(R.drawable.round_button_admin)
        button.setTextColor(resources.getColor(android.R.color.white, null))
        button.setPadding(20, 20, 20, 20)
        button.setOnClickListener {
            mostraDialogConfigurazioneScheduling()
        }
        
        layout.addView(button)
        
        // Separatore
        val separator = View(requireContext())
        separator.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 8
        )
        separator.setBackgroundColor(resources.getColor(R.color.admin_primary, null))
        layout.addView(separator)
    }
    
    
    /**
     * Mostra dialog per configurare la generazione turni
     */
    private fun mostraDialogConfigurazioneScheduling() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        
        // Data inizio
        val startLabel = TextView(requireContext())
        startLabel.text = "Data inizio:"
        startLabel.textSize = 16f
        layout.addView(startLabel)
        
        val startEdit = EditText(requireContext())
        startEdit.hint = "yyyy-MM-dd"
        startEdit.setText(selectedDate)
        layout.addView(startEdit)
        
        // Spaziatore
        val spacer1 = View(requireContext())
        spacer1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20
        )
        layout.addView(spacer1)
        
        // Data fine
        val endLabel = TextView(requireContext())
        endLabel.text = "Data fine:"
        endLabel.textSize = 16f
        layout.addView(endLabel)
        
        val endEdit = EditText(requireContext())
        endEdit.hint = "yyyy-MM-dd"
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        endEdit.setText(dbDateFormat.format(calendar.time))
        layout.addView(endEdit)
        
        // Spaziatore
        val spacer2 = View(requireContext())
        spacer2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20
        )
        layout.addView(spacer2)
        
        // Checkbox weekend
        val weekendCheck = CheckBox(requireContext())
        weekendCheck.text = "Includi weekend"
        weekendCheck.textSize = 16f
        layout.addView(weekendCheck)
        
        AlertDialog.Builder(requireContext())
            .setTitle("🤖 Genera Turni Automatici")
            .setMessage("I turni saranno generati basandosi sugli orari dei settori aziendali:\n• Assistenza Clienti\n• Vendite\n• Amministrazione\n• Tecnico\n• Logistica")
            .setView(layout)
            .setPositiveButton("Genera Turni") { _, _ ->
                val parametri = ParametriScheduling(
                    dataInizio = startEdit.text.toString(),
                    dataFine = endEdit.text.toString(),
                    includiWeekend = weekendCheck.isChecked
                )
                adminViewModel.generaTurniConSettori(parametri)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    /**
     * Mostra la SCHEDA con i turni generati RAGGRUPPATI PER GIORNI
     */
    private fun mostraSchedaTurniGenerati(turni: List<TurnoGenerato>) {
        val scrollView = ScrollView(requireContext())
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(30, 30, 30, 30)
        
        // Titolo
        val titleView = TextView(requireContext())
        titleView.text = "📋 Turni Generati (${turni.size})"
        titleView.textSize = 20f
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        titleView.setTextColor(resources.getColor(R.color.admin_primary, null))
        titleView.setPadding(0, 0, 0, 20)
        layout.addView(titleView)
        
        // Sottotitolo
        val subtitleView = TextView(requireContext())
        subtitleView.text = "Turni generati rispettando le disponibilità reali dei dipendenti:"
        subtitleView.textSize = 14f
        subtitleView.setPadding(0, 0, 0, 20)
        layout.addView(subtitleView)
        
        // RAGGRUPPA TURNI PER GIORNO E POI PER PERSONA
        val turniPerGiorno = turni.groupBy { it.data }.toSortedMap()
        
        for ((data, turniGiorno) in turniPerGiorno) {
            // Titolo del giorno
            val giornoTitleView = TextView(requireContext())
            giornoTitleView.text = "📅 ${formattaDataGiorno(data)}"
            giornoTitleView.textSize = 18f
            giornoTitleView.setTypeface(null, android.graphics.Typeface.BOLD)
            giornoTitleView.setTextColor(resources.getColor(R.color.admin_primary, null))
            giornoTitleView.setPadding(0, 15, 0, 10)
            layout.addView(giornoTitleView)
            
            // Raggruppa i turni per persona per questo giorno
            val turniPerPersona = raggruppaPerPersona(turniGiorno)
            
            for ((persona, turniPersona) in turniPerPersona) {
                val personaCard = creaPersonaCard(persona, turniPersona)
                layout.addView(personaCard)
                
                // Spaziatore tra persone
                val spacer = View(requireContext())
                spacer.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8
                )
                layout.addView(spacer)
            }
            
            // Separatore tra giorni diversi
            val separatoreGiorno = View(requireContext())
            separatoreGiorno.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            )
            separatoreGiorno.setBackgroundColor(resources.getColor(R.color.admin_primary, null))
            separatoreGiorno.setPadding(0, 20, 0, 0)
            layout.addView(separatoreGiorno)
        }
        
        scrollView.addView(layout)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Turni Generati Automaticamente")
            .setView(scrollView)
            .setPositiveButton("✅ OK") { _, _ ->
                // Chiude semplicemente il dialog, nessun salvataggio
            }
            .show()
    }
    
    /**
     * Crea una card per visualizzare un turno generato
     */
    private fun creaTurnoCard(turno: TurnoGenerato, numero: Int, mostraData: Boolean = true): View {
        val cardLayout = LinearLayout(requireContext())
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(20, 15, 20, 15)
        cardLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        
        // Numero turno
        val numeroView = TextView(requireContext())
        numeroView.text = "Turno #$numero"
        numeroView.textSize = 16f
        numeroView.setTypeface(null, android.graphics.Typeface.BOLD)
        numeroView.setTextColor(resources.getColor(R.color.admin_primary, null))
        cardLayout.addView(numeroView)
        
        // Data e orario (mostra data solo se richiesto)
        val dataOrarioView = TextView(requireContext())
        dataOrarioView.text = if (mostraData) {
            "📅 ${turno.data} • ⏰ ${turno.orarioInizio} - ${turno.orarioFine}"
        } else {
            "⏰ ${turno.orarioInizio} - ${turno.orarioFine}"
        }
        dataOrarioView.textSize = 14f
        dataOrarioView.setPadding(0, 5, 0, 5)
        cardLayout.addView(dataOrarioView)
        
        // Dipendenti
        val dipendentiView = TextView(requireContext())
        dipendentiView.text = "👥 Dipendenti: ${turno.dipendenti.joinToString(", ")}"
        dipendentiView.textSize = 14f
        dipendentiView.setPadding(0, 5, 0, 5)
        cardLayout.addView(dipendentiView)
        
        // Modalità
        val modalitaView = TextView(requireContext())
        val icona = if (turno.modalita == "presenza") "🏢" else "🏠"
        modalitaView.text = "$icona Modalità: ${turno.modalita.capitalize()}"
        modalitaView.textSize = 14f
        modalitaView.setPadding(0, 5, 0, 0)
        cardLayout.addView(modalitaView)
        
        return cardLayout
    }
    
    /**
     * Raggruppa i turni per persona (ogni persona può avere più turni nello stesso giorno)
     */
    private fun raggruppaPerPersona(turni: List<TurnoGenerato>): Map<String, List<TurnoGenerato>> {
        val turniPerPersona = mutableMapOf<String, MutableList<TurnoGenerato>>()
        
        for (turno in turni) {
            for (dipendente in turno.dipendenti) {
                if (!turniPerPersona.containsKey(dipendente)) {
                    turniPerPersona[dipendente] = mutableListOf()
                }
                turniPerPersona[dipendente]?.add(turno)
            }
        }
        
        return turniPerPersona.mapValues { it.value.toList() }.toSortedMap()
    }
    
    /**
     * Crea una card per una persona con tutti i suoi turni
     */
    private fun creaPersonaCard(persona: String, turni: List<TurnoGenerato>): View {
        val cardLayout = LinearLayout(requireContext())
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(16, 12, 16, 12)
        cardLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        
        // Nome persona
        val nomeView = TextView(requireContext())
        nomeView.text = "👤 $persona"
        nomeView.textSize = 16f
        nomeView.setTypeface(null, android.graphics.Typeface.BOLD)
        nomeView.setTextColor(resources.getColor(R.color.admin_primary, null))
        cardLayout.addView(nomeView)
        
        // Lista turni per questa persona
        for (turno in turni) {
            val turnoView = TextView(requireContext())
            val icona = if (turno.modalita == "presenza") "🏢" else "🏠"
            turnoView.text = "   $icona ${turno.orarioInizio} - ${turno.orarioFine} (${turno.modalita.capitalize()})"
            turnoView.textSize = 14f
            turnoView.setPadding(0, 4, 0, 0)
            cardLayout.addView(turnoView)
        }
        
        return cardLayout
    }
    
    /**
     * Formatta la data per mostrare il giorno della settimana
     */
    private fun formattaDataGiorno(data: String): String {
        return try {
            val date = dbDateFormat.parse(data)
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                val giornoSettimana = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Lunedì"
                    Calendar.TUESDAY -> "Martedì"
                    Calendar.WEDNESDAY -> "Mercoledì"
                    Calendar.THURSDAY -> "Giovedì"
                    Calendar.FRIDAY -> "Venerdì"
                    Calendar.SATURDAY -> "Sabato"
                    Calendar.SUNDAY -> "Domenica"
                    else -> ""
                }
                "$giornoSettimana $data"
            } else {
                data
            }
        } catch (e: Exception) {
            data
        }
    }
    
    /**
     * Mostra dialog con gli orari dei settori aziendali
     */
    private fun updateUiForAdmin() {
        val adminPrimaryColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        val adminVariantColor = ContextCompat.getColor(requireContext(), R.color.admin_primary_variant)

        // Aggiorna titolo e sottotitolo
        binding.textViewShift.setTextColor(adminPrimaryColor)
        binding.textDescriptionShift.setTextColor(adminVariantColor)
        binding.textDescriptionShift.text = "Gestione turni del team"

        // Aggiorna colore testi data e ore
        binding.textSelectedDate.setTextColor(adminPrimaryColor)
        binding.textMonthlyHours.setTextColor(adminPrimaryColor)

        // Nasconde il pulsante di aggiunta turno singolo per l'admin
        binding.btnAddShift.visibility = View.GONE

        // Ricarica i bottoni per applicare il colore admin
        addBottonsAtBottom()
    }

    private fun mostraOrariSettori(settori: List<SettoreAziendale>) {
        val scrollView = ScrollView(requireContext())
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(30, 30, 30, 30)
        
        // Titolo
        val titleView = TextView(requireContext())
        titleView.text = "📋 Orari Settori Aziendali"
        titleView.textSize = 20f
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        titleView.setTextColor(resources.getColor(R.color.admin_primary, null))
        titleView.setPadding(0, 0, 0, 20)
        layout.addView(titleView)
        
        // Sottotitolo
        val subtitleView = TextView(requireContext())
        subtitleView.text = "Orari di funzionamento per ogni settore aziendale:"
        subtitleView.textSize = 14f
        subtitleView.setPadding(0, 0, 0, 20)
        layout.addView(subtitleView)
        
        // Card per ogni settore
        for (settore in settori) {
            val settoreCard = creaSettoreCard(settore)
            layout.addView(settoreCard)
            
            // Spaziatore tra settori
            val spacer = View(requireContext())
            spacer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 20
            )
            layout.addView(spacer)
        }
        
        scrollView.addView(layout)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Orari Settori Aziendali")
            .setView(scrollView)
            .setPositiveButton("✅ OK", null)
            .show()
    }
    
    /**
     * Crea una card per visualizzare un settore e i suoi orari
     */
    private fun creaSettoreCard(settore: SettoreAziendale): View {
        val cardLayout = LinearLayout(requireContext())
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.setPadding(20, 15, 20, 15)
        cardLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        
        // Nome settore
        val nomeView = TextView(requireContext())
        nomeView.text = "🏢 ${settore.nome}"
        nomeView.textSize = 18f
        nomeView.setTypeface(null, android.graphics.Typeface.BOLD)
        nomeView.setTextColor(resources.getColor(R.color.admin_primary, null))
        cardLayout.addView(nomeView)
        
        // Descrizione
        val descrizioneView = TextView(requireContext())
        descrizioneView.text = settore.descrizione
        descrizioneView.textSize = 14f
        descrizioneView.setPadding(0, 5, 0, 10)
        cardLayout.addView(descrizioneView)
        
        // Orari per ogni giorno
        val giorniItaliani = mapOf(
            "lunedi" to "Lunedì",
            "martedi" to "Martedì",
            "mercoledi" to "Mercoledì",
            "giovedi" to "Giovedì",
            "venerdi" to "Venerdì",
            "sabato" to "Sabato",
            "domenica" to "Domenica"
        )
        
        for ((giorno, orari) in settore.orariSettimana) {
            if (orari.isNotEmpty()) {
                val giornoView = TextView(requireContext())
                val nomeGiorno = giorniItaliani[giorno] ?: giorno.capitalize()
                giornoView.text = "📅 $nomeGiorno: ${orari.joinToString(", ")}"
                giornoView.textSize = 14f
                giornoView.setPadding(10, 3, 0, 3)
                cardLayout.addView(giornoView)
            }
        }
        
        return cardLayout
    }
    
}

