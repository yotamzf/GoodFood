package com.example.goodfoodapp.my_recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goodfoodapp.R
import com.example.goodfoodapp.models.Recipe
import com.squareup.picasso.Picasso

class RecipesAdapter(
    private val onDeleteClick: (Recipe) -> Unit,
    private val onEditClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipesAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        holder.bind(recipe, onDeleteClick, onEditClick)
    }

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(recipe: Recipe, onDeleteClick: (Recipe) -> Unit, onEditClick: (Recipe) -> Unit) {
            // Bind data to the views
            val recipeImage = itemView.findViewById<ImageView>(R.id.ivRecipeImage)
            val recipeTitle = itemView.findViewById<TextView>(R.id.tvRecipeTitle)
            val recipeAuthor = itemView.findViewById<TextView>(R.id.tvRecipeAuthor) // Add this line
            val editButton = itemView.findViewById<ImageButton>(R.id.btnEdit)
            val deleteButton = itemView.findViewById<ImageButton>(R.id.btnDelete)

            // Use Picasso to load the image
            if (!recipe.picture.isNullOrEmpty()) {
                Picasso.get()
                    .load(recipe.picture) // Load the image if the URL is valid
                    .placeholder(R.drawable.ic_recipe_placeholder) // Optional: placeholder while loading
                    .error(R.drawable.ic_recipe_placeholder) // Optional: image on loading error
                    .into(recipeImage)
            } else {
                // Load a placeholder or error image if the URL is empty or null
                Picasso.get()
                    .load(R.drawable.ic_recipe_placeholder) // Placeholder image
                    .into(recipeImage)
            }
            recipeTitle.text = recipe.title
            recipeAuthor.text = recipe.userId

            editButton.setOnClickListener { onEditClick(recipe) }
            deleteButton.setOnClickListener { onDeleteClick(recipe) }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.recipeId == newItem.recipeId
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}
