package com.example.goodfoodapp.authenticate

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.utils.Validator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {

    private lateinit var context: Context
    private val auth: FirebaseAuth = GoodFoodApp.instance.firebaseAuth
    private var userRepository: UserRepository = GoodFoodApp.instance.userRepository
    private val validator = Validator()

    fun setContext(context: Context) {
        this.context = context
    }

    // Provide public methods for validation
    fun validateEmail(email: String): Boolean {
        return validator.validateEmail(email)
    }

    fun validateName(name: String): Boolean {
        return validator.validateName(name)
    }

    fun validatePassword(password: String): Boolean {
        return validator.validatePassword(password)
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return validator.validateConfirmPassword(password, confirmPassword)
    }

    fun signUpUser(email: String, name: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    firebaseUser?.let { user ->
                        val userId = user.uid
                        val profilePicUrl = "" // Placeholder for profile picture
                        val signupDate = System.currentTimeMillis()

                        val newUser = User(userId, email, name, profilePicUrl, signupDate)

                        viewModelScope.launch(Dispatchers.IO) {
                            saveUser(newUser, onSuccess, onFailure)
                        }
                    }
                } else {
                    onFailure("Sign Up Failed: ${task.exception?.message}")
                }
            }
    }

    private suspend fun saveUser(user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        try {
            userRepository.updateUser(user)
            viewModelScope.launch(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main) {
                onFailure("Error saving user: ${e.message}")
            }
        }
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
