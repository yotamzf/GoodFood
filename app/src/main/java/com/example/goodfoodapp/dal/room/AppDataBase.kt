package com.example.goodfoodapp.dal.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.models.Recipe
import com.example.myapplication.room.dao.UserDao
import com.example.myapplication.room.dao.RecipeDao

@Database(entities = [User::class, Recipe::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao
}
