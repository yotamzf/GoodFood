package com.example.goodfoodapp.dal.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.room.dao.UserDao
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

    // Cache image locally using Picasso
    fun cacheImageLocally(context: Context, uri: Uri, onSuccess: (String) -> Unit) {
        Picasso.get().load(uri).into(object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap?.let {
                    val file = File(context.cacheDir, "${System.currentTimeMillis()}.png")
                    try {
                        val fos = FileOutputStream(file)
                        it.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.close()
                        // Return the local path to the caller
                        onSuccess(file.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                // Handle failure case here
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                // Handle image preparation if needed
            }
        })
    }
}
