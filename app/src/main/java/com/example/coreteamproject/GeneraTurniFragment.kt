package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.coreteamproject.databinding.FragmentGeneraTurniBinding
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class GeneraTurniFragment : Fragment() {

    private lateinit var binding: FragmentGeneraTurniBinding
    private val viewModel: AdminSchedulingViewModel by activityViewModels()

    private var dataInizioSelezionata: String? = null
    private var dataFineSelezionata: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_genera_turni, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupListeners()
        setupObservers()

        return binding.root
    }

    private fun setupObservers() {
        // Osserva la programmazione settimanale per abilitare il salvataggio e navigare
        viewModel.programmazioneSettimanale.observe(viewLifecycleOwner) { programmazione ->
            if (programmazione.isNotEmpty()) {
                binding.buttonSalvaTurni.isEnabled = true
                (parentFragment?.view?.findViewById<ViewPager2>(R.id.viewPager))?.currentItem = 2
            }
        }

        // Unico observer per i messaggi
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // Se i turni sono stati salvati, disabilita di nuovo il pulsante Salva
                if (it.contains("salvati con successo")) {
                    binding.buttonSalvaTurni.isEnabled = false
                }
                viewModel.clearMessage()
            }
        }
    }

    private fun setupListeners() {
        binding.buttonSelezionaSettimana.setOnClickListener {
            showWeekSelectionDialog()
        }

        binding.buttonGeneraTurni.setOnClickListener {
            if (dataInizioSelezionata != null && dataFineSelezionata != null) {
                // Chiama la funzione che genera solo l'anteprima in memoria
                viewModel.generaTurniPerSettimana(dataInizioSelezionata!!, dataFineSelezionata!!)
            }
        }

        binding.buttonSalvaTurni.setOnClickListener {
            viewModel.salvaTurniGenerati()
        }
    }

    private fun showWeekSelectionDialog() {
        val weeks = getUpcomingWeeks(4) // Ottiene le prossime 4 settimane
        val weekStrings = weeks.map { formatWeekRange(it.first, it.second) }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Seleziona Settimana")
            .setItems(weekStrings) { _, which ->
                val selectedWeek = weeks[which]
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dataInizioSelezionata = sdf.format(selectedWeek.first)
                dataFineSelezionata = sdf.format(selectedWeek.second)

                binding.textViewSettimanaSelezionata.text = formatWeekRange(selectedWeek.first, selectedWeek.second)
                binding.buttonGeneraTurni.isEnabled = true
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
}
