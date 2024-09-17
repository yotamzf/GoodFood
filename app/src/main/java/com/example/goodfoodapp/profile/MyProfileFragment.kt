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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentMyProfileBinding
import com.example.goodfoodapp.utils.CircleTransform
import com.example.goodfoodapp.utils.showLoadingOverlay
import com.example.goodfoodapp.utils.hideLoadingOverlay
import com.squareup.picasso.Picasso
import com.example.goodfoodapp.models.User
import com.google.android.material.snackbar.Snackbar
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.UnsavedChangesListener
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MyProfileFragment : Fragment(), UnsavedChangesListener {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private val myProfileViewModel: MyProfileViewModel by viewModels()

    private lateinit var auth: FirebaseAuth
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var isEditingName = false

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

        // Initialize Firebase Auth
        auth = GoodFoodApp.instance.firebaseAuth

        val user = auth.currentUser
        user?.let {
            // Show the loading spinner while fetching user data
            binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
            myProfileViewModel.loadUserData(it.uid)
        }

        setupImagePicker()
        observeViewModel()

        // Initially make nameEdit not editable
        binding.nameEdit.isEnabled = false
        binding.emailEdit.isEnabled = false
        disableButtons()

        // Toggle name editing
        binding.editNameIcon.setOnClickListener { toggleNameEdit() }
        binding.nameEdit.addTextChangedListener { text ->
            myProfileViewModel.checkForChanges(text.toString())
        }

        // Save user data
        binding.btnSaveChanges.setOnClickListener {
            user?.let {
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
                disableButtons()
                saveUserData(it.uid)
                disableNameEdit()  // Disable name editing after saving
            }
        }

        // Discard changes
        binding.btnDiscardChanges.setOnClickListener {
            user?.let {
                discardChanges(it.uid)
                disableNameEdit()  // Disable name editing after discarding
            }
        }
    }

    private fun toggleNameEdit() {
        isEditingName = !isEditingName
        binding.nameEdit.isEnabled = isEditingName
    }

    private fun disableNameEdit() {
        isEditingName = false
        binding.nameEdit.isEnabled = false
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                uri?.let {
                    myProfileViewModel.setProfileImageUri(it)
                }
            }
        }
    }

    private fun observeViewModel() {
        myProfileViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                updateUI(it)
            }
            binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
        })

        myProfileViewModel.profileImageUri.observe(viewLifecycleOwner, Observer { uri ->
            uri?.let {
                Picasso.get()
                    .load(it)
                    .placeholder(R.drawable.ic_default_user_profile)
                    .error(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)
            }
        })

        myProfileViewModel.hasUnsavedChanges.observe(viewLifecycleOwner, Observer { hasChanges ->
            if (hasChanges == true) {
                enableButtons()
            } else {
                disableButtons()
            }
        })
    }

    private fun updateUI(user: User) {
        if (!isAdded) return  // Ensure fragment is still added
        requireActivity().runOnUiThread {
            if (_binding != null) {
                binding.nameEdit.setText(user.name)
                binding.emailEdit.setText(user.email)
                if (user.profilePic.isNotEmpty()) {
                    Picasso.get()
                        .load(user.profilePic)
                        .placeholder(R.drawable.ic_default_user_profile)
                        .error(R.drawable.ic_default_user_profile)
                        .transform(CircleTransform())
                        .into(binding.profileImage)
                } else {
                    Picasso.get()
                        .load(R.drawable.ic_default_user_profile)
                        .transform(CircleTransform())
                        .into(binding.profileImage)
                }
                disableButtons()
            }
        }
    }

    private fun saveUserData(uid: String) {
        val updatedName = binding.nameEdit.text.toString().trim()
        val profileImageUri = myProfileViewModel.profileImageUri.value

        if (profileImageUri != null) {
            val localImagePath = saveImageLocally(profileImageUri)
            myProfileViewModel.uploadImageAndSaveUserData(localImagePath, updatedName, myProfileViewModel.user.value)
        } else {
            val user = myProfileViewModel.user.value?.copy(
                name = updatedName,
                profilePic = myProfileViewModel.user.value?.profilePic ?: ""
            ) ?: return

            myProfileViewModel.updateUserData(user)
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

    private fun discardChanges(uid: String) {
        myProfileViewModel.loadUserData(uid)
        showMessage("Changes discarded.")
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
        return myProfileViewModel.hasUnsavedChanges.value ?: false
    }

    override fun showUnsavedChangesDialog(onDiscardChanges: () -> Unit) {
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
