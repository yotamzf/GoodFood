package com.example.goodfoodapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Firebase Authentication and Repository initialized via GoodFoodApp
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UserRepository using the already initialized instance in GoodFoodApp
        val goodFoodApp = application as GoodFoodApp
        userRepository = goodFoodApp.userRepository

        // Set up Navigation
        setupNavigation()
        checkUserLoggedInStatus()
    }

    // Check if user is logged in and navigate to the appropriate fragment
    private fun checkUserLoggedInStatus() {
        val navController = findNavController()
        if (auth.currentUser != null) {
            // User is logged in, navigate to Profile
            navController.navigate(R.id.myProfileFragment)

            // Ensure BottomNavigationView shows "My Profile" as selected
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
            bottomNavigationView.selectedItemId = R.id.nav_my_profile
        } else {
            // User not logged in, navigate to Login
            navController.navigate(R.id.loginFragment)
        }
    }


    // Setup Bottom Navigation and Navigation Controller
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        bottomNavigationView.setupWithNavController(navController)

        // Hide BottomNavigationView for login and signup fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigationView.visibility = if (destination.id == R.id.loginFragment || destination.id == R.id.signUpFragment) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        // Override navigation and handle unsaved changes
        bottomNavigationView.setOnItemSelectedListener { item ->
            handleNavigation(item.itemId, navController)
            true
        }
    }

    private fun handleNavigation(itemId: Int, navController: NavController) {
        val currentFragment = supportFragmentManager.primaryNavigationFragment
        if (currentFragment is UnsavedChangesListener && currentFragment.hasUnsavedChanges()) {
            // Show unsaved changes dialog if needed
            currentFragment.showUnsavedChangesDialog {
                navigateToDestination(itemId, navController)
            }
        } else {
            // No unsaved changes, proceed with navigation
            navigateToDestination(itemId, navController)
        }
    }

    private fun navigateToDestination(itemId: Int, navController: NavController) {
        when (itemId) {
            R.id.nav_my_recipes -> navController.navigate(R.id.myRecipesFragment)
            R.id.nav_search -> navController.navigate(R.id.searchFragment)
            R.id.nav_new_post -> navController.navigate(R.id.newPostFragment)
            R.id.nav_my_profile -> navController.navigate(R.id.myProfileFragment)
            R.id.nav_log_out -> logOut(navController)
        }
    }

    private fun logOut(navController: NavController) {
        // Log out user from Firebase and clear user data from Room
        auth.signOut()
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.clearAllUsers() // Clear all user data
        }
        navController.navigate(R.id.loginFragment)

        // Hide BottomNavigationView after logout
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.visibility = View.GONE
    }


    private fun findNavController(): NavController {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }
}
