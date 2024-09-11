package com.example.goodfoodapp.new_post

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentNewPostBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.utils.Validator
import com.example.goodfoodapp.viewmodels.RecipeViewModel
import com.example.goodfoodapp.viewmodels.RecipeViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.util.*

class NewPostFragment : Fragment() {

    private lateinit var binding: FragmentNewPostBinding
    private lateinit var recipeViewModel: RecipeViewModel
    private val args: NewPostFragmentArgs by navArgs()
    private var hasChanged = false
    private var isEdit = false
    private var hasSubmitted = false
    private lateinit var originalRecipe: Recipe
    private var selectedImageUri: Uri? = null
    private var localImagePath: String = ""
    private val validator = Validator()  // Initialize Validator

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                binding.ivRecipeImage.setImageURI(uri)
                localImagePath = uri.toString() // Save the URI as the local image path
                hasChanged = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val repository = (activity?.application as GoodFoodApp).recipeRepository
        val factory = RecipeViewModelFactory(repository)
        recipeViewModel = ViewModelProvider(this, factory)[RecipeViewModel::class.java]

        isEdit = args.isEditMode
        setupListeners()

        if (isEdit) {
            populateFields()
        }

        binding.btnShare.setOnClickListener {
            if (isValidRecipe()) {
                submitRecipe()
                hasSubmitted = true
            }
        }
    }

    private fun setupListeners() {
        // Listen for changes in the title and content fields
        binding.etTitle.setOnFocusChangeListener { _, _ -> hasChanged = true }
        binding.etContent.setOnFocusChangeListener { _, _ -> hasChanged = true }

        // Image selection listener
        binding.ivRecipeImage.setOnClickListener {
            hasChanged = true
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun populateFields() {
        recipeViewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
                originalRecipe = recipe

                // Only populate if fields are empty
                if (binding.etTitle.text.isNullOrEmpty()) {
                    binding.etTitle.setText(recipe.title)
                }
                if (binding.etContent.text.isNullOrEmpty()) {
                    binding.etContent.setText(recipe.content)
                }

                // Load image with Picasso, or show placeholder if no image
                if (!recipe.picture.isNullOrEmpty()) {
                    Picasso.get().load(recipe.picture)
                        .placeholder(R.drawable.ic_recipe_placeholder)
                        .into(binding.ivRecipeImage)
                } else {
                    binding.ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
                }
            }
        }

        recipeViewModel.getRecipeById(args.recipeId)
    }

    // Use the validator for validation
    private fun isValidRecipe(): Boolean {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        if (!validator.validateRecipeTitle(title) || !validator.validateRecipeContent(content)) {
            showValidationError()
            return false
        }
        return true
    }

    private fun showValidationError() {
        AlertDialog.Builder(requireContext())
            .setTitle("Validation Error")
            .setMessage("Please fill all required fields.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun submitRecipe() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        val recipeId = if (isEdit) args.recipeId else UUID.randomUUID().toString()

        recipeViewModel.checkIfRecipeIdExists(recipeId) { exists ->
            if (!exists || isEdit) {
                val recipe = Recipe(
                    recipeId = recipeId,
                    title = title,
                    content = content,
                    picture = localImagePath,  // Store the local image path or URI
                    uploadDate = System.currentTimeMillis(),
                    userId = userId
                )
                recipeViewModel.insertRecipe(recipe)
                findNavController().navigate(NewPostFragmentDirections.actionNewPostFragmentToMyRecipesFragment())
            } else {
                showErrorDialog("Recipe with this ID already exists.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!hasSubmitted && hasChanged) {
            showUnsavedChangesDialog()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("Are you sure you want to leave without saving the recipe?")
            .setPositiveButton("Yes") { _, _ -> findNavController().navigateUp() }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
