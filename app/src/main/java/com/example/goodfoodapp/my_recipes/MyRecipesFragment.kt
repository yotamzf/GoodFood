package com.example.goodfoodapp.my_recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.goodfoodapp.R

class MyRecipesFragment : Fragment() {

    private lateinit var recipesAdapter: RecipesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_recipes, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvRecipes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Dummy data
        val recipes = listOf(
            Recipe(1, "Title 1", ""),
            Recipe(2, "Title 2", ""),
            Recipe(3, "Title 3", "")
        )

        recipesAdapter = RecipesAdapter(recipes, onDeleteClick = { recipe ->
            // Handle delete click
        }, onEditClick = { recipe ->
            // Handle edit click
        })

        recyclerView.adapter = recipesAdapter

        return view
    }
}
