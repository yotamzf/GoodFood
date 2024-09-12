package com.example.goodfoodapp.dal.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.dal.room.dao.UserDao
import com.example.goodfoodapp.dal.room.dao.RecipeDao

@Database(entities = [User::class, Recipe::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goodfood_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
