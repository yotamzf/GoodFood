package com.example.goodfoodapp.view_recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import com.example.goodfoodapp.R

class ViewRecipeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_recipe, container, false)

        val titleTextView = view.findViewById<TextView>(R.id.tvTitle)
        val contentTextView = view.findViewById<TextView>(R.id.tvContent)

        // You might want to get data from arguments or view model and set it here
        // titleTextView.text = ...
        // contentTextView.text = ...

        return view
    }
}
