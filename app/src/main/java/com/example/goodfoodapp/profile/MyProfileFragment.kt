package com.example.goodfoodapp.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentMyProfileBinding
import com.example.goodfoodapp.utils.CircleTransform
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

class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private var profileImageUri: Uri? = null
    private var originalProfileImageUri: Uri? = null  // Store original image URI

    // Variable to track whether the name is editable
    private var isEditingName = false
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private var originalUser: User? = null  // Store original user data to compare

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Change color of top bar.
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.green_background)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UserRepository with both Firestore and Room
        val userDao = AppDatabase.getInstance(requireContext()).userDao()
        userRepository = UserRepository(userDao, FirebaseFirestore.getInstance())

        val user = auth.currentUser
        if (user != null) {
            loadUserData(user.uid)
        }

        // Initialize the image picker launcher
        setupImagePicker()

        // Initially, make nameEdit not editable
        binding.nameEdit.isEnabled = false

        // Initially disable save and discard buttons and change their opacity
        disableButtons()

        // Enable or disable name editing
        binding.editNameIcon.setOnClickListener {
            toggleNameEdit()
            checkForChanges()
        }

        // Track changes to name field
        binding.nameEdit.addTextChangedListener {
            checkForChanges()
        }

        // Save user data
        binding.btnSaveChanges.setOnClickListener {
            if (user != null) {
                saveUserData(user.uid)
            }
        }

        // Change profile picture
        binding.changePictureIcon.setOnClickListener {
            openImagePicker()
        }

        // Discard changes
        binding.btnDiscardChanges.setOnClickListener {
            discardChanges()
        }
    }

    private fun toggleNameEdit() {
        // Toggle the editing state
        isEditingName = !isEditingName

        // Set whether the EditText is enabled based on the current state
        binding.nameEdit.isEnabled = isEditingName
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
        // First, try to load from Room (local cache)
        CoroutineScope(Dispatchers.IO).launch {
            val cachedUser = userRepository.getUserById(uid)
            cachedUser?.let {
                originalUser = it // Store the original user data for comparison
                updateUI(it)
            }

            // Then, try to load from Firestore (remote)
            val remoteUser = userRepository.getUserByIdFromFirestore(uid)
            remoteUser?.let {
                originalUser = it  // Store the original user data for comparison
                updateUI(it)
                userRepository.insertUserLocally(it)  // Cache it locally
            }
        }
    }

    private fun updateUI(user: User) {
        requireActivity().runOnUiThread {
            binding.nameEdit.setText(user.name)
            binding.emailEdit.setText(user.email)

            // Load profile picture with Picasso
            if (user.profilePic.isNotEmpty()) {
                val imageFile = File(user.profilePic)
                if (imageFile.exists()) {
                    Picasso.get()
                        .load(imageFile)
                        .placeholder(R.drawable.ic_default_user_profile)
                        .error(R.drawable.ic_default_user_profile)
                        .transform(CircleTransform())
                        .into(binding.profileImage)

                    originalProfileImageUri = Uri.parse(user.profilePic)  // Store original image URI
                }
            } else {
                Picasso.get()
                    .load(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)
            }

            disableButtons()  // Disable buttons initially after loading data
        }
    }

    private fun checkForChanges() {
        val updatedName = binding.nameEdit.text.toString()
        val currentImageUri = profileImageUri?.toString() ?: originalProfileImageUri?.toString()

        // Enable buttons if either the name or profile picture has changed
        val hasNameChanged = originalUser?.name != updatedName
        val hasImageChanged = originalProfileImageUri?.toString() != currentImageUri

        if (hasNameChanged || hasImageChanged) {
            enableButtons()
        } else {
            disableButtons()
        }
    }

    private fun saveUserData(uid: String) {
        val updatedName = binding.nameEdit.text.toString()

        // Cache the profile image locally
        profileImageUri?.let { uri ->
            userRepository.cacheImageLocally(requireContext(), uri) { localImagePath ->
                // Save the user with the local image path
                val user = User(
                    userId = uid,
                    email = binding.emailEdit.text.toString(),
                    name = updatedName,
                    profilePic = localImagePath,  // Save the cached image path locally
                    signupDate = originalUser?.signupDate ?: System.currentTimeMillis()  // Keep the original sign-up date
                )

                // Update the user in Firestore
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        showMessage("Changes saved successfully.")
                    }
                    .addOnFailureListener {
                        showMessage("Failed to save changes.")
                    }

                // Save locally (Room)
                CoroutineScope(Dispatchers.IO).launch {
                    userRepository.insertUserLocally(user)
                }

                // Disable buttons after save
                disableButtons()
            }
        }
    }

    private fun discardChanges() {
        // Restore the original profile image URI or default image
        profileImageUri = null  // Clear the current selected image

        // Revert to original profile image or default if none exists
        originalProfileImageUri?.let {
            Picasso.get()
                .load(it)
                .placeholder(R.drawable.ic_default_user_profile)
                .error(R.drawable.ic_default_user_profile)
                .transform(CircleTransform())
                .into(binding.profileImage)
        } ?: run {
            // Set the default image if no original image exists
            Picasso.get()
                .load(R.drawable.ic_default_user_profile)
                .transform(CircleTransform())
                .into(binding.profileImage)
        }

        // Reset the name field and other changes
        binding.nameEdit.setText(originalUser?.name)
        disableButtons()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun disableButtons() {
        // Set the buttons as disabled and lower their opacity
        binding.btnSaveChanges.isEnabled = false
        binding.btnDiscardChanges.isEnabled = false
        binding.btnSaveChanges.alpha = 0.5f  // Lower opacity to show it's disabled
        binding.btnDiscardChanges.alpha = 0.5f  // Lower opacity to show it's disabled
    }

    private fun enableButtons() {
        // Set the buttons as enabled and restore full opacity
        binding.btnSaveChanges.isEnabled = true
        binding.btnDiscardChanges.isEnabled = true
        binding.btnSaveChanges.alpha = 1.0f  // Full opacity
        binding.btnDiscardChanges.alpha = 1.0f  // Full opacity
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}