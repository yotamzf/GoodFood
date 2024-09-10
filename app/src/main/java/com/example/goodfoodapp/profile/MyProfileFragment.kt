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
    private var originalUser: User? = null  // Store original user data to compare
    private var isEditingName = false
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.green_background)

        // Initialize Firebase Auth and UserRepository
        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository(
            AppDatabase.getInstance(requireContext()).userDao(),
            FirebaseFirestore.getInstance()
        )

        val user = auth.currentUser
        user?.let { loadUserData(it.uid) }

        // Setup image picker for changing profile picture
        setupImagePicker()

        // Initially disable editing and buttons
        binding.nameEdit.isEnabled = false
        disableButtons()

        binding.editNameIcon.setOnClickListener { toggleNameEdit() }
        binding.nameEdit.addTextChangedListener { checkForChanges() }
        binding.changePictureIcon.setOnClickListener { openImagePicker() }
        binding.btnSaveChanges.setOnClickListener { user?.let { saveUserData(it.uid) } }
        binding.btnDiscardChanges.setOnClickListener { discardChanges() }
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                profileImageUri = result.data?.data
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
        // Try to load data from Firestore first
        CoroutineScope(Dispatchers.IO).launch {
            val remoteUser = userRepository.getUserByIdFromFirestore(uid)
            if (remoteUser != null) {
                originalUser = remoteUser  // Store original user data for comparison
                updateUI(remoteUser)
                userRepository.insertUserLocally(remoteUser)  // Cache remotely fetched data locally
            } else {
                // If Firestore fails, load from local Room (local cache)
                val cachedUser = userRepository.getUserById(uid)
                cachedUser?.let {
                    originalUser = it  // Store original user data for comparison
                    updateUI(it)
                }
            }
        }
    }

    private fun updateUI(user: User) {
        requireActivity().runOnUiThread {
            binding.nameEdit.setText(user.name)
            binding.emailEdit.setText(user.email)

            // Check if the profilePic path is not empty and the file exists
            val profilePicFile = File(user.profilePic)
            if (profilePicFile.exists()) {
                // Load the profile picture from the file
                Picasso.get()
                    .load(profilePicFile)
                    .placeholder(R.drawable.ic_default_user_profile)  // Placeholder in case loading takes time
                    .error(R.drawable.ic_default_user_profile)        // Error image if loading fails
                    .transform(CircleTransform())                    // Apply circular transformation
                    .into(binding.profileImage)
            } else {
                // If the profile picture file doesn't exist, load the default image
                Picasso.get()
                    .load(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)
            }

            originalProfileImageUri = Uri.parse(user.profilePic) // Store original profile pic URI for comparison
            disableButtons()  // Disable buttons initially after loading data
        }
    }

    private fun toggleNameEdit() {
        isEditingName = !isEditingName
        binding.nameEdit.isEnabled = isEditingName
        checkForChanges()
    }

    // Check for changes in both name and profile picture separately
    private fun checkForChanges() {
        if (hasNameChanged() || hasProfilePictureChanged()) {
            enableButtons()
        } else {
            disableButtons()
        }
    }

    // Check if the profile picture was changed
    private fun hasProfilePictureChanged(): Boolean {
        val currentImageUri = profileImageUri?.toString() ?: originalProfileImageUri?.toString()
        return originalProfileImageUri?.toString() != currentImageUri
    }

    // Check if the name was changed
    private fun hasNameChanged(): Boolean {
        val updatedName = binding.nameEdit.text.toString().trim()
        return originalUser?.name != updatedName
    }

    private fun saveUserData(uid: String) {
        val updatedName = binding.nameEdit.text.toString().trim()
        profileImageUri?.let { uri ->
            userRepository.cacheImageLocally(requireContext(), uri) { localImagePath ->
                val updatedUser = User(
                    uid, binding.emailEdit.text.toString(), updatedName, localImagePath, originalUser?.signupDate ?: System.currentTimeMillis()
                )
                saveUserToDbAndFirestore(updatedUser)
            }
        } ?: run {
            val updatedUser = User(
                uid, binding.emailEdit.text.toString(), updatedName,
                originalProfileImageUri?.toString() ?: "", originalUser?.signupDate ?: System.currentTimeMillis()
            )
            saveUserToDbAndFirestore(updatedUser)
        }
    }

    private fun saveUserToDbAndFirestore(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.updateUser(user, {
                originalUser = user
                requireActivity().runOnUiThread {
                    showMessage("Changes saved successfully.")
                    disableButtons()
                }
            }, {
                requireActivity().runOnUiThread {
                    showMessage("Failed to save changes.")
                }
            })
        }
    }

    private fun discardChanges() {
        // Revert profile picture if it was changed
        if (hasProfilePictureChanged()) {
            profileImageUri = null  // Reset the selected image

            Picasso.get()
                .load(originalProfileImageUri)
                .placeholder(R.drawable.ic_default_user_profile)
                .error(R.drawable.ic_default_user_profile)
                .transform(CircleTransform())
                .into(binding.profileImage)
        }

        // Revert the name if it was changed
        if (hasNameChanged()) {
            binding.nameEdit.setText(originalUser?.name)
        }

        disableButtons()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
