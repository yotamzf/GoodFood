package com.example.goodfoodapp.authenticate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.Navigation
import com.example.goodfoodapp.R
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.utils.Validator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SignUp : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val validator = Validator() // Initialize the Validator
    private lateinit var userRepository: UserRepository
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Firestore and Room (local database)
        firestore = FirebaseFirestore.getInstance()
        val userDao = AppDatabase.getInstance(requireContext()).userDao()
        userRepository = UserRepository(userDao, firestore)

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

            if (validateForm(email, name, password, repeatPassword)) {
                signUpUser(email, name, password, view)
            }
        }

        loginTextView.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        return view
    }

    private fun validateForm(email: String, name: String, password: String, repeatPassword: String): Boolean {
        // Use the Validator utility for validation
        if (!validator.validateEmail(email)) {
            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!validator.validateName(name)) {
            Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!validator.validatePassword(password)) {
            Toast.makeText(context, "Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number", Toast.LENGTH_LONG).show()
            return false
        }
        if (!validator.validateConfirmPassword(password, repeatPassword)) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun signUpUser(email: String, name: String, password: String, view: View) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        // Create user object with signup date and placeholder profilePic
                        val userId = firebaseUser.uid
                        val profilePicUrl = "" //
                        val signupDate = System.currentTimeMillis()
                        val user = User(userId, email, name, profilePicUrl, signupDate)

                        // Save user to Firestore
                        saveUserToFirestore(user)

                        // Save user to Room (local cache)
                        saveUserToLocalDatabase(user)

                        Toast.makeText(context, "Sign Up Successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to the login screen
                        Navigation.findNavController(view).navigate(R.id.action_signUpFragment_to_loginFragment)
                    }
                } else {
                    Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirestore(user: User) {
        firestore.collection("users").document(user.userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(context, "User saved to Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToLocalDatabase(user: User) {
        GlobalScope.launch(Dispatchers.IO) {
            userRepository.insertUserLocally(user)
        }
    }
}
