package com.example.goodfoodapp.dal.repositories

import android.util.Log
import com.example.goodfoodapp.models.FirestoreRecipe
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.dal.room.dao.RecipeDao
import com.example.goodfoodapp.models.RecipeWithUser
import com.example.goodfoodapp.models.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val db: FirebaseFirestore
) {

    private val FRESHNESS_THRESHOLD = 10 * 60 * 1000L // 10 minutes in milliseconds

    // Insert recipe locally in Room database
    suspend fun insertRecipeLocally(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            recipeDao.insertRecipe(recipe)
        }
    }

    // Get recipe locally from Room and check freshness
    suspend fun getRecipeLocally(recipeId: String): Recipe? {
        return withContext(Dispatchers.IO) {
            val recipe = recipeDao.getRecipeById(recipeId)
            if (recipe != null && !isDataStale(recipe.uploadDate)) {
                return@withContext recipe // Return cached recipe if it's still fresh
            } else {
                return@withContext null // Return null if the data is stale
            }
        }
    }

    // Insert recipe into Firestore and locally in Room database
    suspend fun insertRecipe(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            // Insert recipe into Room
            recipeDao.insertRecipe(recipe)

            // Insert recipe into Firestore with the direct image URL
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
        // First, try getting the recipe from the local database (Room)
        val cachedRecipe = getRecipeLocally(recipeId)
        if (cachedRecipe != null) {
            return cachedRecipe // Return cached recipe if fresh
        }

        // If no fresh data in Room, fetch from Firestore
        return try {
            val docSnapshot = db.collection("recipes").document(recipeId).get().await()
            val firestoreRecipe = docSnapshot.toObject(FirestoreRecipe::class.java)
            val recipe = firestoreRecipe?.toRecipe()

            // Cache the recipe locally in Room
            if (recipe != null) {
                insertRecipeLocally(recipe)
            }

            recipe // Return the recipe
        } catch (e: Exception) {
            recipeDao.getRecipeById(recipeId) // Return whatever is in Room as a fallback
        }
    }

    // Get all recipes, prefer fetching from Firestore, fallback to Room
    suspend fun getAllRecipes(): List<Recipe> {
        return try {
            // Fetch all recipes from Firestore
            val querySnapshot = db.collection("recipes")
                .get()
                .await()

            // Convert Firestore documents to FirestoreRecipe objects, then to Recipe objects
            val recipes = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreRecipe::class.java)?.toRecipe()
            }

            // Cache recipes locally in Room
            withContext(Dispatchers.IO) {
                recipes.forEach { insertRecipeLocally(it) }
            }

            recipes // Return the list of recipes from Firestore
        } catch (e: Exception) {
            // If Firestore fails, fallback to fetching from Room
            withContext(Dispatchers.IO) {
                recipeDao.getAllRecipes()
            }
        }
    }

    // Function to search recipes in Firestore
    suspend fun searchRecipesInRemoteStorage(query: String): List<Recipe> {
        return try {
            // Search for recipes in Firestore by title or author's name
            val querySnapshot = db.collection("recipes")
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + "\uf8ff")
                .get()
                .await()

            // Convert Firestore documents to FirestoreRecipe objects, then to Recipe objects
            val recipes = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreRecipe::class.java)?.toRecipe()
            }

            // Cache the found recipes locally in Room
            withContext(Dispatchers.IO) {
                recipes.forEach { insertRecipeLocally(it) }
            }

            recipes // Return the list of recipes from Firestore
        } catch (e: Exception) {
            // If Firestore fails, log the error
            Log.e("FirestoreError", "Failed to search recipes in Firestore: ${e.message}")
            emptyList() // Return empty list if the search fails
        }
    }

    // Get all recipes by a specific user, prefer fetching from Firestore, fallback to Room
    suspend fun getRecipesByUser(userId: String): List<Recipe> {
        // First, check the cache in Room
        val cachedRecipes = recipeDao.getRecipesByUser(userId)
        if (cachedRecipes.isNotEmpty() && cachedRecipes.all { !isDataStale(it.uploadDate) }) {
            return cachedRecipes // Return cached recipes if they're all fresh
        }

        // Otherwise, fetch fresh data from Firestore
        return try {
            val querySnapshot = db.collection("recipes")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Convert Firestore documents to Recipe objects
            val recipes = querySnapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreRecipe::class.java)?.toRecipe()
            }

            // Cache fetched recipes in Room
            recipes.forEach {
                insertRecipeLocally(it)
            }

            recipes
        } catch (e: Exception) {
            // Fallback to Room if Firestore fetch fails
            cachedRecipes
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

    // Get all recipes with user details
    suspend fun getAllRecipesWithUserDetails(): List<RecipeWithUser> {
        return withContext(Dispatchers.IO) {
            val recipeWithUserList = mutableListOf<RecipeWithUser>()
            val recipesList = mutableListOf<Recipe>()

            try {
                // Fetch all recipes from Firestore
                val recipeSnapshot = db.collection("recipes").get().await()
                val recipes = recipeSnapshot.toObjects(FirestoreRecipe::class.java)

                for (recipe in recipes) {
                    // For each recipe, fetch the corresponding user details
                    val userSnapshot = db.collection("users").document(recipe.userId).get().await()
                    val user = userSnapshot.toObject(User::class.java)

                    if (user != null) {
                        val recipeWithUser = RecipeWithUser(
                            recipeId = recipe.recipeId,
                            title = recipe.title,
                            picture = recipe.picture,
                            content = recipe.content,
                            uploadDate = recipe.uploadDate?.toDate()?.time ?: 0L,
                            userId = user.userId,
                            userName = user.name
                        )
                        recipeWithUserList.add(recipeWithUser)

                        val recipeTemp = Recipe(
                            recipeId = recipe.recipeId,
                            title = recipe.title,
                            picture = recipe.picture,
                            content = recipe.content,
                            uploadDate = recipe.uploadDate?.toDate()?.time ?: 0L,
                            userId = user.userId,
                        )

                        recipesList.add(recipeTemp)
                    }
                }

                // Save to Room database
                recipeDao.insertAll(recipesList)

            } catch (e: Exception) {
                // Handle exceptions and return an empty list in case of failure
            }

            recipeWithUserList
        }
    }

    suspend fun searchRecipesWithUserDetails(query: String): List<RecipeWithUser> {
        return withContext(Dispatchers.IO) {
            try {
                // Perform the search in the Room database
                recipeDao.searchRecipesByQuery(query)
            } catch (e: Exception) {
                // Handle exceptions and return an empty list in case of failure
                e.printStackTrace()
                emptyList()
            }
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

    // Helper function to check if data is stale
    private fun isDataStale(uploadDate: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - uploadDate > FRESHNESS_THRESHOLD
    }
}
