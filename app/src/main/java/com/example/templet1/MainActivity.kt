package com.example.templet1

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup BottomNav
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)
        bottomNav.setupWithNavController(navController)

        // Handle profile icon click
        findViewById<ImageView>(R.id.profileIcon).setOnClickListener {
            // navigate directly if added to nav_graph
            try {
                navController.navigate(R.id.profile)
            } catch (e: IllegalArgumentException) {
                // if profile is not in this nav_graph, replace manually
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, Profile())
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Handle notification icon click
        findViewById<ImageView>(R.id.notificationBell).setOnClickListener {
            try {
                navController.navigate(R.id.notification)
            } catch (e: IllegalArgumentException) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, Notification())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
