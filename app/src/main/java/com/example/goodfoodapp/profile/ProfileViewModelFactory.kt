package com.example.goodfoodapp.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.services.ImgurApiService

class ProfileViewModelFactory(
    private val context: Context,
    private val userRepository: UserRepository,
    private val imgurApiService: ImgurApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context, userRepository, imgurApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
