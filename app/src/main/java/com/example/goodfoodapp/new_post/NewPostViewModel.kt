package com.example.goodfoodapp.new_post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.dal.services.ImgurApiService
import com.example.goodfoodapp.databinding.FragmentNewPostBinding
import com.example.goodfoodapp.models.Recipe
import com.example.goodfoodapp.utils.Validator
import com.example.goodfoodapp.viewmodels.RecipeViewModel
import java.io.File
import java.util.*

class NewPostViewModel : ViewModel() {

    var binding: FragmentNewPostBinding? = null
    val recipeViewModel = RecipeViewModel()
    var args: NewPostFragmentArgs? = null

    private val _hasChanged = MutableLiveData<Boolean>(false)
    val hasChanged: LiveData<Boolean> get() = _hasChanged

    private val _isEdit = MutableLiveData<Boolean>(false)
    val isEdit: LiveData<Boolean> get() = _isEdit

    private val _hasSubmitted = MutableLiveData<Boolean>(false)
    val hasSubmitted: LiveData<Boolean> get() = _hasSubmitted

    private val _originalRecipe = MutableLiveData<Recipe?>()
    val originalRecipe: LiveData<Recipe?> get() = _originalRecipe

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> get() = _selectedImageUri

    private val _localImagePath = MutableLiveData<String>("")
    val localImagePath: LiveData<String> get() = _localImagePath

    val validator = Validator()

    // For error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun init(isEditMode: Boolean, recipeId: String?) {
        _isEdit.value = isEditMode
        if (isEditMode && recipeId != null) {
            populateFields(recipeId)
        }
    }

    fun setHasChanged(value: Boolean) {
        _hasChanged.value = value
    }

    fun setHasSubmitted(value: Boolean) {
        _hasSubmitted.value = value
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setLocalImagePath(path: String) {
        _localImagePath.value = path
    }

    fun setTitleChanged(newTitle: String) {
        if (_isEdit.value == true && _originalRecipe.value != null) {
            _hasChanged.value = newTitle != _originalRecipe.value?.title
        } else {
            _hasChanged.value = newTitle.isNotBlank()
        }
    }

    fun setContentChanged(newContent: String) {
        if (_isEdit.value == true && _originalRecipe.value != null) {
            _hasChanged.value = newContent != _originalRecipe.value?.content
        } else {
            _hasChanged.value = newContent.isNotBlank()
        }
    }

    private fun populateFields(recipeId: String) {
        recipeViewModel.getRecipeById(recipeId)
        recipeViewModel.recipe.observeForever { recipe ->
            if (recipe != null) {
                _originalRecipe.value = recipe
            }
        }
    }

    fun isValidRecipe(title: String, content: String): Boolean {
        return validator.validateRecipeTitle(title) && validator.validateRecipeContent(content)
    }

    fun uploadImageToImgurAndSubmitRecipe(imgurApiService: ImgurApiService, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (_selectedImageUri.value == null) {
            // No image selected, submit the recipe without an image
            submitRecipe("", onSuccess, onFailure)
            return
        }

        val imageFile = File(_localImagePath.value ?: "")
        imgurApiService.uploadImage(imageFile, { imageUrl ->
            submitRecipe(imageUrl, onSuccess, onFailure)
        }, { errorMessage ->
            _errorMessage.postValue("Failed to upload image: $errorMessage")
            onFailure(errorMessage)
        })
    }

    private fun submitRecipe(imageUrl: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userId = GoodFoodApp.instance.firebaseAuth.currentUser?.uid ?: return
        val title = binding?.etTitle?.text.toString()
        val content = binding?.etContent?.text.toString()
        val recipeId = if (_isEdit.value == true) args?.recipeId ?: "" else UUID.randomUUID().toString()

        recipeViewModel.checkIfRecipeIdExists(recipeId) { exists ->
            if (!exists || _isEdit.value == true) {
                // If no new image was selected and it's edit mode, keep the original image URL
                val finalImageUrl = if (_selectedImageUri.value == null && _isEdit.value == true) {
                    _originalRecipe.value?.picture
                } else {
                    imageUrl
                }

                // Save the recipe with the final image URL
                val recipe = Recipe(
                    recipeId = recipeId,
                    title = title,
                    content = content,
                    picture = finalImageUrl ?: "",
                    uploadDate = System.currentTimeMillis(),
                    userId = userId
                )
                recipeViewModel.insertRecipe(recipe)
                _hasSubmitted.value = true
                onSuccess()
            } else {
                _errorMessage.postValue("Recipe with this ID already exists.")
                onFailure("Recipe with this ID already exists.")
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
