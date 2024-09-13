package com.example.goodfoodapp.authenticate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.goodfoodapp.R
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.databinding.FragmentSignUpBinding
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
        val binding = FragmentSignUpBinding.inflate(inflater, container, false)

        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        val db = AppDatabase.getInstance(requireContext())
        userRepository = UserRepository(db.userDao(), FirebaseFirestore.getInstance())

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

            if (validateForm(email, name, password, repeatPassword, binding)) {
                signUpUser(email, name, password, binding.root)
            }
        }

        loginTextView.setOnClickListener {
            Navigation.findNavController(binding.root).navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        return binding.root
    }

    private fun validateForm(
        email: String,
        name: String,
        password: String,
        repeatPassword: String,
        binding: FragmentSignUpBinding
    ): Boolean {
        var isValid = true

        if (!validator.validateEmail(email)) {
            binding.etEmail.error = "Invalid email"
            binding.etEmail.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            isValid = false
        }

        if (!validator.validateName(name)) {
            binding.etName.error = "Name can't be empty"
            binding.etName.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            isValid = false
        }

        if (!validator.validatePassword(password)) {
            binding.etPassword.error = "Password must contain at least 6 characters, one uppercase letter, one lowercase letter, and one number"
            binding.etPassword.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            isValid = false
        }

        if (!validator.validateConfirmPassword(password, repeatPassword)) {
            binding.etRepeatPassword.error = "Passwords do not match"
            binding.etRepeatPassword.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            isValid = false
        }

        return isValid
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
            userRepository.updateUser(user)

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
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
