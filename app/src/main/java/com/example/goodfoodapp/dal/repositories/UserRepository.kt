package com.example.goodfoodapp.dal.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.dal.room.dao.UserDao
import com.example.goodfoodapp.dal.services.ImgurApiService
import com.example.goodfoodapp.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class UserRepository(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore
) {

    // Lazy initialization of ImgurApiService using the global instance from GoodFoodApp
    private val imgurApiService: ImgurApiService by lazy {
        (GoodFoodApp.instance as GoodFoodApp).imgurApiService
    }

    suspend fun getUserById(userId: String): User? {
        // Retrieve user from Room (local cache)
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }

    suspend fun getUserByIdFromFirestore(userId: String): User? {
        // Retrieve user from Firestore (remote)
        return withContext(Dispatchers.IO) {
            try {
                val documentSnapshot = firestore.collection("users").document(userId).get().await()
                documentSnapshot.toObject(User::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Upload image to Imgur and cache the image locally
    fun uploadAndCacheImage(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        imgurApiService.uploadImage(File(uri.path ?: ""), { imageUrl ->
            // After successful upload, cache image locally
            cacheImageLocally(context, Uri.parse(imageUrl), onSuccess)
        }, { error ->
            onError(error)
        })
    }

    // Cache the image locally using Picasso or any other mechanism
    private fun cacheImageLocally(context: Context, uri: Uri, onSuccess: (String) -> Unit) {
        // Implement caching logic (using Picasso or direct file operations)
        // For example, using Picasso to download and save locally:
        // Picasso.get().load(uri).into(Target...)
        // Once saved, call onSuccess(localImagePath)
    }

    // Insert or update user in the local Room database
    suspend fun insertUserLocally(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insertUser(user)
        }
    }

    // Update user in both Firestore (remote) and Room (local)
    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            try {
                // Update local cache
                userDao.insertUser(user)

                // Update remote Firestore
                firestore.collection("users").document(user.userId).set(user).await()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle update failure
            }
        }
    }

    // Clear all local users (for example, on logout)
    suspend fun clearAllUsers() {
        withContext(Dispatchers.IO) {
            userDao.clearAllUsers()
        }
    }

    suspend fun fetchAndSaveUsers() {
        withContext(Dispatchers.IO) {
            try {
                // Fetch all users from remote (Firestore)
                val users = getAllUsersFromRemote()

                // Save all users to Room database
                userDao.insertAllUsers(users)

                Log.d("SaveUsers", "All users have been fetched from remote and saved to Room.")
            } catch (e: Exception) {
                // Log the exception if any occurs
                Log.e("SaveUsersError", "Error fetching and saving users: ${e.message}", e)
            }
        }
    }

    suspend fun getAllUsersFromRemote(): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Reference to the 'users' collection in Firestore
                val userCollection = firestore.collection("users")

                // Fetch all user documents from the collection
                val snapshot = userCollection.get().await()

                // Convert the snapshot to a list of User objects
                snapshot.toObjects(User::class.java)
            } catch (e: Exception) {
                // Log the exception and return an empty list in case of failure
                Log.e("FirestoreError", "Error fetching users: ${e.message}", e)
                emptyList()
            }
        }
    }
}
