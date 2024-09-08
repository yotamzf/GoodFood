package com.example.goodfoodapp

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class SignUp : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        val nameEditText = view.findViewById<EditText>(R.id.etName)
        val passwordEditText = view.findViewById<EditText>(R.id.etPassword)
        val repeatPasswordEditText = view.findViewById<EditText>(R.id.etRepeatPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnSignUp)
        val loginTextView = view.findViewById<TextView>(R.id.tvLogin)

        // Handle Sign Up button click
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val repeatPassword = repeatPasswordEditText.text.toString().trim()

            if (validateForm(email, password, repeatPassword)) {
                signUpUser(email, password)
            }
        }

        // Handle Login Text Click to navigate back to Login Fragment
        loginTextView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun validateForm(email: String, password: String, repeatPassword: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != repeatPassword) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign up success, navigate to Profile or main app screen
                    Toast.makeText(context, "Sign Up Successful!", Toast.LENGTH_SHORT).show()

                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.fragment_container, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    // If sign-up fails, display a message to the user.
                    Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
