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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import com.example.goodfoodapp.R
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var bottomNavigationView: BottomNavigationView

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

            // Check if email or password is empty
            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                return@setOnClickListener
            }

            loginWithFirebase(email, password, view)
        }


        // Handle Sign Up Prompt (navigate to sign-up screen)
        signUpTextView.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_signup_fragment)
        }

        return view
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
                            val user = fetchUserFromFirestore(userId)
                            if (user != null) {
                                // Cache user data locally
                                userRepository.insertUserLocally(user)

                                // Navigate to My Profile Fragment
                                navigateToApp(view)
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

    private suspend fun fetchUserFromFirestore(userId: String): User? {
        return try {
            val document = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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