package com.example.coreteamproject

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.res.ColorStateList

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupBottomNavigation()

        val user = FirebaseAuth.getInstance().currentUser
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Mostra il fragment iniziale (HomeFragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Gestione del BottomNavigationView (la tua logica originale)
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

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        val iconColors = intArrayOf(
            getColor(R.color.purple_500),
            getColor(R.color.white)
        )

        val textColors = intArrayOf(
            getColor(R.color.purple_500),
            getColor(R.color.white)
        )

        bottomNavigation.itemIconTintList = ColorStateList(states, iconColors)
        bottomNavigation.itemTextColor = ColorStateList(states, textColors)
    }
}