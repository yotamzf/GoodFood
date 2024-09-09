package com.example.goodfoodapp.utils

import android.util.Patterns

class Validator {
    // Regular expression to enforce password complexity:
    // At least one uppercase letter, one lowercase letter, one number, and a minimum of 6 characters.
    private val _passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$".toRegex()

    // Validates that the email is in a correct format
    fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Validates that the password meets the required pattern
    fun validatePassword(password: String): Boolean {
        return password.isNotEmpty() && _passwordPattern.matches(password)
    }

    // Checks if password and confirmation password are the same
    fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    // Validates that the name is not empty
    fun validateName(name: String): Boolean {
        return name.isNotEmpty()
    }
}
