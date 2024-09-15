package com.example.goodfoodapp.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.services.ImgurApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileViewModel(
    private val context: Context,
    private val userRepository: UserRepository,
    private val imgurApiService: ImgurApiService
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    private val _profileImageUri = MutableLiveData<Uri?>()
    val profileImageUri: LiveData<Uri?> get() = _profileImageUri

    fun loadUserData(uid: String) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cachedUser = userRepository.getUserById(uid)
                cachedUser?.let { _user.postValue(it) }

                val remoteUser = userRepository.getUserByIdFromFirestore(uid)
                if (remoteUser != null && remoteUser != cachedUser) {
                    _user.postValue(remoteUser)
                    userRepository.insertUserLocally(remoteUser)
                }
            } catch (e: Exception) {
                _message.postValue("Failed to load user data: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun saveUserData(uid: String, updatedName: String, currentProfileImageUri: Uri?) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localImagePath = currentProfileImageUri?.let { saveImageLocally(it) }
                val file = localImagePath?.let { File(it) }

                if (file != null) {
                    imgurApiService.uploadImage(file, { imageUrl ->
                        updateUserInRepository(uid, updatedName, imageUrl)
                    }, { error ->
                        _message.postValue("Image upload failed: $error")
                        _isLoading.postValue(false)
                    })
                } else {
                    updateUserInRepository(uid, updatedName, _user.value?.profilePic ?: "")
                }
            } catch (e: Exception) {
                _message.postValue("Failed to save user data: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    private fun updateUserInRepository(uid: String, updatedName: String, imageUrl: String) {
        val updatedUser = _user.value?.copy(name = updatedName, profilePic = imageUrl) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.updateUser(updatedUser)
                _message.postValue("Changes saved successfully.")
                _user.postValue(updatedUser) // This triggers the observer to disable editing
            } catch (e: Exception) {
                _message.postValue("Failed to save user data: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun saveImageLocally(uri: Uri): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_pic.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    fun setProfileImageUri(uri: Uri?) {
        _profileImageUri.value = uri
    }

    fun hasUnsavedChanges(updatedName: String, originalName: String?, currentImageUri: Uri?): Boolean {
        val hasNameChanged = originalName != updatedName
        val hasImageChanged = _profileImageUri.value != currentImageUri
        return hasNameChanged || hasImageChanged
    }
}
