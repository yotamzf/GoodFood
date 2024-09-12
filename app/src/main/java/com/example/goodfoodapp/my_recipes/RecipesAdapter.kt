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
import com.example.goodfoodapp.models.RecipeWithUser
import com.squareup.picasso.Picasso

class RecipesAdapter(
    private val showEditAndDeleteButtons: Boolean,
    private val showAuthor: Boolean,
    private val onDeleteClick: (RecipeWithUser) -> Unit,
    private val onEditClick: (RecipeWithUser) -> Unit
) : ListAdapter<RecipeWithUser, RecipesAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        holder.bind(recipe, showEditAndDeleteButtons, showAuthor, onDeleteClick, onEditClick)
    }

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            recipe: RecipeWithUser,
            showEditAndDeleteButtons: Boolean,
            showAuthor: Boolean,
            onDeleteClick: (RecipeWithUser) -> Unit,
            onEditClick: (RecipeWithUser) -> Unit
        ) {
            // Bind data to the views
            val recipeImage = itemView.findViewById<ImageView>(R.id.ivRecipeImage)
            val recipeTitle = itemView.findViewById<TextView>(R.id.tvRecipeTitle)
            val recipeAuthor = itemView.findViewById<TextView>(R.id.tvRecipeAuthor)
            val editButton = itemView.findViewById<ImageButton>(R.id.btnEdit)
            val deleteButton = itemView.findViewById<ImageButton>(R.id.btnDelete)

            // Use Picasso to load the image
            if (!recipe.picture.isNullOrEmpty()) {
                Picasso.get()
                    .load(recipe.picture)
                    .placeholder(R.drawable.ic_recipe_placeholder)
                    .error(R.drawable.ic_recipe_placeholder)
                    .into(recipeImage)
            } else {
                Picasso.get()
                    .load(R.drawable.ic_recipe_placeholder)
                    .into(recipeImage)
            }

            recipeTitle.text = recipe.title
            recipeAuthor.text = recipe.userName

            // Set visibility based on parameters
            editButton.visibility = if (showEditAndDeleteButtons) View.VISIBLE else View.GONE
            deleteButton.visibility = if (showEditAndDeleteButtons) View.VISIBLE else View.GONE
            recipeAuthor.visibility = if (showAuthor) View.VISIBLE else View.GONE

            editButton.setOnClickListener { onEditClick(recipe) }
            deleteButton.setOnClickListener { onDeleteClick(recipe) }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<RecipeWithUser>() {
        override fun areItemsTheSame(oldItem: RecipeWithUser, newItem: RecipeWithUser): Boolean {
            return oldItem.recipeId == newItem.recipeId
        }

        override fun areContentsTheSame(oldItem: RecipeWithUser, newItem: RecipeWithUser): Boolean {
            return oldItem == newItem
        }
    }
}
