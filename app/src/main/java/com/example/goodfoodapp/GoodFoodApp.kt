package com.example.goodfoodapp

import android.app.Application
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.dal.services.ImgurApiService
import com.google.firebase.firestore.FirebaseFirestore

class GoodFoodApp : Application() {

    lateinit var recipeRepository: RecipeRepository
        private set

    // Expose ImgurApiService as well for global access
    lateinit var imgurApiService: ImgurApiService
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firestore, Room database, and Imgur API service
        val database = AppDatabase.getInstance(this)
        val firestore = FirebaseFirestore.getInstance()

        // Initialize Imgur API service with your client ID
        imgurApiService = ImgurApiService("29b9c21077c2383")

        // Initialize the repository that will be shared across the app, no need to pass imgurApiService here
        recipeRepository = RecipeRepository(database.recipeDao(), firestore)
    }
}
