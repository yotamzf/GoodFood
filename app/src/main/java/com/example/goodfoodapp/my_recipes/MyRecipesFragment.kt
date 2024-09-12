package com.example.goodfoodapp.my_recipes

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentMyRecipesBinding
import com.example.goodfoodapp.models.Recipe
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MyRecipesFragment : Fragment() {

    private lateinit var binding: FragmentMyRecipesBinding
    private lateinit var recipesAdapter: RecipesAdapter
    private lateinit var auth: FirebaseAuth

    // Access the RecipeRepository from GoodFoodApp
    private val recipeRepository by lazy {
        (requireActivity().application as com.example.goodfoodapp.GoodFoodApp).recipeRepository
    }

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

        // Setup RecyclerView
        binding.rvRecipes.layoutManager = LinearLayoutManager(requireContext())
        recipesAdapter = RecipesAdapter(
            showEditAndDeleteButtons = true,
            showAuthor = false,
            onDeleteClick = { recipe -> showDeleteConfirmationDialog(recipe) },
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
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.myRecipesFragment) {
            val action = MyRecipesFragmentDirections.actionMyRecipesFragmentToNewPostFragment(
                recipeId = recipe.recipeId,
                recipeTitle = recipe.title,
                recipeContent = recipe.content,
                recipePicture = recipe.picture ?: "",
                isEditMode = true
            )
            navController.navigate(action)
        }
    }

    private fun showDeleteConfirmationDialog(recipe: Recipe) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Recipe")
        builder.setMessage("Are you sure you want to delete this recipe?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            deleteRecipe(recipe)
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun deleteRecipe(recipe: Recipe) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    recipeRepository.deleteRecipe(recipe)
                }
                fetchUserRecipes()
                showSnackbar("Post deleted")
            } catch (e: Exception) {
                showErrorDialog("Failed to delete recipe: ${e.message}")
            }
        }
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    inner class RecipesAdapter(
        private val showEditAndDeleteButtons: Boolean,
        private val showAuthor: Boolean,
        private val onDeleteClick: (Recipe) -> Unit,
        private val onEditClick: (Recipe) -> Unit
    ) : RecyclerView.Adapter<RecipesAdapter.RecipeViewHolder>() {

        private var recipes: List<Recipe> = listOf()

        fun submitList(recipeList: List<Recipe>) {
            recipes = recipeList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_recipe, parent, false)
            return RecipeViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            val recipe = recipes[position]
            holder.bind(recipe)
        }

        override fun getItemCount(): Int = recipes.size

        inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivRecipeImage = itemView.findViewById<ImageView>(R.id.ivRecipeImage)
            private val tvRecipeTitle = itemView.findViewById<TextView>(R.id.tvRecipeTitle)
            private val btnEdit = itemView.findViewById<AppCompatImageButton>(R.id.btnEdit)
            private val btnDelete = itemView.findViewById<AppCompatImageButton>(R.id.btnDelete)

            fun bind(recipe: Recipe) {
                tvRecipeTitle.text = recipe.title

                // Load image from Imgur first, fallback to local storage, and default icon if both fail
                if (!recipe.picture.isNullOrEmpty()) {
                    Picasso.get().load(recipe.picture).into(ivRecipeImage, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {}
                        override fun onError(e: Exception?) {
                            val file = File(requireContext().filesDir, "${recipe.recipeId}.jpg")
                            if (file.exists()) {
                                Picasso.get().load(file).into(ivRecipeImage)
                            } else {
                                ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
                            }
                        }
                    })
                } else {
                    ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
                }

                if (showEditAndDeleteButtons) {
                    btnEdit.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE
                    btnEdit.setOnClickListener { onEditClick(recipe) }
                    btnDelete.setOnClickListener { onDeleteClick(recipe) }
                } else {
                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.GONE
                }
            }
        }
    }
}
