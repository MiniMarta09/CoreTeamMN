package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreteamproject.databinding.FragmentVisualizzaTurniBinding
import java.text.SimpleDateFormat
import java.util.*

class VisualizzaTurniFragment : Fragment() {

    private lateinit var binding: FragmentVisualizzaTurniBinding
    private val viewModel: AdminSchedulingViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    private lateinit var adapter: VisualizzaTurniAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visualizza_turni, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Carica automaticamente i turni della settimana corrente all'avvio
        loadCurrentWeekShifts()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = VisualizzaTurniAdapter()
        binding.recyclerViewTurni.adapter = adapter
        // Usiamo un LinearLayoutManager per una lista verticale di card
        binding.recyclerViewTurni.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        // Ora osserviamo la programmazione settimanale per persona
        viewModel.programmazioneSettimanale.observe(viewLifecycleOwner) { programmazione ->
            adapter.submitList(programmazione)
        }
    }

    private fun setupListeners() {
        binding.buttonSelezionaSettimana.setOnClickListener {
            showWeekSelectionDialog()
        }
    }


    private fun showWeekSelectionDialog() {
        val weeks = getUpcomingWeeks(4) // Mostra le prossime 4 settimane come opzione
        val weekStrings = weeks.map { formatWeekRange(it.first, it.second) }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Seleziona Settimana da Visualizzare")
            .setItems(weekStrings) { _, which ->
                val selectedWeek = weeks[which]
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDateStr = sdf.format(selectedWeek.first)
                val endDateStr = sdf.format(selectedWeek.second)

                // Aggiorna il testo della settimana selezionata
                binding.textViewSettimanaSelezionata.text = formatWeekRange(selectedWeek.first, selectedWeek.second)
                
                // Carica i turni appropriati in base al ruolo dell'utente
                usersViewModel.userRole.value?.let { userRole ->
                    when (userRole) {
                        UserRole.ADMIN -> {
                            // Admin vede tutti i turni della settimana
                            viewModel.loadShiftsForWeek(startDateStr, endDateStr)
                        }
                        UserRole.USER -> {
                            // Dipendente vede solo i suoi turni personali
                            viewModel.loadShiftsForEmployee(startDateStr, endDateStr)
                        }
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun formatWeekRange(startDate: Date, endDate: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return "${sdf.format(startDate)} - ${sdf.format(endDate)}"
    }

    private fun getUpcomingWeeks(numberOfWeeks: Int): List<Pair<Date, Date>> {
        val weeks = mutableListOf<Pair<Date, Date>>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0 until numberOfWeeks) {
            val startOfWeek = calendar.time
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val endOfWeek = calendar.time
            weeks.add(Pair(startOfWeek, endOfWeek))
            calendar.add(Calendar.DAY_OF_WEEK, 1) // Passa alla settimana successiva
        }
        return weeks
    }

    /**
     * Carica automaticamente i turni della settimana corrente all'avvio del fragment
     */
    private fun loadCurrentWeekShifts() {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        val startOfWeek = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = calendar.time
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateStr = sdf.format(startOfWeek)
        val endDateStr = sdf.format(endOfWeek)
        
        // Aggiorna il testo della settimana selezionata
        binding.textViewSettimanaSelezionata.text = formatWeekRange(startOfWeek, endOfWeek)
        
        // Controlla il ruolo dell'utente per caricare i turni appropriati
        usersViewModel.userRole.observe(viewLifecycleOwner) { userRole ->
            when (userRole) {
                UserRole.ADMIN -> {
                    // Admin vede tutti i turni della settimana
                    viewModel.loadShiftsForWeek(startDateStr, endDateStr)
                }
                UserRole.USER -> {
                    // Dipendente vede solo i suoi turni personali
                    viewModel.loadShiftsForEmployee(startDateStr, endDateStr)
                }
                else -> {
                    // Ruolo non definito, non caricare nulla
                }
            }
        }
    }

}
