package com.example.goodfoodapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.dal.room.dao.UserDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Repository for handling local storage and Firestore
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UserRepository with Room and Firestore
        val db = AppDatabase.getInstance(applicationContext)
        val userDao: UserDao = db.userDao()
        val firestore = FirebaseFirestore.getInstance()
        userRepository = UserRepository(userDao, firestore)

        // Find the NavHostFragment and get the NavController from it
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Find the BottomNavigationView and set up with NavController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setupWithNavController(navController)

        // Ensure bottom navigation is hidden on login and sign-up fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.signUpFragment) {
                bottomNavigationView.visibility = View.GONE
            } else {
                bottomNavigationView.visibility = View.VISIBLE
            }
        }

        // Handle BottomNavigationView item selection (including Log Out)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_my_recipes -> {
                    navController.navigate(R.id.myRecipesFragment)  // Navigates to My Recipes
                    true
                }
                R.id.nav_search -> {
                    navController.navigate(R.id.searchFragment)  // Navigates to Search
                    true
                }
                R.id.nav_new_post -> {
                    navController.navigate(R.id.newPostFragment)  // Navigates to New Post
                    true
                }
                R.id.nav_my_profile -> {
                    navController.navigate(R.id.myProfileFragment)  // Navigates to My Profile
                    true
                }
                R.id.nav_log_out -> {
                    // Log out the user from Firebase
                    auth.signOut()

                    // Clear user data from Room
                    CoroutineScope(Dispatchers.IO).launch {
                        userDao.clearAllUsers() // Assuming you have a method to clear all user data in UserDao
                    }

                    // Navigate to login fragment after logging out
                    navController.navigate(R.id.loginFragment)
                    true
                }
                else -> false
            }
        }

        // Check if the user is logged in and navigate to the appropriate fragment
        if (auth.currentUser != null) {
            // If the user is already logged in, navigate to the Profile or Home Fragment
            navController.navigate(R.id.myProfileFragment)
            bottomNavigationView.selectedItemId = R.id.nav_my_profile
        } else {
            // If the user is not logged in, navigate to the Login Fragment
            navController.navigate(R.id.loginFragment)
        }
    }
}
