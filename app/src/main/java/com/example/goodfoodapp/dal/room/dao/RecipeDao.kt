package com.example.goodfoodapp.dal.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.goodfoodapp.models.Recipe

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE recipeId = :recipeId")
    suspend fun getRecipeById(recipeId: String): Recipe?

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :title || '%'")
    suspend fun getRecipesByTitle(title: String): List<Recipe>

    @Query("""
        SELECT * FROM recipes 
        INNER JOIN users ON recipes.userId = users.userId 
        WHERE recipes.title LIKE '%' || :query || '%' OR users.name LIKE '%' || :query || '%'
    """)
    suspend fun getRecipesByTitleOrAuthor(query: String): List<Recipe>

    @Query("SELECT * FROM recipes WHERE userId = :userId")
    suspend fun getRecipesByUser(userId: String): List<Recipe>

    @Query("DELETE FROM recipes WHERE recipeId = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<Recipe>
}
