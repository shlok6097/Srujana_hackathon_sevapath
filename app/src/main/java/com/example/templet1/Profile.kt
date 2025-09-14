package com.example.templet1

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : Fragment() {

    private lateinit var profileImage: ShapeableImageView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etDob: EditText
    private lateinit var tvProvider: TextView
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
        etDob = view.findViewById(R.id.et_dob) // Make sure to add this EditText in XML
        tvProvider = view.findViewById(R.id.tv_provider) // TextView for provider
        btnSave = view.findViewById(R.id.btn_save_profile)

        // Settings icon click
        val settingsIcon: ImageView = view.findViewById(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

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

        return view
    }

    private fun fetchUserData(uid: String) {
        val userRef = firestore.collection("users").document(uid) // use lowercase consistently

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                etName.setText(document.getString("name") ?: "")
                etEmail.setText(document.getString("email") ?: "")
                etPhone.setText(document.getString("phone") ?: "")
            } else {

                val defaultData = hashMapOf(
                    "name" to auth.currentUser?.displayName.orEmpty(),
                    "email" to auth.currentUser?.email.orEmpty(),
                    "phone" to "",
                    "dob" to "",
                    "provider" to auth.currentUser?.providerData?.get(0)?.providerId.orEmpty(),
                    "uid" to uid
                )
                userRef.set(defaultData).addOnSuccessListener {
                    etName.setText(defaultData["name"] as String)
                    etEmail.setText(defaultData["email"] as String)
                    etPhone.setText(defaultData["phone"] as String)
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error creating user: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error fetching profile: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun saveUserData(uid: String) {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val dob = etDob.text.toString().trim()

        // Validation
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
            "mobile" to phone,
            "dob" to dob
        )

        userRef.update(updatedData).addOnSuccessListener {
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            // If document doesn't exist, create it
            userRef.set(updatedData).addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile created", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { ex ->
                Toast.makeText(requireContext(), "Error saving profile: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
