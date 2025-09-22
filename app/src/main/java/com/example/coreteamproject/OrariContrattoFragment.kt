package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreteamproject.databinding.FragmentOrariContrattoBinding

class OrariContrattoFragment : Fragment() {

    private lateinit var binding: FragmentOrariContrattoBinding
    private val viewModel: AdminSchedulingViewModel by activityViewModels()
    private lateinit var adapter: OrariContrattoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_orari_contratto, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupRecyclerView()
        setupObservers()

        viewModel.loadOrariContratto()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = OrariContrattoAdapter()
        binding.recyclerViewOrari.adapter = adapter
        binding.recyclerViewOrari.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.orariContratto.observe(viewLifecycleOwner) { orari ->
            adapter.submitList(orari)
        }
    }
}
