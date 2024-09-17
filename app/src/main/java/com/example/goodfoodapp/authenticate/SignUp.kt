package com.example.goodfoodapp.authenticate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentSignUpBinding

class SignUp : Fragment() {

    private lateinit var signUpViewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSignUpBinding.inflate(inflater, container, false)

        // Initialize ViewModel
        signUpViewModel = ViewModelProvider(this)[SignUpViewModel::class.java]
        signUpViewModel.setContext(requireContext())

        val emailEditText = binding.etEmail
        val nameEditText = binding.etName
        val passwordEditText = binding.etPassword
        val repeatPasswordEditText = binding.etRepeatPassword
        val signUpButton = binding.btnSignUp
        val loginTextView = binding.tvLogin

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val repeatPassword = repeatPasswordEditText.text.toString().trim()

            // Validate input fields using public methods from ViewModel
            val isEmailValid = signUpViewModel.validateEmail(email)
            val isNameValid = signUpViewModel.validateName(name)
            val isPasswordValid = signUpViewModel.validatePassword(password)
            val isConfirmPasswordValid = signUpViewModel.validateConfirmPassword(password, repeatPassword)

            if (isEmailValid && isNameValid && isPasswordValid && isConfirmPasswordValid) {
                signUpViewModel.signUpUser(
                    email, name, password,
                    onSuccess = {
                        showToast("Sign Up Successful!")
                        Navigation.findNavController(binding.root).navigate(R.id.action_signUpFragment_to_loginFragment)
                    },
                    onFailure = { errorMessage ->
                        showToast(errorMessage)
                    }
                )
            } else {
                if (!isEmailValid) {
                    emailEditText.error = "Invalid email"
                    emailEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }

                if (!isNameValid) {
                    nameEditText.error = "Name can't be empty"
                    nameEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }

                if (!isPasswordValid) {
                    passwordEditText.error = "Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number"
                    passwordEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }

                if (!isConfirmPasswordValid) {
                    repeatPasswordEditText.error = "Passwords do not match"
                    repeatPasswordEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
            }
        }

        loginTextView.setOnClickListener {
            Navigation.findNavController(binding.root).navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        return binding.root
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
