package com.example.goodfoodapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel() : ViewModel() {

    private val repository: UserRepository = GoodFoodApp.instance.userRepository

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    fun getUserById(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUserById(userId)
            withContext(Dispatchers.Main) {
                _user.value = user
            }
        }
    }
}
