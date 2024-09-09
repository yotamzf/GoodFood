package com.example.goodfoodapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if the user is logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignUp()) // Replace with your HomeFragment or main screen
                .commit()
        } else {
            // If not logged in, navigate to the LoginFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}
