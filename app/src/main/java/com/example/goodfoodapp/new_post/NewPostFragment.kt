package com.example.goodfoodapp.new_post

import android.app.Activity
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.goodfoodapp.R
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.dal.services.ImgurApiService
import com.example.goodfoodapp.databinding.FragmentNewPostBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.utils.hideLoadingOverlay
import com.example.goodfoodapp.utils.showLoadingOverlay
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class NewPostFragment : Fragment() {

    private lateinit var binding: FragmentNewPostBinding
    private val newPostViewModel: NewPostViewModel by viewModels()
    private val args: NewPostFragmentArgs by navArgs()
    private lateinit var imgurApiService: ImgurApiService

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                binding.ivRecipeImage.setImageURI(it)
                saveImageLocally(it)
                newPostViewModel.setSelectedImageUri(it)
                newPostViewModel.setHasChanged(true)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNewPostBinding.inflate(inflater, container, false)
        newPostViewModel.binding = binding
        newPostViewModel.args = args
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.green_background)

        // Show the loading spinner on fragment creation
        binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()

        // Initialize Imgur API Service
        imgurApiService = (activity?.application as GoodFoodApp).imgurApiService

        newPostViewModel.init(args.isEditMode, args.recipeId)

        // Update the header title based on mode
        binding.tvHeader.text = getString(if (args.isEditMode) R.string.edit_post else R.string.create_post)

        setupListeners()
        observeViewModel()

        if (!args.isEditMode) {
            // Hide the spinner if it's a new post
            binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
        }

        binding.btnShare.setOnClickListener {
            if (isValidRecipe()) {
                // Show the spinner while uploading and submitting the recipe
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
                newPostViewModel.uploadImageToImgurAndSubmitRecipe(imgurApiService, {
                    // Success callback
                    Snackbar.make(binding.root, "Recipe successfully saved!", Snackbar.LENGTH_LONG).show()
                    findNavController().navigate(NewPostFragmentDirections.actionNewPostFragmentToMyRecipesFragment())
                }, { errorMessage ->
                    // Failure callback
                    Snackbar.make(binding.root, "Failed to upload image: $errorMessage", Snackbar.LENGTH_LONG).show()
                    binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
                })
                newPostViewModel.setHasSubmitted(true)
            }
        }
    }

    private fun setupListeners() {
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                newPostViewModel.setTitleChanged(s.toString())
            }
        })

        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                newPostViewModel.setContentChanged(s.toString())
            }
        })

        binding.ivRecipeImage.setOnClickListener {
            newPostViewModel.setHasChanged(true)
            openImagePicker()
        }
    }

    private fun observeViewModel() {
        newPostViewModel.originalRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
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

                // Hide the spinner once the data is fully loaded
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
        }

        newPostViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
                newPostViewModel.clearErrorMessage()
            }
        }

        newPostViewModel.hasSubmitted.observe(viewLifecycleOwner) { hasSubmitted ->
            if (hasSubmitted == true) {
                // Hide the spinner after submitting the recipe
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
        }
    }

    private fun isValidRecipe(): Boolean {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        var isValid = true
        val validator = newPostViewModel.validator

        if (!validator.validateRecipeTitle(title)) {
            binding.etTitle.error = "Title can't be empty"
            binding.etTitle.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            isValid = false
        } else {
            binding.etTitle.error = null
            binding.etTitle.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }

        if (!validator.validateRecipeContent(content)) {
            binding.etContent.error = "Content can't be empty"
            binding.etContent.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            isValid = false
        } else {
            binding.etContent.error = null
            binding.etContent.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }

        return isValid
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveImageLocally(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        val file = File(requireContext().filesDir, "${UUID.randomUUID()}.jpg")
        try {
            val outputStream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            newPostViewModel.setLocalImagePath(file.absolutePath)
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

    override fun onDestroyView() {
        super.onDestroyView()
        if (newPostViewModel.hasChanged.value == true && newPostViewModel.hasSubmitted.value != true) {
            showUnsavedChangesDialog()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("Are you sure you want to leave without saving the recipe?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val rootView = view ?: requireActivity().findViewById(android.R.id.content)

                newPostViewModel.setHasChanged(false)
                Snackbar.make(rootView, "Changes discarded", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton("No") { _, _ ->
                findNavController().navigateUp()
            }
            .create()
            .show()
    }
}
