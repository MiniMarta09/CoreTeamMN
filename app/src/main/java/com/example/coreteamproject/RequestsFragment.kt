package com.example.coreteamproject

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Fragment per visualizzare e gestire le richieste degli utenti
class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RequestsViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUserId: String? = null
    private lateinit var requestAdapter: RequestAdapter
    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    // Metodo chiamato dopo che la vista del fragment è stata creata
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUserId = firebaseAuth.currentUser?.uid

        // Carica il profilo utente per determinare se è un admin
        // Questa informazione è necessaria per decidere quali richieste caricare
        loadUserProfile()

        binding.fabAddRequest.setOnClickListener {
            showRequestTypeDialog()
        }

        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            requestAdapter.submitList(requests)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    // Carica il profilo dell'utente corrente da Firestore per verificare il suo ruolo
    private fun loadUserProfile() {
        val userId = currentUserId ?: return

        FirebaseFirestore.getInstance().collection("Profili").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userRole = document.getString("RUOLO") // Legge RUOLO maiuscolo
                    isAdmin = userRole == "ADMIN"
                    if (isAdmin) {
                                                binding.textViewRequestsTitle.text = "Richieste Dipendenti"
                    }
                } else {
                    isAdmin = false
                }
                setupRecyclerView()
                viewModel.loadRequests(isAdmin)
            }
            .addOnFailureListener { e ->
                isAdmin = false
                setupRecyclerView()
                viewModel.loadRequests(isAdmin)
                Toast.makeText(context, "Errore caricamento profilo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Inizializza la RecyclerView e il suo Adapter
    private fun setupRecyclerView() {
        requestAdapter = RequestAdapter(
            currentUserId = currentUserId,
            isAdmin = isAdmin,
            onDeleteClicked = { request -> showDeleteConfirmationDialog(request) },
            onApproveClicked = { request -> showApproveConfirmationDialog(request) },
            onRejectClicked = { request -> showRejectConfirmationDialog(request) }
        )
        binding.recyclerViewRequests.apply {
            adapter = requestAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // Mostra un dialogo per scegliere il tipo di richiesta da creare (Ferie, Permesso, etc.)
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

    // Mostra il dialogo principale per la creazione di una nuova richiesta
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

        buttonStartTime.setOnClickListener {
            showTimeSelectionDialog(viewModel.generateTimeList()) { time ->
                selectedStartTime = time
                buttonStartTime.text = time
            }
        }

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

                if (reason.isEmpty()) {
                    Toast.makeText(context, "La motivazione è obbligatoria", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedStartDate == null) {
                    Toast.makeText(context, "Seleziona la data di inizio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

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

    // Funzione helper per ottenere il nome visualizzabile di un tipo di richiesta
    private fun getRequestTypeDisplayName(type: RequestsViewModel.RequestType): String {
        return when (type) {
            RequestsViewModel.RequestType.FERIE -> "Ferie"
            RequestsViewModel.RequestType.PERMESSO_ENTRATA -> "Permesso Entrata"
            RequestsViewModel.RequestType.PERMESSO_USCITA -> "Permesso Uscita"
            RequestsViewModel.RequestType.SMARTWORKING -> "Smartworking"
        }
    }

    // Mostra un dialogo di conferma prima di eliminare una richiesta
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

    // Mostra un dialogo di conferma per approvare una richiesta (solo admin)
    private fun showApproveConfirmationDialog(request: RequestsViewModel.Request) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accetta Richiesta")
            .setMessage("Sei sicuro di voler accettare questa richiesta?")
            .setPositiveButton("Accetta") { _, _ ->
                viewModel.updateRequestStatus(request.id, RequestsViewModel.RequestStatus.ACCETTATA)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Mostra un dialogo di conferma per rifiutare una richiesta (solo admin)
    private fun showRejectConfirmationDialog(request: RequestsViewModel.Request) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rifiuta Richiesta")
            .setMessage("Sei sicuro di voler rifiutare questa richiesta?")
            .setPositiveButton("Rifiuta") { _, _ ->
                viewModel.updateRequestStatus(request.id, RequestsViewModel.RequestStatus.RIFIUTATA)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Metodo chiamato quando la vista del fragment viene distrutta per pulire il binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Mostra un dialogo per selezionare un orario da una lista predefinita
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