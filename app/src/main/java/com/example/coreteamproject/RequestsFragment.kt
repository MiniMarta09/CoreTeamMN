package com.example.coreteamproject

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreteamproject.databinding.FragmentRequestsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RequestsViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUserId: String? = null
    private lateinit var requestAdapter: RequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUserId = firebaseAuth.currentUser?.uid

        setupRecyclerView()

        // Click listener per aggiungere una nuova richiesta
        binding.fabAddRequest.setOnClickListener {
            showRequestTypeDialog()
        }

        // Osserva le richieste e aggiorna la UI
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            requestAdapter.submitList(requests)
        }

        // Osserva gli errori e li mostra in un Toast
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupRecyclerView() {
        requestAdapter = RequestAdapter(
            currentUserId = currentUserId,
            onDeleteClicked = { request -> showDeleteConfirmationDialog(request) }
        )
        binding.recyclerViewRequests.apply {
            adapter = requestAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // Mostra dialog per scegliere il tipo di richiesta
    private fun showRequestTypeDialog() {
        val requestTypes = arrayOf("Ferie", "Permesso Entrata", "Permesso Uscita", "Smartworking")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tipo di Richiesta")
            .setItems(requestTypes) { _, which ->
                val selectedType = when (which) {
                    0 -> RequestsViewModel.RequestType.FERIE
                    1 -> RequestsViewModel.RequestType.PERMESSO_ENTRATA
                    2 -> RequestsViewModel.RequestType.PERMESSO_USCITA
                    3 -> RequestsViewModel.RequestType.SMARTWORKING
                    else -> RequestsViewModel.RequestType.FERIE
                }
                showRequestDialog(selectedType)
            }
            .show()
    }

    // Mostra dialog per inserire i dettagli della richiesta
    private fun showRequestDialog(requestType: RequestsViewModel.RequestType) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_new_request, null)

        val editTextReason = dialogView.findViewById<EditText>(R.id.editTextReason)
        val buttonStartDate = dialogView.findViewById<Button>(R.id.buttonStartDate)
        val buttonEndDate = dialogView.findViewById<Button>(R.id.buttonEndDate)
        val buttonStartTime = dialogView.findViewById<Button>(R.id.buttonStartTime)
        val buttonEndTime = dialogView.findViewById<Button>(R.id.buttonEndTime)
        val layoutEndDate = dialogView.findViewById<LinearLayout>(R.id.layoutEndDate)
        val layoutTime = dialogView.findViewById<LinearLayout>(R.id.layoutTime)

        var selectedStartDate: Date? = null
        var selectedEndDate: Date? = null
        var selectedStartTime: String? = null
        var selectedEndTime: String? = null

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Configura la visibilità dei campi in base al tipo di richiesta
        when (requestType) {
            RequestsViewModel.RequestType.FERIE, RequestsViewModel.RequestType.SMARTWORKING -> {
                layoutEndDate.visibility = View.VISIBLE
                layoutTime.visibility = View.GONE
            }
            RequestsViewModel.RequestType.PERMESSO_ENTRATA, RequestsViewModel.RequestType.PERMESSO_USCITA -> {
                layoutEndDate.visibility = View.GONE
                layoutTime.visibility = View.VISIBLE
            }
        }

        // Date picker per data inizio
        buttonStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedStartDate = calendar.time
                    buttonStartDate.text = dateFormat.format(selectedStartDate!!)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Date picker per data fine
        buttonEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedEndDate = calendar.time
                    buttonEndDate.text = dateFormat.format(selectedEndDate!!)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Selettore orario per orario inizio
        buttonStartTime.setOnClickListener {
            showTimeSelectionDialog(viewModel.generateTimeList()) { time ->
                selectedStartTime = time
                buttonStartTime.text = time
            }
        }

        // Selettore orario per orario fine
        buttonEndTime.setOnClickListener {
            showTimeSelectionDialog(viewModel.generateTimeList()) { time ->
                selectedEndTime = time
                buttonEndTime.text = time
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuova Richiesta - ${getRequestTypeDisplayName(requestType)}")
            .setView(dialogView)
            .setPositiveButton("Invia Richiesta") { _, _ ->
                val reason = editTextReason.text.toString().trim()

                // Validazione input
                if (reason.isEmpty()) {
                    Toast.makeText(context, "La motivazione è obbligatoria", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedStartDate == null) {
                    Toast.makeText(context, "Seleziona la data di inizio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Validazioni specifiche per tipo
                when (requestType) {
                    RequestsViewModel.RequestType.FERIE, RequestsViewModel.RequestType.SMARTWORKING -> {
                        if (selectedEndDate == null) {
                            Toast.makeText(context, "Seleziona la data di fine", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        if (selectedEndDate!!.before(selectedStartDate)) {
                            Toast.makeText(context, "La data di fine deve essere successiva a quella di inizio", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    RequestsViewModel.RequestType.PERMESSO_ENTRATA, RequestsViewModel.RequestType.PERMESSO_USCITA -> {
                        if (selectedStartTime == null) {
                            Toast.makeText(context, "Seleziona l'orario", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                }

                // Salva la richiesta
                viewModel.saveRequest(
                    type = requestType,
                    startDate = selectedStartDate!!,
                    endDate = selectedEndDate,
                    startTime = selectedStartTime,
                    endTime = selectedEndTime,
                    reason = reason
                )
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun getRequestTypeDisplayName(type: RequestsViewModel.RequestType): String {
        return when (type) {
            RequestsViewModel.RequestType.FERIE -> "Ferie"
            RequestsViewModel.RequestType.PERMESSO_ENTRATA -> "Permesso Entrata"
            RequestsViewModel.RequestType.PERMESSO_USCITA -> "Permesso Uscita"
            RequestsViewModel.RequestType.SMARTWORKING -> "Smartworking"
        }
    }

    // Mostra dialog di conferma per eliminazione
    private fun showDeleteConfirmationDialog(request: RequestsViewModel.Request) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elimina Richiesta")
            .setMessage("Sei sicuro di voler eliminare questa richiesta?")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteRequest(request.id)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Mostra un dialog per selezionare un orario da una lista
    private fun showTimeSelectionDialog(timeList: List<String>, onTimeSelected: (String) -> Unit) {
        val timeArray = timeList.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleziona Orario")
            .setItems(timeArray) { _, which ->
                onTimeSelected(timeArray[which])
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}