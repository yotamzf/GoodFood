package com.example.goodfoodapp.authenticate

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogInViewModel : ViewModel() {

    private lateinit var context: Context
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userRepository: UserRepository

    fun setContext(context: Context) {
        this.context = context
        val db = AppDatabase.getInstance(context)
        userRepository = UserRepository(db.userDao(), FirebaseFirestore.getInstance())
    }

    // Validate email and password inputs
    fun validateInputs(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }

    // Handle login process with Firebase
    fun loginWithFirebase(email: String, password: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val userId = firebaseUser?.uid ?: return@addOnCompleteListener

                    // Fetch user data from Firestore after successful login
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val user = userRepository.getUserByIdFromFirestore(userId)
                            if (user != null) {
                                userRepository.updateUser(user) // Cache user data locally
                                onSuccess(userId)
                            } else {
                                onFailure("Failed to fetch user data from Firestore")
                            }
                        } catch (e: Exception) {
                            onFailure("Error: ${e.message}")
                        }
                    }
                } else {
                    onFailure("Login failed. Please check your email or password.")
                }
            }
    }

    fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
