package com.example.goodfoodapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.goodfoodapp.utils.Validator
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val validator = Validator()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_log_in, container, false)

        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        val passwordEditText = view.findViewById<EditText>(R.id.etPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val signUpTextView = view.findViewById<TextView>(R.id.tvSignUpPrompt)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle Login Button Click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateForm(email, password)) {
                loginWithFirebase(email, password, view)
            } else {
                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Sign Up Prompt (navigate to sign-up screen)
        signUpTextView.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_signup_fragment)
        }

        return view
    }
    private fun validateForm(email: String, password: String): Boolean{
        if (!validator.validateEmail(email)) {
            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!validator.validatePassword(password)) {
            Toast.makeText(context, "Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun loginWithFirebase(email: String, password: String, view: View) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_myRecipesFragment)
                } else {
                    // Login failed
                    Toast.makeText(context, "Email or password are incorrect", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
