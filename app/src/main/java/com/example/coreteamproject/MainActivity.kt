package com.example.coreteamproject

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var usersViewModel: UsersViewModel
    private var isAdminThemeApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Controlla se è stato passato il flag per il tema admin
        isAdminThemeApplied = intent.getBooleanExtra("ADMIN_THEME", false)
        
        // Imposta il tema appropriato prima di chiamare super.onCreate()
        if (isAdminThemeApplied) {
            setTheme(R.style.Theme_CoreTeamProject_Admin)
        } else {
            setTheme(R.style.Theme_CoreTeamProject)
        }

        super.onCreate(savedInstanceState)

        // Inizializza ViewModel
        usersViewModel = ViewModelProvider(this)[UsersViewModel::class.java]
        
        // Imposta il layout dell'interfaccia
        setContentView(R.layout.activity_main)
        
        // Carica il profilo utente per determinare il ruolo
        usersViewModel.caricaProfiloUtente()
        
        // Osserva il ruolo dell'utente per applicare il tema corretto
        usersViewModel.userRole.observe(this) { ruolo ->
            if (ruolo == UserRole.ADMIN && !isAdminThemeApplied) {
                // Riavvia l'activity con il tema admin
                recreateWithAdminTheme()
            } else if (ruolo == UserRole.ADMIN && isAdminThemeApplied) {
                // Applica i colori admin agli elementi che non sono coperti dal tema
                applyAdminUIColors()
            }
        }

        // Crea il canale per le notifiche
        createNotificationChannel()

        // Richiede il permesso per inviare notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        // Trova la toolbar definita nel layout e la imposta come action bar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurazione Navigation Component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Trova il bottom navigation view nel layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Gestione manuale della BottomNavigationView.
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    // Riavvia l'activity con il tema admin
    private fun recreateWithAdminTheme() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("ADMIN_THEME", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Applica i colori admin agli elementi UI
    private fun applyAdminUIColors() {
        val adminColor = ContextCompat.getColor(this, R.color.admin_primary)
        val adminColorSelector = ContextCompat.getColorStateList(this, R.color.bottom_nav_item_color_admin)
        
        // Aggiorna la toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val appBarLayout = findViewById<AppBarLayout>(R.id.appBarLayout)
        toolbar?.setBackgroundColor(adminColor)
        appBarLayout?.setBackgroundColor(adminColor)
        
        // Aggiorna la bottom navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.setBackgroundColor(adminColor)
        bottomNavigation?.itemIconTintList = adminColorSelector
    }

    // Crea un canale notifiche per poter inviare notifiche
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for general notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Crea il menu nella Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    // Gestisce l'evento di click sulle voci del menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}