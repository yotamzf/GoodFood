package com.example.goodfoodapp.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MyProfileViewModel : ViewModel() {

    private val userRepository = GoodFoodApp.instance.userRepository
    private val imgurApiService = GoodFoodApp.instance.imgurApiService

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _profileImageUri = MutableLiveData<Uri?>()
    val profileImageUri: LiveData<Uri?> get() = _profileImageUri

    // LiveData to track if there are unsaved changes
    private val _hasUnsavedChanges = MutableLiveData<Boolean>(false)
    val hasUnsavedChanges: LiveData<Boolean> get() = _hasUnsavedChanges

    fun loadUserData(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load user from local cache (Room) first
                val cachedUser = userRepository.getUserById(uid)
                if (cachedUser != null) {
                    withContext(Dispatchers.Main) {
                        _user.value = cachedUser
                    }
                }

                // Then, attempt to load from Firestore
                val remoteUser = userRepository.getUserByIdFromFirestore(uid)
                if (remoteUser != null && remoteUser != cachedUser) {
                    withContext(Dispatchers.Main) {
                        _user.value = remoteUser
                    }
                    userRepository.insertUserLocally(remoteUser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle any exceptions, if necessary
                }
            }
        }
    }

    fun updateUserData(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.updateUser(user)
                withContext(Dispatchers.Main) {
                    _user.value = user
                    _hasUnsavedChanges.value = false  // Reset unsaved changes after saving
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle any exceptions, if necessary
                }
            }
        }
    }

    fun setProfileImageUri(uri: Uri) {
        _profileImageUri.value = uri
    }

    fun uploadImageAndSaveUserData(
        localImagePath: String,
        updatedName: String,
        originalUser: User?
    ) {
        val file = File(localImagePath)
        imgurApiService.uploadImage(file, { imageUrl ->
            val user = originalUser?.copy(
                name = updatedName,
                profilePic = imageUrl
            ) ?: return@uploadImage
            updateUserData(user)
        }, {
            // Handle upload error
        })
    }

    // Method to check for changes
    fun checkForChanges(updatedName: String) {
        val originalUser = _user.value
        // Check if the name is changed and not empty
        val hasNameChanged = originalUser?.name != updatedName && updatedName.isNotBlank()
        _hasUnsavedChanges.value = hasNameChanged
    }
}
