package com.example.goodfoodapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val recipeId: String,
    val title: String,
    val picture: String,  // Recipe picture URL
    val content: String,
    val uploadDate: Long, // Timestamp in milliseconds
    val userId: String    // ID of the user who posted the recipe
)