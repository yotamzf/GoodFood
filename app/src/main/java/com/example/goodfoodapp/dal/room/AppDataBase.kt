package com.example.goodfoodapp.dal.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.goodfoodapp.dal.room.dao.UserDao
import com.example.goodfoodapp.models.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
