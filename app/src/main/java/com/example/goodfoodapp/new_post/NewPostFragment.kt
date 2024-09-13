package com.example.goodfoodapp.new_post

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
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
import com.example.goodfoodapp.utils.showLoadingOverlay
import com.example.goodfoodapp.utils.hideLoadingOverlay
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

        // Show the loading spinner on fragment creation
        binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()

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
        } else {
            // Hide the spinner if it's a new post
            binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
        }

        binding.btnShare.setOnClickListener {
            if (isValidRecipe()) {
                // Show the spinner while uploading and submitting the recipe
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
                uploadImageToImgurAndSubmitRecipe(imgurApiService)
                hasSubmitted = true
            }
        }
    }

    private fun setupListeners() {
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEdit && ::originalRecipe.isInitialized) {
                    hasChanged = s.toString() != originalRecipe.title
                } else {
                    // When creating a new post, mark changes as soon as the user starts typing
                    hasChanged = !s.isNullOrBlank()
                }
            }
        })

        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEdit && ::originalRecipe.isInitialized) {
                    hasChanged = s.toString() != originalRecipe.content
                } else {
                    // Mark changes when creating a new post
                    hasChanged = !s.isNullOrBlank()
                }
            }
        })

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

                // Setup text watchers after recipe is initialized
                setupListeners()

                // Hide the spinner once the data is fully loaded
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
        }

        recipeViewModel.getRecipeById(args.recipeId)
    }

    private fun isValidRecipe(): Boolean {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        var isValid = true

        if (!validator.validateRecipeTitle(title)) {
            binding.etTitle.error = "Title can't be empty"
            binding.etTitle.setHintTextColor(resources.getColor(R.color.red)) // Change hint color to red
            isValid = false
        } else {
            binding.etTitle.error = null // Clear the error when valid
            binding.etTitle.setHintTextColor(resources.getColor(R.color.black)) // Reset to default color
        }

        if (!validator.validateRecipeContent(content)) {
            binding.etContent.error = "Content can't be empty"
            binding.etContent.setHintTextColor(resources.getColor(R.color.red)) // Change hint color to red
            isValid = false
        } else {
            binding.etContent.error = null // Clear the error when valid
            binding.etContent.setHintTextColor(resources.getColor(R.color.black)) // Reset to default color
        }

        return isValid
    }

    private fun uploadImageToImgurAndSubmitRecipe(imgurApiService: ImgurApiService) {
        if (selectedImageUri == null) {
            // No image selected, submit the recipe without an image
            submitRecipe("")
            return
        }

        val imageFile = File(localImagePath)
        imgurApiService.uploadImage(imageFile, { imageUrl ->
            requireActivity().runOnUiThread {
                submitRecipe(imageUrl)
            }
        }, { errorMessage ->
            requireActivity().runOnUiThread {
                Snackbar.make(binding.root, "Failed to upload image: $errorMessage", Snackbar.LENGTH_LONG).show()
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay() // Hide the spinner on failure
            }
        })
    }


    private fun submitRecipe(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        val recipeId = if (isEdit) args.recipeId else UUID.randomUUID().toString()

        recipeViewModel.checkIfRecipeIdExists(recipeId) { exists ->
            if (!exists || isEdit) {
                // If no new image was selected and it's edit mode, keep the original image URL
                val finalImageUrl = if (selectedImageUri == null && isEdit) {
                    originalRecipe.picture
                } else {
                    imageUrl // This could be the new image URL or empty string in case of new post
                }

                // Save the recipe with the final image URL
                val recipe = Recipe(
                    recipeId = recipeId,
                    title = title,
                    content = content,
                    picture = finalImageUrl,  // Use the original image if no new image was selected
                    uploadDate = System.currentTimeMillis(),
                    userId = userId
                )
                recipeViewModel.insertRecipe(recipe)
                Snackbar.make(binding.root, "Recipe successfully saved!", Snackbar.LENGTH_LONG).show()
                findNavController().navigate(NewPostFragmentDirections.actionNewPostFragmentToMyRecipesFragment())
            } else {
                showErrorDialog("Recipe with this ID already exists.")
            }
            // Hide the spinner after submitting the recipe
            binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
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
                hasChanged = false

                // Use the activity's view if the fragment's view is null
                val rootView = view ?: requireActivity().findViewById(android.R.id.content)
                Snackbar.make(rootView, "Changes discarded", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton("No") { _, _ ->
                // Keep the user on the same fragment
                findNavController().navigateUp()
            }
            .create()
            .show()
    }
}
