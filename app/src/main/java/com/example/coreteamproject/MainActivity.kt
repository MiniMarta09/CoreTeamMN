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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavController

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

        // Configurazione Navigation Component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Trova il bottom navigation view nel layout
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Gestione manuale della BottomNavigationView.
        // L'approccio automatico con setupWithNavController non gestiva correttamente il back stack
        // quando si tornava alla Home da altri fragment. Cliccare sull'icona Home non aveva effetto.
        // Per risolvere, intercettiamo manualmente la selezione degli item.
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Caso specifico per il pulsante Home.
                R.id.homeFragment -> {
                    // Usiamo popBackStack per tornare a Home. Questo metodo svuota lo stack di navigazione
                    // fino a trovare l'istanza di homeFragment, garantendo che non si accumulino fragment superflui.
                    // Il flag 'inclusive' a false indica che homeFragment stesso non deve essere rimosso.
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                // Per tutti gli altri item, usiamo la navigazione standard.
                R.id.profileFragment -> {
                    // navigate() aggiunge il fragment allo stack, che è il comportamento desiderato
                    // per la navigazione verso sezioni diverse dalla Home.
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
        
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
