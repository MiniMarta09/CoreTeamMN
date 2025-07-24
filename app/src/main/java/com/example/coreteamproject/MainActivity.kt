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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // Richiesta permesso notifiche (solo Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar) // Imposta la Toolbar come ActionBar

        setupBottomNavigation()

        val user = FirebaseAuth.getInstance().currentUser
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Mostra il fragment iniziale (HomeFragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Gestione del BottomNavigationView
        bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_profile -> ProfileFragment()
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

    // Imposta i colori personalizzati per le icone e il testo del BottomNavigationView
    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked), // Stato selezionato
            intArrayOf(-android.R.attr.state_checked)  // Stato non selezionato
        )

        // Puoi scegliere un colore che contrasti bene con purple_500 quando l'elemento è selezionato.
        // Ad esempio, potresti usare bianco anche per lo stato selezionato, o un altro colore chiaro.
        val iconColors = intArrayOf(
            getColor(R.color.white), // Icona selezionata: bianca per essere visibile su sfondo viola
            getColor(R.color.white)  // Icona non selezionata: bianca
        )

        val textColors = intArrayOf(
            getColor(R.color.white), // Testo selezionato: bianco per essere visibile su sfondo viola
            getColor(R.color.white)  // Testo non selezionato: bianco
        )

        bottomNavigation.itemIconTintList = ColorStateList(states, iconColors)
        bottomNavigation.itemTextColor = ColorStateList(states, textColors)
    }

    // Crea un canale per le notifiche (richiesto da Android 8+)
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

    // Aggiunge il menu "Esci" nella Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    // Gestisce il click sul menu "Esci"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Logout da Firebase
                FirebaseAuth.getInstance().signOut()

                // Torna alla schermata di benvenuto
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
