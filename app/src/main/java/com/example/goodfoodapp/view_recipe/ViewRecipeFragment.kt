package com.example.goodfoodapp.view_recipe

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentViewRecipeBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.viewmodels.RecipeViewModel
import com.example.goodfoodapp.viewmodels.UserViewModel
import com.squareup.picasso.Picasso

class ViewRecipeFragment : Fragment() {

    private var _binding: FragmentViewRecipeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recipeViewModel: RecipeViewModel
    private lateinit var userViewModel: UserViewModel


    // Get arguments passed via SafeArgs
    private val args: ViewRecipeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.green_background)

        // Initialize ViewModel
        recipeViewModel = ViewModelProvider(this)[RecipeViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // Get the recipe ID passed as SafeArg
        val recipeId = args.recipeId

        // Attempt to load recipe from local storage, otherwise fallback to Firestore
        loadRecipeData(recipeId)
    }

    // Function to load recipe data
    private fun loadRecipeData(recipeId: String) {
        // Observe the recipe data
        recipeViewModel.getRecipeById(recipeId)
        recipeViewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
                // Display recipe data
                try {
                    displayRecipeData(recipe)
                } catch(e: Exception) {
                    Log.d("ViewRecipe", "displayRecipeData")
                }
                // Fetch the user data once recipe data is available
                try {
                    loadUserData(recipe.userId)
                } catch(e: Exception) {
                    Log.d("ViewRecipe", "loadUserData")
                }
            }
        }
    }

    // Function to load user data
    private fun loadUserData(userId: String) {
        userViewModel.getUserById(userId)
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Display user name
                binding.tvUserName.text = user.name

                // Check if the profile picture URL is not empty or null
                if (!user.profilePic.isNullOrEmpty()) {
                    Picasso.get().load(user.profilePic)
                        .placeholder(R.drawable.ic_default_user_profile)
                        .into(binding.ivUserPicture)
                } else {
                    // Load a default profile picture if the URL is empty or null
                    binding.ivUserPicture.setImageResource(R.drawable.ic_default_user_profile)
                }
            }
        }
    }

    // Function to display the recipe data
    private fun displayRecipeData(recipe: Recipe) {
        binding.tvHeader.text = recipe.title
        binding.tvPublishDate.text = convertTimestampToDate(recipe.uploadDate)
        binding.tvContent.text = recipe.content

        // Check if the recipe picture URL is not empty or null
        if (!recipe.picture.isNullOrEmpty()) {
            Picasso.get()
                .load(recipe.picture)
                .placeholder(R.drawable.ic_recipe_placeholder)
                .into(binding.ivRecipeImage)
        } else {
            // Load a default recipe image if the URL is empty or null
            binding.ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
        }
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
