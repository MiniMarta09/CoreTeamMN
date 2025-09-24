package com.example.coreteamproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.coreteamproject.databinding.FragmentShiftBinding
import com.google.android.material.tabs.TabLayoutMediator

// Fragment per la visualizzazione e modifica dei turni
class ShiftFragment : Fragment() {

    private lateinit var binding: FragmentShiftBinding
    private lateinit var usersViewModel: UsersViewModel
    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il data binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shift, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        // Inizializza i ViewModel
        usersViewModel = ViewModelProvider(requireActivity())[UsersViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Osserva il ruolo utente per decidere quale UI mostrare
        usersViewModel.userRole.observe(viewLifecycleOwner) { role ->
            isAdmin = (role == UserRole.ADMIN)
            if (isAdmin) {
                // Se l'utente è admin, mostra la nuova interfaccia a schede
                setupAdminUI()
            } else {
                // Altrimenti, mostra un'interfaccia per l'utente standard
                setupUserUI()
            }
        }
    }

    /**
     * Configura l'interfaccia per l'amministratore con TabLayout e ViewPager2.
     */
    private fun setupAdminUI() {
        binding.tabLayout.visibility = View.VISIBLE
        binding.viewPager.visibility = View.VISIBLE

        // Imposta il colore di sfondo e testo della TabLayout
        val adminPrimaryColor = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        binding.tabLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_cream))
        binding.tabLayout.setTabTextColors(ContextCompat.getColor(requireContext(), R.color.gray), adminPrimaryColor)
        binding.tabLayout.setSelectedTabIndicatorColor(adminPrimaryColor)

        // Aggiorna titolo e sottotitolo per la vista admin
        binding.textViewShift.setTextColor(adminPrimaryColor)
        binding.textDescriptionShift.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_primary_variant))
        binding.textViewShift.text = "Gestione Turni"
        binding.textDescriptionShift.text = "Strumenti per la pianificazione del team"

        // Configura il ViewPager con l'adapter per admin
        val pagerAdapter = ShiftPagerAdapter(this, isUserMode = false)
        binding.viewPager.adapter = pagerAdapter

        // Collega la TabLayout al ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orari Contratto"
                1 -> "Genera Turni"
                2 -> "Visualizza Turni"
                else -> null
            }
        }.attach()
    }

    /**
     * Configura l'interfaccia per l'utente standard (dipendente).
     * Mostra solo le schede accessibili ai dipendenti.
     */
    private fun setupUserUI() {
        binding.tabLayout.visibility = View.VISIBLE
        binding.viewPager.visibility = View.VISIBLE

        // Imposta i colori per l'utente standard
        val userPrimaryColor = ContextCompat.getColor(requireContext(), R.color.purple_500)
        binding.tabLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_cream))
        binding.tabLayout.setTabTextColors(ContextCompat.getColor(requireContext(), R.color.gray), userPrimaryColor)
        binding.tabLayout.setSelectedTabIndicatorColor(userPrimaryColor)

        // Aggiorna titolo e sottotitolo per la vista dipendente
        binding.textViewShift.setTextColor(userPrimaryColor)
        binding.textDescriptionShift.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        binding.textViewShift.text = "I Miei Turni"
        binding.textDescriptionShift.text = "Visualizza i tuoi turni assegnati"

        // Configura il ViewPager con l'adapter per dipendenti
        val pagerAdapter = ShiftPagerAdapter(this, isUserMode = true)
        binding.viewPager.adapter = pagerAdapter

        // Collega la TabLayout al ViewPager (solo schede per dipendenti)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orari Settori"
                1 -> "I Miei Turni"
                else -> null
            }
        }.attach()
    }
}
