package com.example.goodfoodapp.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.goodfoodapp.R
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.my_recipes.RecipesAdapter
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var recipesAdapter: RecipesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter
        recipesAdapter = RecipesAdapter(
            onDeleteClick = { recipe ->
                // Handle delete click
            },
            onEditClick = { recipe ->
                // Handle edit click
            }
        )
        recyclerView.adapter = recipesAdapter

        // Initialize search input and button
        val searchEditText = view.findViewById<EditText>(R.id.etSearch)
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearch)

        // Load all recipes initially
        loadAllRecipes()

        // Set search button click listener
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            searchRecipes(query)
        }

        return view
    }

    private fun loadAllRecipes() {
        val recipeDao = AppDatabase.getInstance(requireContext()).recipeDao()

        // Use lifecycleScope to run a coroutine
        lifecycleScope.launch {
            val recipes = recipeDao.getAllRecipes()
            recipesAdapter.submitList(recipes)
        }
    }

    private fun searchRecipes(query: String) {
        val recipeDao = AppDatabase.getInstance(requireContext()).recipeDao()

        // Use lifecycleScope to run a coroutine
        lifecycleScope.launch {
            // Fetch recipes that match the search query by title or author's name
            val recipes = recipeDao.getRecipesByTitleOrAuthor(query)
            recipesAdapter.submitList(recipes)
        }
    }
}
