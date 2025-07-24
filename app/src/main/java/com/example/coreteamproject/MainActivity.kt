package com.example.coreteamproject

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.res.ColorStateList
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.Manifest
import android.content.Context
import androidx.core.app.ActivityCompat
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

// Classe principale dell'app, gestisce l'interfaccia principale
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Imposta il layout dell'interfaccia
        setContentView(R.layout.activity_main)

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

        // Imposta i colori personalizzati per la bottom navigation
        setupBottomNavigation()

        // Recupera l'utente loggato tramite FirebaseAuth
        val user = FirebaseAuth.getInstance().currentUser

        // Trova il bottom navigation view nel layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Visualizza il fragment iniziale all'avvio dell'app
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Listener per la selezione delle voci del menu in basso
        bottomNavigation.setOnItemSelectedListener { item ->
            // Seleziona il fragment da visualizzare in base all'elemento toccato
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()     // Selezionato "Home"
                R.id.nav_profile -> ProfileFragment() // Selezionato "Profile"
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }
        
    }

    // Metodo che imposta i colori delle icone e dei testi nella BottomNavigationView
    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Definisce gli stati selezionato/non selezionato
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),    // Quando l'elemento è selezionato
            intArrayOf(-android.R.attr.state_checked)    // Quando NON è selezionato
        )

        // Colori delle icone in base allo stato
        val iconColors = intArrayOf(
            getColor(R.color.white), // Icona selezionata
            getColor(R.color.white)  // Icona non selezionata
        )

        val textColors = intArrayOf(
            getColor(R.color.white), // Testo selezionato
            getColor(R.color.white)  // Testo non selezionato
        )

        // Applica i colori alle icone e ai testi del BottomNavigationView
        bottomNavigation.itemIconTintList = ColorStateList(states, iconColors)
        bottomNavigation.itemTextColor = ColorStateList(states, textColors)
    }

    // Crea un canale notifiche per poter inviare notifiche
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Definizione del canale: ID, nome e importanza
            val channel = NotificationChannel(
                "default_channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for general notifications"
            }

            // Registra il canale nel sistema
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Crea il menu nella Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu) // Carica il layout del menu
        return true
    }

    // Gestisce l'evento di click sulle voci del menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Esegue il logout dell'utente da Firebase
                FirebaseAuth.getInstance().signOut()

                // Torna alla schermata di benvenuto
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item) // Gestione default per altri item
        }
    }
}
