package com.example.goodfoodapp.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
//import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContextCompat
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
import com.example.goodfoodapp.search.SearchViewModel
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
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.green_background)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        // Observe ViewModel
        observeViewModel()

        // Setup RecyclerView
        setupRecyclerView()

        // Initialize search input and button
        val searchEditText = view.findViewById<EditText>(R.id.etSearch)
        val searchButton = view.findViewById<ImageButton>(R.id.btnSearch)

        // Set search button click listener
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            viewModel.performSearch(query)
        }

        // Performing search on each key stroke
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                viewModel.performSearch(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Performing search on "Enter"
        //        searchEditText.setOnEditorActionListener { _, actionId, event ->
        //            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
        //                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
        //                val query = searchEditText.text.toString()
        //                viewModel.performSearch(query)
        //                true
        //            } else {
        //                false
        //            }
        //        }

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

            // Show "No recipes found" message if the list is empty
            if (recipes.isEmpty()) {
                binding.tvNoRecipes.visibility = View.VISIBLE
                binding.rvSearchResults.visibility = View.GONE
            } else {
                binding.tvNoRecipes.visibility = View.GONE
                binding.rvSearchResults.visibility = View.VISIBLE
            }
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
