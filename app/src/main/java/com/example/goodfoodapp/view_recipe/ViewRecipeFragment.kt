package com.example.goodfoodapp.view_recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentViewRecipeBinding
import com.example.goodfoodapp.models.FirestoreRecipe
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.utils.showLoadingOverlay
import com.example.goodfoodapp.utils.hideLoadingOverlay
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ViewRecipeFragment : Fragment() {

    private var _binding: FragmentViewRecipeBinding? = null
    private val binding get() = _binding!!

    private val args: ViewRecipeFragmentArgs by navArgs()  // SafeArgs to get the recipeId passed

    private val firestore by lazy { FirebaseFirestore.getInstance() }  // Firestore instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show the loading spinner and blur on the parent layout (loading_overlay)
        binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()

        // Get the recipe ID from the arguments
        val recipeId = args.recipeId

        // Fetch the recipe details
        fetchRecipeDetails(recipeId)
    }

    private fun fetchRecipeDetails(recipeId: String) {
        firestore.collection("recipes").document(recipeId).get()
            .addOnSuccessListener { documentSnapshot ->
                val recipe = documentSnapshot.toObject(FirestoreRecipe::class.java)

                if (recipe != null) {
                    // Bind recipe details to the views
                    displayRecipeDetails(recipe)

                    // Fetch user details using the userId from the recipe
                    fetchUserDetails(recipe.userId)
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors and hide the spinner and blur
                showError("Failed to fetch recipe details: ${exception.message}")
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
    }

    private fun fetchUserDetails(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)

                if (user != null) {
                    // Bind user details to the views
                    displayUserDetails(user)
                }

                // Hide the loading spinner and blur once all data is loaded
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
            .addOnFailureListener { exception ->
                // Handle any errors and hide the spinner and blur
                showError("Failed to fetch user details: ${exception.message}")
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
    }

    // Function to display recipe details in the UI
    private fun displayRecipeDetails(recipe: FirestoreRecipe) {
        binding.tvHeader.text = recipe.title
        // Convert the Firestore Timestamp to Long for display
        val uploadDateMillis = recipe.uploadDate?.toDate()?.time ?: 0L
        binding.tvPublishDate.text = convertTimestampToDate(uploadDateMillis)
        binding.tvContent.text = recipe.content

        // Check if the recipe picture is null or empty
        if (recipe.picture.isNotEmpty()) {
            // Load recipe image using Picasso if the URL is valid
            Picasso.get()
                .load(recipe.picture)
                .placeholder(R.drawable.ic_recipe_placeholder)
                .into(binding.ivRecipeImage)
        } else {
            // Set a default image if the recipe picture is empty or null
            binding.ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
        }
    }

    // Function to display user details in the UI
    private fun displayUserDetails(user: User) {
        binding.tvUserName.text = user.name

        // Check if the user profile picture is null or empty
        if (user.profilePic.isNotEmpty()) {
            // Load user profile picture using Picasso if the URL is valid
            Picasso.get()
                .load(user.profilePic)
                .placeholder(R.drawable.ic_default_user_profile)
                .into(binding.ivUserPicture)
        } else {
            // Set a default profile picture if the user picture is empty or null
            binding.ivUserPicture.setImageResource(R.drawable.ic_default_user_profile)
        }
    }

    // Helper function to convert timestamp (in milliseconds) to date format
    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Function to handle errors and display messages
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
