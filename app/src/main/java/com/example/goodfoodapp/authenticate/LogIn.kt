package com.example.goodfoodapp.authenticate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.goodfoodapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class LogIn : Fragment() {

    private lateinit var logInViewModel: LogInViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log_in, container, false)

        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        val passwordEditText = view.findViewById<EditText>(R.id.etPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val signUpTextView = view.findViewById<TextView>(R.id.tvSignUpPrompt)

        // Initialize ViewModel without Factory
        logInViewModel = ViewModelProvider(this)[LogInViewModel::class.java]
        logInViewModel.setContext(requireContext()) // Set the context after ViewModel is initialized

        // Handle Login Button Click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (logInViewModel.validateInputs(email, password)) {
                logInViewModel.loginWithFirebase(email, password,
                    onSuccess = { userId ->
                        navigateToApp(view)
                    },
                    onFailure = { errorMessage ->
                        logInViewModel.showToast(errorMessage)
                    })
            } else {
                if (email.isEmpty()) emailEditText.error = "Email is required"
                if (password.isEmpty()) passwordEditText.error = "Password is required"
            }
        }

        // Handle Sign Up Prompt (navigate to sign-up screen)
        signUpTextView.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_signup_fragment)
        }

        return view
    }

    private fun navigateToApp(view: View) {
        // Navigate to My Profile Fragment
        Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_myProfileFragment)

        // Ensure BottomNavigationView is updated
        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.nav_my_profile
    }
}
