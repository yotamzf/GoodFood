package com.example.goodfoodapp.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentMyProfileBinding
import com.example.goodfoodapp.utils.CircleTransform
import com.example.goodfoodapp.utils.showLoadingOverlay
import com.example.goodfoodapp.utils.hideLoadingOverlay
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.example.goodfoodapp.models.User
import com.example.goodfoodapp.dal.repositories.UserRepository
import com.example.goodfoodapp.dal.room.AppDatabase
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.UnsavedChangesListener
import java.io.FileOutputStream
import java.io.InputStream

class MyProfileFragment : Fragment(), UnsavedChangesListener {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private var profileImageUri: Uri? = null
    private var originalProfileImageUri: Uri? = null  // Store original image URI
    private var originalUser: User? = null  // Store original user data to compare
    private var isEditingName = false  // Track if the name is editable
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val imgurApiService = GoodFoodApp.instance.imgurApiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Change status bar color to green as requested
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.green_background)

        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        val userDao = AppDatabase.getInstance(requireContext()).userDao()
        userRepository = UserRepository(userDao, FirebaseFirestore.getInstance())

        val user = auth.currentUser
        user?.let {
            // Show the loading spinner while fetching user data
            binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
            loadUserData(it.uid)
        }

        setupImagePicker()

        // Initially make nameEdit not editable
        binding.nameEdit.isEnabled = false
        disableButtons()

        // Toggle name editing
        binding.editNameIcon.setOnClickListener { toggleNameEdit() }
        binding.nameEdit.addTextChangedListener { checkForChanges() }

        binding.emailEdit.isEnabled = false

        // Save user data
        binding.btnSaveChanges.setOnClickListener {
            user?.let {
                // Show spinner while saving data
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
                saveUserData(it.uid)
            }
        }

        // Change profile picture
        binding.changePictureIcon.setOnClickListener { openImagePicker() }

        // Discard changes
        binding.btnDiscardChanges.setOnClickListener { user?.let { discardChanges(it.uid) } }
    }

    private fun toggleNameEdit() {
        isEditingName = !isEditingName
        binding.nameEdit.isEnabled = isEditingName
        checkForChanges()
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                profileImageUri = result.data?.data

                // Load selected image into profileImage view using Picasso
                Picasso.get()
                    .load(profileImageUri)
                    .placeholder(R.drawable.ic_default_user_profile)
                    .error(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)

                checkForChanges()  // Check for changes once the image is picked
            }
        }
    }

    private fun loadUserData(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load user from local cache (Room) first
                val cachedUser = userRepository.getUserById(uid)
                if (cachedUser != null) {
                    originalUser = cachedUser
                    updateUI(cachedUser)  // Update UI with cached data
                }

                // Then, attempt to load from Firestore
                val remoteUser = userRepository.getUserByIdFromFirestore(uid)
                if (remoteUser != null && remoteUser != cachedUser) {
                    originalUser = remoteUser
                    updateUI(remoteUser)  // Update UI with remote data if different
                    userRepository.insertUserLocally(remoteUser)  // Cache Firestore data locally
                }
            } catch (e: Exception) {
                // Handle any exceptions
                requireActivity().runOnUiThread {
                    showMessage("Failed to load user data: ${e.message}")
                }
            } finally {
                // Hide the loading spinner after data is loaded
                requireActivity().runOnUiThread {
                    binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
                }
            }
        }
    }

    private fun updateUI(user: User) {
        if (!isAdded) return  // Ensure fragment is still added
        requireActivity().runOnUiThread {
            if (_binding != null) {  // Check if binding is not null before using it
                binding.nameEdit.setText(user.name)
                binding.emailEdit.setText(user.email)

                // Load profile picture with Picasso and CircleTransform
                if (user.profilePic.isNotEmpty()) {
                    Picasso.get()
                        .load(user.profilePic)
                        .placeholder(R.drawable.ic_default_user_profile)
                        .error(R.drawable.ic_default_user_profile)
                        .transform(CircleTransform())
                        .into(binding.profileImage)

                    originalProfileImageUri = Uri.parse(user.profilePic)
                } else {
                    Picasso.get()
                        .load(R.drawable.ic_default_user_profile)
                        .transform(CircleTransform())
                        .into(binding.profileImage)
                }

                disableButtons()  // Disable buttons initially after loading data
            }
        }
    }

    private fun checkForChanges() {
        val updatedName = binding.nameEdit.text.toString()
        val currentImageUri = profileImageUri?.toString() ?: originalProfileImageUri?.toString()

        val hasNameChanged = originalUser?.name != updatedName
        val hasImageChanged = originalProfileImageUri?.toString() != currentImageUri

        if (hasNameChanged || hasImageChanged) {
            enableButtons()
        } else {
            disableButtons()
        }
    }

    private fun saveUserData(uid: String) {
        val updatedName = binding.nameEdit.text.toString().trim()

        if (profileImageUri != null) {
            // Save the image locally first
            val localImagePath = saveImageLocally(profileImageUri!!)

            // Get the file from the local path
            val file = File(localImagePath)

            // Upload the image to Imgur using the ImgurApiService accessed via GoodFoodApp
            imgurApiService.uploadImage(file, { imageUrl ->
                // On success, update the user object with the Imgur image link
                val user = originalUser?.copy(
                    name = updatedName,
                    profilePic = imageUrl  // Use the Imgur URL instead of local URI
                ) ?: return@uploadImage

                // Now save the user data to Firestore and Room
                updateUserData(user)

            }, { error ->
                // Handle upload error (e.g., show a message)
                requireActivity().runOnUiThread {
                    showMessage("Image upload failed: $error")
                }
            })
        } else {
            // No image was changed, just update the name
            val user = originalUser?.copy(
                name = updatedName,
                profilePic = originalUser?.profilePic ?: ""  // Keep the original picture if no change
            ) ?: return

            updateUserData(user)
        }
    }


    private fun saveImageLocally(uri: Uri): String {
        val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(uri)
        val file = File(requireContext().filesDir, "profile_pic.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }


    private fun updateUserData(user: User) {
        // Update Firestore and local Room database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userRepository.updateUser(user)
                requireActivity().runOnUiThread {
                    showMessage("Changes saved successfully.")
                    disableButtons()

                    // Reload the user data from the database to refresh the screen
                    loadUserData(user.userId)

                    // Automatically toggle name field back to non-editable
                    if (isEditingName) {
                        toggleNameEdit()  // Return name field to uneditable state
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    showMessage("Failed to save user data: ${e.message}")
                }
            } finally {
                // Hide the loading spinner after saving
                requireActivity().runOnUiThread {
                    binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
                }
            }
        }
    }

    private fun discardChanges(uid: String) {
        loadUserData(uid)
        showMessage("Changes discarded.")
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun disableButtons() {
        binding.btnSaveChanges.isEnabled = false
        binding.btnDiscardChanges.isEnabled = false
        binding.btnSaveChanges.alpha = 0.5f
        binding.btnDiscardChanges.alpha = 0.5f
    }

    private fun enableButtons() {
        binding.btnSaveChanges.isEnabled = true
        binding.btnDiscardChanges.isEnabled = true
        binding.btnSaveChanges.alpha = 1.0f
        binding.btnDiscardChanges.alpha = 1.0f
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun hasUnsavedChanges(): Boolean {
        val updatedName = binding.nameEdit.text.toString()
        val currentImageUri = profileImageUri?.toString() ?: originalProfileImageUri?.toString()
        return (originalUser?.name != updatedName) || (originalProfileImageUri?.toString() != currentImageUri)
    }

    override fun showUnsavedChangesDialog(onDiscardChanges: () -> Unit) {
        // Implement the dialog to warn users about unsaved changes
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage("You have unsaved changes. Discard them?")
            .setPositiveButton("Discard") { _, _ -> onDiscardChanges() }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
