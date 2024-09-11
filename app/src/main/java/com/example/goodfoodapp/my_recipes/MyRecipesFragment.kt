package com.example.goodfoodapp.my_recipes

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goodfoodapp.databinding.FragmentMyRecipesBinding
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.models.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyRecipesFragment : Fragment() {

    private lateinit var binding: FragmentMyRecipesBinding
    private lateinit var recipesAdapter: RecipesAdapter
    private lateinit var recipeRepository: RecipeRepository
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Initialize RecipeRepository with Room and Firestore
        recipeRepository = RecipeRepository(
            recipeDao = AppDatabase.getInstance(requireContext()).recipeDao(),
            db = FirebaseFirestore.getInstance()
        )

        // Setup RecyclerView
        binding.rvRecipes.layoutManager = LinearLayoutManager(requireContext())
        recipesAdapter = RecipesAdapter(
            showEditAndDeleteButtons = true,
            showAuthor = false,
            onDeleteClick = { recipe -> showDeleteConfirmationDialog(recipe) }, // Show confirmation dialog
            onEditClick = { recipe -> editRecipe(recipe) }
        )
        binding.rvRecipes.adapter = recipesAdapter

        // Fetch user's recipes
        fetchUserRecipes()
    }

    private fun fetchUserRecipes() {
        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val recipes = recipeRepository.getRecipesByUser(userId)
            recipesAdapter.submitList(recipes)
        }
    }

    private fun editRecipe(recipe: Recipe) {
        val action = MyRecipesFragmentDirections.actionMyRecipesFragmentToNewPostFragment(
            recipeId = recipe.recipeId,
            recipeTitle = recipe.title,
            recipeContent = recipe.content,
            recipePicture = recipe.picture
        )
        findNavController().navigate(action)
    }

    // Show confirmation dialog before deletion
    private fun showDeleteConfirmationDialog(recipe: Recipe) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("מחיקת מתכון")
        builder.setMessage("האם ברצונך למחוק את המתכון הזה?")
        builder.setPositiveButton("כן") { dialog, _ ->
            deleteRecipe(recipe) // Proceed with deletion if the user confirms
            dialog.dismiss()
        }
        builder.setNegativeButton("לא") { dialog, _ ->
            dialog.dismiss() // Cancel deletion if the user declines
        }
        builder.create().show()
    }

    private fun deleteRecipe(recipe: Recipe) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    recipeRepository.deleteRecipe(recipe)  // Correctly call the repository method
                }
                fetchUserRecipes()  // Refresh list after deletion
            } catch (e: Exception) {
                // Show error message in case of failure
                showErrorDialog("Failed to delete recipe: ${e.message}")
            }
        }
    }

    // Show an error dialog if deletion fails
    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}