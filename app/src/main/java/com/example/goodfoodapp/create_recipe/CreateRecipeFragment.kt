package com.example.goodfoodapp.create_recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.EditText
import com.example.goodfoodapp.R

class CreateRecipeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_recipe, container, false)

        val titleEditText = view.findViewById<EditText>(R.id.etTitle)
        val contentEditText = view.findViewById<EditText>(R.id.etContent)
        val shareButton = view.findViewById<Button>(R.id.btnShare)

        shareButton.setOnClickListener {
            // Handle the share action
        }

        return view
    }
}
