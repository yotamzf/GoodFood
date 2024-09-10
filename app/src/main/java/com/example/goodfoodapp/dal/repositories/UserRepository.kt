package com.example.goodfoodapp.dal.repositories

import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.room.dao.UserDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao, private val db: FirebaseFirestore) {

    // Insert user locally into Room (for caching)
    suspend fun insertUserLocally(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insertUser(user)
        }
    }

    // Insert user into Firestore
    fun insertUser(user: User) {
        val userDoc = db.collection("users").document(user.userId)
        userDoc.set(user)
    }

    // Get user by ID from Room (local database)
    suspend fun getUserById(userId: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }

    // Get user by ID from Firestore (remote database)
    suspend fun getUserByIdFromFirestore(userId: String): User? {
        return try {
            val documentSnapshot = db.collection("users").document(userId).get().await()
            if (documentSnapshot.exists()) {
                documentSnapshot.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
