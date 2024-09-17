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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.goodfoodapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        // Initialize ViewModel
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
        requireActivity().lifecycleScope.launch(Dispatchers.Main) {
            val navController = Navigation.findNavController(view)

            // Check the current destination to ensure we are navigating correctly
            if (navController.currentDestination?.id == R.id.loginFragment) {
                navController.navigate(R.id.action_loginFragment_to_myRecipesFragment)

                // Ensure BottomNavigationView is updated
                val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
                bottomNavigationView.selectedItemId = R.id.nav_my_recipes
                bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }
}
