package com.example.goodfoodapp.authenticate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.lifecycle.lifecycleScope
import com.example.goodfoodapp.R
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LogIn : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log_in, container, false)

        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        val passwordEditText = view.findViewById<EditText>(R.id.etPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val signUpTextView = view.findViewById<TextView>(R.id.tvSignUpPrompt)

        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        val db = AppDatabase.getInstance(requireContext())
        userRepository = UserRepository(db.userDao(), FirebaseFirestore.getInstance())

        // Handle Login Button Click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInputs(email, password, emailEditText, passwordEditText)) {
                loginWithFirebase(email, password, view)
            }
        }

        // Handle Sign Up Prompt (navigate to sign-up screen)
        signUpTextView.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_signup_fragment)
        }

        return view
    }

    // Validate email and password inputs
    private fun validateInputs(
        email: String,
        password: String,
        emailEditText: EditText,
        passwordEditText: EditText
    ): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            isValid = false
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun loginWithFirebase(email: String, password: String, view: View) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val userId = firebaseUser?.uid ?: return@addOnCompleteListener

                    // Fetch user data from Firestore after successful login
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val user = userRepository.getUserByIdFromFirestore(userId)
                            if (user != null) {
                                userRepository.updateUser(user) // Cache user data locally
                                navigateToApp(view) // Navigate to Profile Fragment
                            } else {
                                showToast("Failed to fetch user data from Firestore")
                            }
                        } catch (e: Exception) {
                            showToast("Error: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } else {
                    showToast("Login failed. Please check your email or password.")
                }
            }
    }

    private fun navigateToApp(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            // Navigate to My Profile Fragment
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_myProfileFragment)

            // Ensure BottomNavigationView is updated
            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
            bottomNavigationView.selectedItemId = R.id.nav_my_profile
        }
    }


    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
