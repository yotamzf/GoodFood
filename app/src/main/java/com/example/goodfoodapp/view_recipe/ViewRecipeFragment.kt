package com.example.goodfoodapp.view_recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentViewRecipeBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.viewmodels.RecipeViewModel
import com.example.goodfoodapp.viewmodels.RecipeViewModelFactory
import com.example.goodfoodapp.viewmodels.UserViewModel
import com.example.goodfoodapp.viewmodels.UserViewModelFactory
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ViewRecipeFragment : Fragment() {

    private var _binding: FragmentViewRecipeBinding? = null
    private val binding get() = _binding!!

    // Get arguments passed via SafeArgs
    private val args: ViewRecipeFragmentArgs by navArgs()

    // Initialize ViewModels for Recipe and User
    private val recipeViewModel: RecipeViewModel by viewModels {
        RecipeViewModelFactory((requireActivity().application as GoodFoodApp).recipeRepository)
    }
    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory((requireActivity().application as GoodFoodApp).userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the recipe ID passed as SafeArg
        val recipeId = args.recipeId

        // Attempt to load recipe from local storage, otherwise fallback to Firestore
        lifecycleScope.launch {
            loadRecipeData(recipeId)
        }
    }

    // Function to load recipe data
    private suspend fun loadRecipeData(recipeId: String) {
        // Try to get the recipe from local Room database first
        recipeViewModel.fetchRecipeLocally(recipeId)

        // Observe the recipe data
        recipeViewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            // If data is missing, incomplete, or blank, fetch from Firestore
            if (recipe == null || recipe.title.isBlank() || recipe.picture.isBlank()) {
                recipeViewModel.getRecipeById(recipeId)
            } else {
                // Display recipe data
                displayRecipeData(recipe)
            }
        }

        // Fetch user data for profile picture and name
        recipeViewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            recipe?.let {
                // Launch a coroutine to call the suspend function
                lifecycleScope.launch {
                    loadUserData(it.userId)
                }
            }
        }
    }

    // Function to load user data
    private suspend fun loadUserData(userId: String) {
        userViewModel.getUserById(userId)

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Display user name and profile picture using Picasso
                binding.tvUserName.text = user.name
                Picasso.get().load(user.profilePic).placeholder(R.drawable.ic_default_user_profile).into(binding.ivUserPicture)
            }
        }
    }

    // Function to display the recipe data
    private fun displayRecipeData(recipe: Recipe) {
        binding.tvHeader.text = recipe.title
        binding.tvPublishDate.text = convertTimestampToDate(recipe.uploadDate)
        binding.tvContent.text = recipe.content

        // Load recipe image using Picasso
        Picasso.get().load(recipe.picture).placeholder(R.drawable.ic_recipe_placeholder).into(binding.ivRecipeImage)
    }

    // Convert timestamp to date format (helper function)
    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
