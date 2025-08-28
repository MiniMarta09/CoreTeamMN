package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.coreteamproject.databinding.CardAnnuncioBinding
import com.example.coreteamproject.databinding.FragmentBoardBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

// Fragment per la bacheca degli annunci
class BoardFragment : Fragment() {

    private var _binding: FragmentBoardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BoardViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUserId: String? = null

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

        // Click listener per aggiungere un nuovo annuncio
        binding.fabAddAnnuncio.setOnClickListener {
            showEditAnnuncioDialog(null)
        }

        // Osserva gli annunci e aggiorna la UI
        viewModel.annunci.observe(viewLifecycleOwner) { annunci ->
            updateAnnunciUI(annunci)
        }

        // Osserva gli errori e li mostra in un Toast
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    // Aggiorna la UI con la lista degli annunci
    private fun updateAnnunciUI(annunci: List<BoardViewModel.Annuncio>) {
        binding.annunciContainer.removeAllViews()
        if (annunci.isEmpty()) {
            val noAnnunciTextView = TextView(context).apply {
                text = "Nessun annuncio presente."
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
            }
            binding.annunciContainer.addView(noAnnunciTextView)
        } else {
            val inflater = LayoutInflater.from(context)
            annunci.forEach { annuncio ->
                val cardView = inflater.inflate(R.layout.card_annuncio, binding.annunciContainer, false)

                val contentTextView = cardView.findViewById<TextView>(R.id.textViewContent)
                val authorTextView = cardView.findViewById<TextView>(R.id.textViewAuthor)
                val sectorTextView = cardView.findViewById<TextView>(R.id.textViewSector)
                val dateTextView = cardView.findViewById<TextView>(R.id.textViewDate)
                val editButton = cardView.findViewById<View>(R.id.buttonEdit)
                val deleteButton = cardView.findViewById<View>(R.id.buttonDelete)
                val actionsLayout = cardView.findViewById<View>(R.id.actions_layout)

                contentTextView.text = annuncio.content
                authorTextView.text =  "${annuncio.authorName}"
                sectorTextView.text = annuncio.settore
                dateTextView.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(annuncio.timestamp)

                if (currentUserId == annuncio.userId) {
                    actionsLayout.visibility = View.VISIBLE
                } else {
                    actionsLayout.visibility = View.GONE
                }

                editButton.setOnClickListener {
                    showEditAnnuncioDialog(annuncio)
                }
                deleteButton.setOnClickListener {
                    showDeleteConfirmationDialog(annuncio)
                }

                binding.annunciContainer.addView(cardView)
            }
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
}