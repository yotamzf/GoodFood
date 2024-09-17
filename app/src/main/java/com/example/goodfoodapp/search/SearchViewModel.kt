package com.example.goodfoodapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.models.RecipeWithUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel() : ViewModel() {
    private lateinit var recipeRepository: RecipeRepository

    private val _recipes = MutableLiveData<List<RecipeWithUser>>()
    val recipes: LiveData<List<RecipeWithUser>> get() = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun initialize(repository: RecipeRepository) {
        this.recipeRepository = repository
    }

    fun fetchAllRecipes() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val recipes = withContext(Dispatchers.IO) {
                    recipeRepository.getAllRecipesWithUserDetails()
                }
                _recipes.value = recipes
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun performSearch(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val searchResults = withContext(Dispatchers.IO) {
                    recipeRepository.searchRecipesWithUserDetails(query)
                }
                _recipes.value = searchResults
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
