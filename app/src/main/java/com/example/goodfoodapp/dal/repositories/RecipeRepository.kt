package com.example.goodfoodapp.dal.repositories

import com.example.goodfoodapp.models.FirestoreRecipe
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.dal.room.dao.RecipeDao
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

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

            // Delete from Firestore using the recipe ID (Ensure the document reference is correct)
            val documentRef = db.collection("recipes").document(recipe.recipeId)
            documentRef.delete().await()
        }
    }

    // Get all recipes by a specific user, prefer fetching from Firebase, fallback to Room
    suspend fun getRecipesByUser(userId: String): List<Recipe> {
        return try {
            // Fetch recipes from Firestore
            val querySnapshot = db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Convert Firestore documents to FirestoreRecipe objects, then to Recipe objects
            val recipes = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreRecipe::class.java)?.toRecipe()
            }

            // Cache recipes locally in Room
            recipes.forEach { insertRecipeLocally(it) }

            recipes // Return the list from Firestore
        } catch (e: Exception) {
            // If Firestore fails, fallback to fetching from Room
            withContext(Dispatchers.IO) {
                recipeDao.getRecipesByUser(userId)
            }
        }
    }

    // Insert or update recipe in both Room and Firestore
    suspend fun insertRecipe(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            // Insert recipe into the local Room database
            recipeDao.insertRecipe(recipe)

            // Convert Long to Timestamp before inserting into Firestore
            val firestoreRecipe = FirestoreRecipe(
                recipeId = recipe.recipeId,
                title = recipe.title,
                picture = recipe.picture,
                content = recipe.content,
                uploadDate = Timestamp(Date(recipe.uploadDate)),  // Convert Long to Timestamp
                userId = recipe.userId
            )
            db.collection("recipes").document(recipe.recipeId).set(firestoreRecipe).await()
        }
    }
}
