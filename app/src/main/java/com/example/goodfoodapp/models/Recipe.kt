package com.example.goodfoodapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val recipeId: String,
    val title: String="",
    var picture: String="",  // Recipe picture URL
    val content: String="",
    val uploadDate: Long=0L, // Timestamp in milliseconds
    val userId: String=""    // ID of the user who posted the recipe
)
