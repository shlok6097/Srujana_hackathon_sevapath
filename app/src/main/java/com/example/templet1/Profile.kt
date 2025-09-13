package com.example.templet1

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : Fragment() {

    private lateinit var profileImage: ShapeableImageView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image)
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        btnSave = view.findViewById(R.id.btn_save_profile)


        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        userId = auth.currentUser?.uid

        // Fetch user data
        userId?.let { fetchUserData(it) }

        // Save button click
        btnSave.setOnClickListener {
            userId?.let { uid -> saveUserData(uid) }
        }
        val settingsIcon: ImageView = view.findViewById(R.id.settings_icon)
        settingsIcon.setOnClickListener {


            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, SettingsFragment())
                .addToBackStack(null)
                .commit()

        }



        return view
    }

    private fun fetchUserData(uid: String) {
        val userRef = firestore.collection("Users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Fill existing fields
                etName.setText(document.getString("name") ?: "")
                etEmail.setText(document.getString("email") ?: "")
                etPhone.setText(document.getString("phone") ?: "")

                // Check for missing fields and add them without overwriting existing ones
                val updates = hashMapOf<String, Any>()
                if (!document.contains("name")) updates["name"] = ""
                if (!document.contains("email")) updates["email"] = auth.currentUser?.email ?: ""
                if (!document.contains("phone")) updates["phone"] = ""

                if (updates.isNotEmpty()) {
                    userRef.update(updates)
                }

            } else {
                // Document does not exist at all → maybe warn or do nothing
                Toast.makeText(requireContext(), "User document does not exist.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error fetching profile: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun saveUserData(uid: String) {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        // Simple validation
        if (TextUtils.isEmpty(name)) {
            etName.error = "Name is required"
            return
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Email is required"
            return
        }

        val userRef = firestore.collection("users").document(uid)

        val updatedData = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "phone" to phone
        )

        userRef.update(updatedData).addOnSuccessListener {
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            // If document does not exist, create it
            userRef.set(updatedData).addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile created", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { ex ->
                Toast.makeText(requireContext(), "Error saving profile: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
