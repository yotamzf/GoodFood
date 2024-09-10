package com.example.goodfoodapp.models
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String = "",
    val email: String = "",
    val name: String = "",
    val profilePic: String = "",  // Profile picture URL
    val signupDate: Long = 0L     // Timestamp in milliseconds
)
