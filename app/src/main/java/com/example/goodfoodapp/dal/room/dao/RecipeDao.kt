package com.example.goodfoodapp.dal.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.models.RecipeWithUser

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE recipeId = :recipeId")
    suspend fun getRecipeById(recipeId: String): Recipe?

    @Query("""
    SELECT * FROM recipes 
    INNER JOIN users ON recipes.userId = users.userId 
    WHERE LOWER(recipes.title) LIKE '%' || LOWER(:query) || '%' 
    OR LOWER(users.name) LIKE '%' || LOWER(:query) || '%'
    """)
    suspend fun getRecipesByTitleOrAuthor(query: String): List<Recipe>


//    @Transaction
//    @Query("""
//        SELECT * FROM recipes
//        WHERE title LIKE '%' || :query || '%' OR userId IN (
//            SELECT userId FROM users WHERE name LIKE '%' || :query || '%'
//        )
//    """)
//    suspend fun searchRecipesByQuery(query: String): List<RecipeWithUser>


    @Query("""
        SELECT 
            recipes.recipeId AS recipeId,
            recipes.title AS title,
            recipes.picture AS picture,
            recipes.content AS content,
            recipes.uploadDate AS uploadDate,
            recipes.userId AS userId,
            users.name AS userName
        FROM recipes
        INNER JOIN users ON recipes.userId = users.userId
        WHERE recipes.title LIKE '%' || :query || '%' OR users.name LIKE '%' || :query || '%'
    """)
    suspend fun searchRecipesByQuery(query: String): List<RecipeWithUser>

//    @Query("""
//        SELECT recipes.recipeId, recipes.title, recipes.picture, recipes.content, recipes.uploadDate, users.userId, users.name as userName
//        FROM recipes
//        INNER JOIN users ON recipes.userId = users.userId
//    """)
//    suspend fun getAllRecipesWithUserDetails(): List<RecipeWithUser>

    @Query("SELECT * FROM recipes WHERE userId = :userId")
    suspend fun getRecipesByUser(userId: String): List<Recipe>

    @Query("DELETE FROM recipes WHERE recipeId = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<Recipe>
}