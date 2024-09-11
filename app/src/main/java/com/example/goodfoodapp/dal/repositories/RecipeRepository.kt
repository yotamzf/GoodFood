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

    // Corrected return type to Recipe?
    suspend fun getRecipeLocally(recipeId: String): Recipe? {
        return withContext(Dispatchers.IO) {
            recipeDao.getRecipeById(recipeId)  // Return the recipe from Room database
        }
    }

    // Insert recipe into Firestore and locally in Room database
    suspend fun insertRecipe(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            // Insert recipe into Room
            recipeDao.insertRecipe(recipe)

            // Insert recipe into Firestore
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

    // Get recipe by ID from Firestore, fallback to Room if necessary
    suspend fun getRecipeById(recipeId: String): Recipe? {
        return try {
            // Attempt to fetch recipe from Firestore
            val docSnapshot = db.collection("recipes").document(recipeId).get().await()
            val firestoreRecipe = docSnapshot.toObject(FirestoreRecipe::class.java)
            firestoreRecipe?.toRecipe()
        } catch (e: Exception) {
            // Fallback to Room if Firestore fails
            recipeDao.getRecipeById(recipeId)
        }
    }

    // Get all recipes by a specific user, prefer fetching from Firestore, fallback to Room
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

    // Delete recipe from both Firestore and Room
    suspend fun deleteRecipe(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            // Delete from Firestore
            db.collection("recipes").document(recipe.recipeId).delete().await()

            // Delete from local Room database
            recipeDao.deleteRecipeById(recipe.recipeId)
        }
    }

    // Check if recipe ID exists in Firestore
    suspend fun checkRecipeIdInFirestore(recipeId: String): Boolean {
        return try {
            val docSnapshot = db.collection("recipes").document(recipeId).get().await()
            docSnapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Check if recipe ID exists in Room
    suspend fun checkRecipeIdInRoom(recipeId: String): Boolean {
        return recipeDao.getRecipeById(recipeId) != null
    }
}
