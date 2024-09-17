package com.example.goodfoodapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeViewModel() : ViewModel() {
    private val repository: RecipeRepository = GoodFoodApp.instance.recipeRepository

    // LiveData for individual recipe
    private val _recipe = MutableLiveData<Recipe>()
    val recipe: LiveData<Recipe> get() = _recipe

    // LiveData for all recipes by a specific user
    private val _recipesByUser = MutableLiveData<List<Recipe>>()
    val recipesByUser: LiveData<List<Recipe>> get() = _recipesByUser

    // Insert recipe via repository (both Firestore and Room)
    fun insertRecipe(recipe: Recipe) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertRecipe(recipe) // Insert into Firestore and Room
            } catch (e: Exception) {
                // Handle the error, e.g., log it or notify user
            }
        }
    }

    // Fetch recipe by ID from Firestore (or potentially another remote source)
    fun getRecipeById(recipeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recipe = repository.getRecipeById(recipeId)
                recipe?.let {
                    // Switch to main thread to update LiveData
                    withContext(Dispatchers.Main) {
                        _recipe.value = it
                    }
                } ?: run {
                    // Handle case where recipe is null
                    fetchRecipeLocally(recipeId)
                }
            } catch (e: Exception) {
                // Handle error, and fallback to local fetch if necessary
                fetchRecipeLocally(recipeId)
            }
        }
    }

    // Fetch recipe from the local database (Room)
    fun fetchRecipeLocally(recipeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recipe = repository.getRecipeLocally(recipeId)
                recipe?.let {
                    // Switch to main thread to update LiveData
                    withContext(Dispatchers.Main) {
                        _recipe.value = it
                    }
                }
            } catch (e: Exception) {
                // Handle the error appropriately, e.g., log or notify user
            }
        }
    }

    // Check if recipe ID exists (for duplication prevention)
    fun checkIfRecipeIdExists(recipeId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val existsInFirestore = repository.checkRecipeIdInFirestore(recipeId)
            val existsInRoom = repository.checkRecipeIdInRoom(recipeId)
            withContext(Dispatchers.Main) {
                callback(existsInFirestore || existsInRoom)
            }
        }
    }

    // Fetch all recipes for a specific user (Firestore or local source)
    fun getRecipesByUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recipes = repository.getRecipesByUser(userId)
                // Switch to main thread to update LiveData
                withContext(Dispatchers.Main) {
                    _recipesByUser.value = recipes
                }
            } catch (e: Exception) {
                // Handle the error appropriately, e.g., log or notify user
            }
        }
    }
}
