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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.goodfoodapp.R
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.utils.Validator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUp : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private val validator = Validator() // Initialize the Validator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        val db = AppDatabase.getInstance(requireContext())
        userRepository = UserRepository(db.userDao(), FirebaseFirestore.getInstance())

        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        val nameEditText = view.findViewById<EditText>(R.id.etName)
        val passwordEditText = view.findViewById<EditText>(R.id.etPassword)
        val repeatPasswordEditText = view.findViewById<EditText>(R.id.etRepeatPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnSignUp)
        val loginTextView = view.findViewById<TextView>(R.id.tvLogin)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val repeatPassword = repeatPasswordEditText.text.toString().trim()

            // Validate form inputs before attempting sign-up
            if (validateForm(email, name, password, repeatPassword)) {
                signUpUser(email, name, password, view)
            }
        }

        // Navigate to Login Fragment when "Already have an account?" is clicked
        loginTextView.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        return view
    }

    private fun validateForm(email: String, name: String, password: String, repeatPassword: String): Boolean {
        // Use Validator utility to validate inputs
        return when {
            !validator.validateEmail(email) -> {
                showToast("Please enter a valid email")
                false
            }
            !validator.validateName(name) -> {
                showToast("Please enter a valid name")
                false
            }
            !validator.validatePassword(password) -> {
                showToast("Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number")
                false
            }
            !validator.validateConfirmPassword(password, repeatPassword) -> {
                showToast("Passwords do not match")
                false
            }
            else -> true
        }
    }

    private fun signUpUser(email: String, name: String, password: String, view: View) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    firebaseUser?.let { user ->
                        val userId = user.uid
                        val profilePicUrl = "" // Placeholder for profile picture
                        val signupDate = System.currentTimeMillis()

                        val newUser = User(userId, email, name, profilePicUrl, signupDate)

                        // Save user to Firestore and local database
                        lifecycleScope.launch(Dispatchers.IO) {
                            saveUser(newUser, view)
                        }
                    }
                } else {
                    showToast("Sign Up Failed: ${task.exception?.message}")
                }
            }
    }

    private suspend fun saveUser(user: User, view: View) {
        try {
            userRepository.updateUser(user) // Cache user locally and update Firestore

            // Navigate to Login Fragment upon successful sign-up
            lifecycleScope.launch(Dispatchers.Main) {
                showToast("Sign Up Successful!")
                Navigation.findNavController(view).navigate(R.id.action_signUpFragment_to_loginFragment)
            }
        } catch (e: Exception) {
            lifecycleScope.launch(Dispatchers.Main) {
                showToast("Error saving user: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
