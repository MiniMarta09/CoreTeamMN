package com.example.coreteamproject

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
            annunci.forEach { annuncio ->
                val cardView = createAnnuncioCard(annuncio)
                binding.annunciContainer.addView(cardView)
            }
        }
    }

    // Crea la CardView per un singolo annuncio
    private fun createAnnuncioCard(annuncio: BoardViewModel.Annuncio): CardView {
        // Infla il layout della card
        val cardView = LayoutInflater.from(context).inflate(R.layout.card_annuncio, binding.annunciContainer, false) as CardView

        // Trova le view all'interno della card
        val authorTextView = cardView.findViewById<TextView>(R.id.textViewAuthor)
        val sectorTextView = cardView.findViewById<TextView>(R.id.textViewSector)
        val contentTextView = cardView.findViewById<TextView>(R.id.textViewContent)
        val dateTextView = cardView.findViewById<TextView>(R.id.textViewDate)
        val editButton = cardView.findViewById<Button>(R.id.buttonEdit)
        val deleteButton = cardView.findViewById<Button>(R.id.buttonDelete)

        // Popola la card con i dati
        contentTextView.text = annuncio.content
        dateTextView.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(annuncio.timestamp)

        authorTextView.text = annuncio.authorName
        sectorTextView.text = "Settore: ${annuncio.settore}"
        sectorTextView.visibility = View.VISIBLE

        // Mostra i bottoni solo all'autore
        val isAuthor = currentUserId == annuncio.userId
        editButton.visibility = if (isAuthor) View.VISIBLE else View.GONE
        deleteButton.visibility = if (isAuthor) View.VISIBLE else View.GONE

        if (isAuthor) {
            editButton.setOnClickListener {
                showEditAnnuncioDialog(annuncio)
            }
            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(annuncio)
            }
        }

        return cardView
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