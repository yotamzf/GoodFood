package com.example.goodfoodapp.dal.repositories

import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.dal.room.dao.RecipeDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecipeRepository(private val recipeDao: RecipeDao, private val db: FirebaseFirestore) {

    // Insert recipe locally in Room database
    suspend fun insertRecipeLocally(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            recipeDao.insertRecipe(recipe)
        }
    }

    // Delete recipe from both Room and Firestore
    suspend fun deleteRecipe(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            // Delete from local Room database using the recipe ID
            recipeDao.deleteRecipeById(recipe.recipeId)

            // Delete from Firestore using the recipe ID
            db.collection("recipes").document(recipe.recipeId).delete().await()
        }
    }

    // Get all recipes by a specific user
    suspend fun getRecipesByUser(userId: String): List<Recipe> {
        // Fetch recipes from local Room database first
        var recipes = withContext(Dispatchers.IO) { recipeDao.getRecipesByUser(userId) }

        // If no recipes are found locally, fetch them from Firestore
        if (recipes.isEmpty()) {
            val querySnapshot = db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Convert Firestore documents to Recipe objects
            recipes = querySnapshot.toObjects(Recipe::class.java)

            // Cache recipes locally in Room
            recipes.forEach { insertRecipeLocally(it) }
        }

        return recipes
    }
}
