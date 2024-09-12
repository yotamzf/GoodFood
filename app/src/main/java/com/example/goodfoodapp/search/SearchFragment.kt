package com.example.goodfoodapp.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.dal.room.dao.RecipeDao
import com.example.goodfoodapp.databinding.FragmentSearchBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.my_recipes.RecipesAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var recipesAdapter: RecipesAdapter
    private lateinit var recipeRepository: RecipeRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var recipeDao: RecipeDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        recipeDao =  AppDatabase.getInstance(requireContext()).recipeDao()
        // Initialize RecipeRepository with Room and Firestore
        recipeRepository = RecipeRepository(
            recipeDao = recipeDao,
            db = FirebaseFirestore.getInstance()
        )

        // Setup RecyclerView
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recipesAdapter = RecipesAdapter(
            showEditAndDeleteButtons = false,
            showAuthor = true,
            onDeleteClick = { recipe ->
                // Handle delete click
            },
            onEditClick = { recipe ->
                // Handle edit click
            }
        )
        binding.rvSearchResults.adapter = recipesAdapter


        // Initialize search input and button
        val searchEditText = view.findViewById<EditText>(R.id.etSearch)
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearch)

        // Load all recipes initially
        fetchAllRecipes()

        // Set search button click listener
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            searchAllRecipes(query)
        }

        // Add a text change listener to handle "on type" search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                searchAllRecipes(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed before text changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed during the text change
            }
        })

    }

    private fun fetchAllRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            val recipes = recipeRepository.getAllRecipesWithUserDetails()
            recipesAdapter.submitList(recipes)
        }
    }

    private fun searchAllRecipes(query: String) {
        // Use lifecycleScope to run a coroutine
        lifecycleScope.launch {
            try {
                // Perform the search in the Room database
                val localRecipes = withContext(Dispatchers.IO) {
                    recipeRepository.searchRecipesWithUserDetails(query) // Use the new search function
                }
                // Submit the search results to the adapter
                recipesAdapter.submitList(localRecipes)
            } catch (e: Exception) {
                // Handle exceptions, if necessary
                e.printStackTrace()
            }
        }
    }

}
