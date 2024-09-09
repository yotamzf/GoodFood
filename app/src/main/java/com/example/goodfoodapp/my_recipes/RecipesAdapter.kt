package com.example.goodfoodapp.my_recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goodfoodapp.R

class RecipesAdapter(
    private val recipes: List<Recipe>,
    private val onDeleteClick: (Recipe) -> Unit,
    private val onEditClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipesAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        val recipeTitle: TextView = itemView.findViewById(R.id.tvRecipeTitle)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
        val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.recipeTitle.text = recipe.title
        // Set image and listeners for delete and edit buttons
        holder.deleteButton.setOnClickListener { onDeleteClick(recipe) }
        holder.editButton.setOnClickListener { onEditClick(recipe) }
    }

    override fun getItemCount() = recipes.size
}
