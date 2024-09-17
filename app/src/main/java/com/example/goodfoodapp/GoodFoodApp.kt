package com.example.goodfoodapp

import android.app.Application
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.dal.services.ImgurApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GoodFoodApp : Application() {

    // Singleton instance for global access
    companion object {
        lateinit var instance: GoodFoodApp
            private set
    }

    lateinit var recipeRepository: RecipeRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var imgurApiService: ImgurApiService
        private set

    lateinit var firestoreDb: FirebaseFirestore
        private set

    lateinit var firebaseAuth: FirebaseAuth
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firestore, Room database, and Imgur API service
        val database = AppDatabase.getInstance(this)
        firestoreDb = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        imgurApiService = ImgurApiService("29b9c21077c2383")

        // Initialize the repository shared across the app
        recipeRepository = RecipeRepository(database.recipeDao(), firestoreDb)
        userRepository = UserRepository(database.userDao(), firestoreDb)
    }
}
