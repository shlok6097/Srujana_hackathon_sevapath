package com.example.templet1

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth


class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val switchNotifications = view.findViewById<Switch>(R.id.switch_notifications)
        val switchTheme = view.findViewById<Switch>(R.id.switch_theme)
        val btnLogout = view.findViewById<Button>(R.id.btn_logout)

        // Logout with confirmation
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(requireContext(), Get_In::class.java))
                    activity?.finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Dark mode toggle
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Notifications switch
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                requireContext(),
                if (isChecked) "Notifications Enabled" else "Notifications Disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        return view
    }
}
