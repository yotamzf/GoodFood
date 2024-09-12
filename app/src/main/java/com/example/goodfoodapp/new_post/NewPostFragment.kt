package com.example.goodfoodapp.new_post

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
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
import com.example.goodfoodapp.R
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.dal.services.ImgurApiService
import com.example.goodfoodapp.databinding.FragmentNewPostBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.utils.Validator
import com.example.goodfoodapp.viewmodels.RecipeViewModel
import com.example.goodfoodapp.viewmodels.RecipeViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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
    private val validator = Validator()

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                binding.ivRecipeImage.setImageURI(uri)
                saveImageLocally(uri)  // Save the image locally
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

        // Set the Imgur API Service from the GoodFoodApp
        val imgurApiService = (activity?.application as GoodFoodApp).imgurApiService

        isEdit = args.isEditMode

        // Update the header title based on mode
        binding.tvHeader.text = getString(if (isEdit) R.string.edit_post else R.string.create_post)

        setupListeners()

        if (isEdit) {
            populateFields()
        }

        binding.btnShare.setOnClickListener {
            if (isValidRecipe()) {
                uploadImageToImgurAndSubmitRecipe(imgurApiService)  // Upload the image to Imgur before submitting the recipe
                hasSubmitted = true
            }
        }
    }

    private fun setupListeners() {
        binding.etTitle.setOnFocusChangeListener { _, _ -> hasChanged = true }
        binding.etContent.setOnFocusChangeListener { _, _ -> hasChanged = true }

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

                if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText(recipe.title)
                if (binding.etContent.text.isNullOrEmpty()) binding.etContent.setText(recipe.content)

                // Load the image using Picasso
                if (!recipe.picture.isNullOrEmpty()) {
                    Picasso.get().load(recipe.picture).into(binding.ivRecipeImage, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {}
                        override fun onError(e: Exception?) {
                            loadImageFromLocalStorage(recipe.recipeId)
                        }
                    })
                } else {
                    binding.ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
                }
            }
        }

        recipeViewModel.getRecipeById(args.recipeId)
    }

    private fun isValidRecipe(): Boolean {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()
        return validator.validateRecipeTitle(title) && validator.validateRecipeContent(content)
    }

    private fun uploadImageToImgurAndSubmitRecipe(imgurApiService: ImgurApiService) {
        val imageFile = File(localImagePath)
        imgurApiService.uploadImage(imageFile, { imageUrl ->
            submitRecipe(imageUrl)
        }, { errorMessage ->
            Snackbar.make(binding.root, "Failed to upload image: $errorMessage", Snackbar.LENGTH_LONG).show()
        })
    }

    private fun submitRecipe(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        val recipeId = if (isEdit) args.recipeId else UUID.randomUUID().toString()

        recipeViewModel.checkIfRecipeIdExists(recipeId) { exists ->
            if (!exists || isEdit) {
                // Save the direct image URL
                val recipe = Recipe(
                    recipeId = recipeId,
                    title = title,
                    content = content,
                    picture = imageUrl,  // Save the direct image URL
                    uploadDate = System.currentTimeMillis(),
                    userId = userId
                )
                recipeViewModel.insertRecipe(recipe)
                Snackbar.make(binding.root, "Recipe successfully saved!", Snackbar.LENGTH_LONG).show()
                findNavController().navigate(NewPostFragmentDirections.actionNewPostFragmentToMyRecipesFragment())
            } else {
                showErrorDialog("Recipe with this ID already exists.")
            }
        }
    }

    private fun saveImageLocally(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        val file = File(requireContext().filesDir, "${UUID.randomUUID()}.jpg")
        try {
            val outputStream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            localImagePath = file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadImageFromLocalStorage(recipeId: String) {
        val file = File(requireContext().filesDir, "$recipeId.jpg")
        if (file.exists()) {
            Picasso.get().load(file).into(binding.ivRecipeImage)
        } else {
            binding.ivRecipeImage.setImageResource(R.drawable.ic_recipe_placeholder)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
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
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                Snackbar.make(binding.root, "Changes discarded", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton("No") { _, _ -> findNavController().navigateUp() }
            .create()
            .show()
    }
}
