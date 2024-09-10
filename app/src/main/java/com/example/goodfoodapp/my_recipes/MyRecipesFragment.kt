package com.example.goodfoodapp.my_recipes

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goodfoodapp.R
import com.example.goodfoodapp.my_recipes.RecipesAdapter
import com.example.goodfoodapp.databinding.FragmentMyRecipesBinding
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.models.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            onDeleteClick = { recipe -> deleteRecipe(recipe) },
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

    private fun deleteRecipe(recipe: Recipe) {
        CoroutineScope(Dispatchers.Main).launch {
            recipeRepository.deleteRecipe(recipe)  // Correctly call the repository method
            fetchUserRecipes()  // Refresh list after deletion
        }
    }
}
