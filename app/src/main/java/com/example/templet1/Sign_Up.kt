package com.example.templet1

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.templet1.databinding.FragmentSignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.Login
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import java.util.Calendar

class Sign_Up : BottomSheetDialogFragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Google
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 100

    // Facebook
    private lateinit var callbackManager: CallbackManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Google setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Facebook setup
        callbackManager = CallbackManager.Factory.create()

        // Date picker
        binding.dobInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, y, m, d -> binding.dobInput.setText("$d/${m + 1}/$y") },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Email signup
        binding.signupButton.setOnClickListener { registerUser() }

        // Google login
        binding.socialLoginLayout.getChildAt(0).setOnClickListener { googleSignIn() }

        // Facebook login
        binding.socialLoginLayout.getChildAt(1).setOnClickListener { facebookSignIn() }

        // Navigate to Login (your own fragment)
        binding.loginPromptText.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.login_fragment_container, Log_In())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Notify the activity to show main UI
        (activity as? Get_In)?.showRoleSelectionUI()
    }








    //  Email signup
    private fun registerUser() {
        val name = binding.nameInput.text.toString().trim()
        val mobile = binding.mobileInput.text.toString().trim()
        val dob = binding.dobInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mobile) || TextUtils.isEmpty(dob)
            || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)
        ) {
            Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful && auth.currentUser != null) {
                val uid = auth.currentUser!!.uid
                val user = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "mobile" to mobile,
                    "dob" to dob,
                    "email" to email,
                    "provider" to "email"
                )
                saveUserToFirestore(uid, user)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Auth Error: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------------- GOOGLE SIGNIN ----------------
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                if (account != null) handleGoogleResult(account)
            } catch (e: Exception) {
                Log.e("GOOGLE_SIGNIN", "Error: ${e.message}", e)
            }
        }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleLauncher.launch(signInIntent)
    }

    private fun handleGoogleResult(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful && auth.currentUser != null) {
                val uid = auth.currentUser!!.uid
                val user = mapOf(
                    "uid" to uid,
                    "name" to (account.displayName ?: ""),
                    "email" to (account.email ?: ""),
                    "mobile" to "",
                    "dob" to "",
                    "provider" to "google"
                )
                saveUserToFirestore(uid, user)
            } else {
                Toast.makeText(requireContext(), "Google Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- FACEBOOK SIGNIN ----------------
    private fun facebookSignIn() {
        val loginManager = LoginManager.getInstance()
        loginManager.logInWithReadPermissions(this, listOf("email", "public_profile"))
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful && auth.currentUser != null) {
                        val uid = auth.currentUser!!.uid
                        val profile = com.facebook.Profile.getCurrentProfile()
                        val user = mapOf(
                            "uid" to uid,
                            "name" to (profile?.name ?: ""),
                            "email" to (auth.currentUser?.email ?: ""),
                            "mobile" to "",
                            "dob" to "",
                            "provider" to "facebook"
                        )
                        saveUserToFirestore(uid, user)
                    } else {
                        Toast.makeText(requireContext(), "Facebook Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancel() {
                Toast.makeText(requireContext(), "Facebook login cancelled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(requireContext(), "Facebook Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ---------------- SAVE TO FIRESTORE ----------------
    private fun saveUserToFirestore(uid: String, user: Map<String, Any?>) {
        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Welcome ${user["name"]}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
