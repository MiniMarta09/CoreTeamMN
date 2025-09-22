package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreteamproject.databinding.FragmentBoardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

// Fragment per la bacheca degli annunci
class BoardFragment : Fragment() {

    private var _binding: FragmentBoardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BoardViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUserId: String? = null
    private lateinit var annuncioAdapter: AnnuncioAdapter
    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    // Inizializza la vista, gli observer e i listener
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUserId = firebaseAuth.currentUser?.uid

        setupRecyclerView()
        loadUserProfile()

        // Click listener per aggiungere un nuovo annuncio
        binding.fabAddAnnuncio.setOnClickListener {
            showEditAnnuncioDialog(null)
        }

        // Osserva gli annunci e aggiorna la UI
        viewModel.annunci.observe(viewLifecycleOwner) { annunci ->
            annuncioAdapter.submitList(annunci)
        }

        // Osserva gli errori e li mostra in un Toast
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun setupRecyclerView() {
        annuncioAdapter = AnnuncioAdapter(
            currentUserId = currentUserId,
            onLikeClicked = { annuncioId -> viewModel.toggleLike(annuncioId) },
            onDislikeClicked = { annuncioId -> viewModel.toggleDislike(annuncioId) },
            onEditClicked = { annuncio -> showEditAnnuncioDialog(annuncio) },
            onDeleteClicked = { annuncio -> showDeleteConfirmationDialog(annuncio) }
        )
        binding.recyclerViewAnnunci.apply {
            adapter = annuncioAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // Mostra il dialog per creare o modificare un annuncio
    private fun showEditAnnuncioDialog(annuncio: BoardViewModel.Annuncio? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_annuncio, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextAnnuncioContent)

        annuncio?.let {
            editText.setText(it.content)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (annuncio == null) "Nuovo Annuncio" else "Modifica Annuncio")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val content = editText.text.toString()
                if (content.isNotBlank()) {
                    viewModel.saveAnnuncio(content, annuncio?.id)
                } else {
                    Toast.makeText(context, "Il contenuto non può essere vuoto", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Mostra il dialog di conferma per l'eliminazione
    private fun showDeleteConfirmationDialog(annuncio: BoardViewModel.Annuncio) {
        AlertDialog.Builder(requireContext())
            .setTitle("Conferma Eliminazione")
            .setMessage("Sei sicuro di voler eliminare questo annuncio?")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteAnnuncio(annuncio.id)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Pulisce il binding quando la vista viene distrutta
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("Profili").document(userId).get()
            .addOnSuccessListener { document ->
                val userRole = document.getString("RUOLO")
                isAdmin = userRole == "ADMIN"
                if (isAdmin) {
                    updateUiForAdmin()
                }
            }
            .addOnFailureListener {
                isAdmin = false
            }
    }

    private fun updateUiForAdmin() {
        val adminPrimaryColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        val adminVariantColor = ContextCompat.getColor(requireContext(), R.color.admin_primary_variant)

        // Aggiorna titolo e sottotitolo
        binding.textViewBachecaTitle.setTextColor(adminPrimaryColor)
        binding.textViewBachecaDescription.setTextColor(adminVariantColor)

        // Aggiorna colore pulsante
        binding.fabAddAnnuncio.backgroundTintList = android.content.res.ColorStateList.valueOf(adminPrimaryColor)
    }
}