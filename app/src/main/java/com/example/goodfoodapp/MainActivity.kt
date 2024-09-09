package com.example.goodfoodapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the NavHostFragment and get the NavController from it
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Check if the user is logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            navController.navigate(R.id.signUpFragment)
        } else {
            navController.navigate(R.id.loginFragment)
        }
    }
}
