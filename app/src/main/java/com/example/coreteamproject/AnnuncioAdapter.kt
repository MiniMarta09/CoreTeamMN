package com.example.coreteamproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coreteamproject.databinding.CardAnnuncioBinding
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat

class AnnuncioAdapter(
    private val currentUserId: String?,
    private val onLikeClicked: (String) -> Unit,
    private val onDislikeClicked: (String) -> Unit,
    private val onEditClicked: (BoardViewModel.Annuncio) -> Unit,
    private val onDeleteClicked: (BoardViewModel.Annuncio) -> Unit
) : ListAdapter<BoardViewModel.Annuncio, AnnuncioAdapter.AnnuncioViewHolder>(AnnuncioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnuncioViewHolder {
        val binding = CardAnnuncioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnnuncioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnuncioViewHolder, position: Int) {
        val annuncio = getItem(position)
        holder.bind(annuncio, currentUserId, onLikeClicked, onDislikeClicked, onEditClicked, onDeleteClicked)
    }

    class AnnuncioViewHolder(private val binding: CardAnnuncioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            annuncio: BoardViewModel.Annuncio,
            currentUserId: String?,
            onLikeClicked: (String) -> Unit,
            onDislikeClicked: (String) -> Unit,
            onEditClicked: (BoardViewModel.Annuncio) -> Unit,
            onDeleteClicked: (BoardViewModel.Annuncio) -> Unit
        ) {
            binding.textViewContent.text = annuncio.content
            binding.textViewAuthor.text = annuncio.authorName
            binding.textViewSector.text = annuncio.settore
            binding.textViewDate.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(annuncio.timestamp)

            // Gestione Like/Dislike
            binding.textViewLikesCount.text = annuncio.likes.toString()
            binding.textViewDislikesCount.text = annuncio.dislikes.toString()

            val likedColor = ContextCompat.getColor(itemView.context, R.color.purple_500)
            val defaultColor = ContextCompat.getColor(itemView.context, R.color.gray)

            binding.buttonLike.setColorFilter(if (annuncio.likedBy.contains(currentUserId)) likedColor else defaultColor)
            binding.buttonDislike.setColorFilter(if (annuncio.dislikedBy.contains(currentUserId)) likedColor else defaultColor)

            binding.buttonLike.setOnClickListener { onLikeClicked(annuncio.id) }
            binding.buttonDislike.setOnClickListener { onDislikeClicked(annuncio.id) }

            // Gestione bottoni Edit/Delete
            if (currentUserId == annuncio.userId) {
                binding.actionsLayout.visibility = android.view.View.VISIBLE
                binding.buttonEdit.setOnClickListener { onEditClicked(annuncio) }
                binding.buttonDelete.setOnClickListener { onDeleteClicked(annuncio) }
            } else {
                binding.actionsLayout.visibility = android.view.View.GONE
            }
        }
    }
}

    class AnnuncioDiffCallback : DiffUtil.ItemCallback<BoardViewModel.Annuncio>() {
    override fun areItemsTheSame(oldItem: BoardViewModel.Annuncio, newItem: BoardViewModel.Annuncio): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BoardViewModel.Annuncio, newItem: BoardViewModel.Annuncio): Boolean {
        // Ora che Annuncio ha un equals() corretto, possiamo confrontare direttamente gli oggetti.
        // Tuttavia, per essere sicuri che il contenuto venga verificato, confrontiamo i campi che possono cambiare.
        return oldItem.likes == newItem.likes &&
                oldItem.dislikes == newItem.dislikes &&
                oldItem.likedBy == newItem.likedBy &&
                oldItem.dislikedBy == newItem.dislikedBy
    }
}
