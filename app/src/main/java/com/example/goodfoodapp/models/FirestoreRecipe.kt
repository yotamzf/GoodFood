package com.example.goodfoodapp.models

import com.google.firebase.Timestamp

data class FirestoreRecipe(
    val recipeId: String = "",
    val title: String = "",
    var picture: String = "",  // Recipe picture URL
    val content: String = "",
    val uploadDate: Timestamp? = null, // Firestore Timestamp
    val userId: String = ""
) {
    // Convert FirestoreRecipe back to Room-compatible Recipe
    fun toRecipe(): Recipe {
        return Recipe(
            recipeId = recipeId,
            title = title,
            picture = picture,
            content = content,
            uploadDate = uploadDate?.toDate()?.time ?: 0L,  // Convert Timestamp to Long
            userId = userId
        )
    }
}
