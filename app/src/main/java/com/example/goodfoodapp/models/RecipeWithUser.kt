package com.example.goodfoodapp.models

data class RecipeWithUser(
    val recipeId: String,
    val title: String,
    val picture: String,
    val content: String,
    val uploadDate: Long,
    val userId: String,
    val userName: String
)
