package com.example.goodfoodapp

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.example.goodfoodapp.utils.Validator

class SignUp : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val validator = Validator()

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
            val name = nameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val repeatPassword = repeatPasswordEditText.text.toString().trim()

            if (validateForm(email, name, password, repeatPassword)) {
                signUpUser(email, password)
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

    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign up success, navigate to Profile or main app screen
                    Toast.makeText(context, "Sign Up Successful!", Toast.LENGTH_SHORT).show()

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LoginFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    // If sign-up fails, display a message to the user.
                    Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
