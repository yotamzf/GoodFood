package com.example.goodfoodapp.dal.repositories

import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.dal.room.dao.RecipeDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecipeRepository(private val recipeDao: RecipeDao, private val db: FirebaseFirestore) {

    suspend fun insertRecipeLocally(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            recipeDao.insertRecipe(recipe)
        }
    }

    suspend fun getRecipesByUser(userId: String): List<Recipe> {
        // Fetch from local cache
        var recipes = withContext(Dispatchers.IO) { recipeDao.getRecipesByUser(userId) }

        // Fetch from Firestore if not available locally
        if (recipes.isEmpty()) {
            val querySnapshot = db.collection("recipes").whereEqualTo("userId", userId).get().await()
            recipes = querySnapshot.toObjects(Recipe::class.java)
            recipes.forEach { insertRecipeLocally(it) }  // Cache recipes locally
        }

        return recipes
    }
}
