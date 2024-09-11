package com.example.goodfoodapp

import android.app.Application
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore

class GoodFoodApp : Application() {

    lateinit var recipeRepository: RecipeRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firestore and Room database
        val database = AppDatabase.getInstance(this)
        val firestore = FirebaseFirestore.getInstance()

        // Initialize the repository that will be shared across the app
        recipeRepository = RecipeRepository(database.recipeDao(), firestore)
    }
}
