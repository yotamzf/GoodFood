package com.example.goodfoodapp.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goodfoodapp.R
import com.example.goodfoodapp.dal.repositories.RecipeRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.example.goodfoodapp.databinding.FragmentSearchBinding
import com.example.goodfoodapp.models.RecipeWithUser
import com.example.goodfoodapp.my_recipes.RecipesAdapter
import com.example.goodfoodapp.utils.hideLoadingOverlay
import com.example.goodfoodapp.utils.showLoadingOverlay
import com.example.goodfoodapp.viewmodels.SearchViewModel
import com.example.goodfoodapp.viewmodels.SearchViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var recipesAdapter: RecipesAdapter
    private lateinit var viewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val recipeDao = AppDatabase.getInstance(requireContext()).recipeDao()
        val repository = RecipeRepository(recipeDao, FirebaseFirestore.getInstance())
        val factory = SearchViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SearchViewModel::class.java]

        // Observe ViewModel
        observeViewModel()

        // Show loading spinner initially
        binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()

        // Setup RecyclerView
        setupRecyclerView()

        // Initialize search input and button
        val searchEditText = view.findViewById<EditText>(R.id.etSearch)
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearch)

        // Handle Enter key press in the search EditText
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = searchEditText.text.toString()
                viewModel.performSearch(query)
                true // Consume the event
            } else {
                false // Do not consume the event
            }
        }

        // Set search button click listener
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            viewModel.performSearch(query)
        }

        // Add a text change listener to handle "on type" search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                viewModel.performSearch(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Load all recipes initially
        viewModel.fetchAllRecipes()
    }

    private fun setupRecyclerView() {
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recipesAdapter = RecipesAdapter(
            showEditAndDeleteButtons = false,
            showAuthor = true,
            onDeleteClick = { /* No delete in search */ },
            onEditClick = { /* No edit in search */ },
            onRecipeClick = { recipe -> viewRecipe(recipe) }
        )
        binding.rvSearchResults.adapter = recipesAdapter
    }

    private fun observeViewModel() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipesAdapter.submitList(recipes)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
            } else {
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
        }
    }

    private fun viewRecipe(recipe: RecipeWithUser) {
        val action = SearchFragmentDirections.actionSearchFragmentToViewRecipeFragment(recipe.recipeId)
        findNavController().navigate(action)
    }
}
