package com.example.goodfoodapp.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
import com.example.goodfoodapp.GoodFoodApp
import com.example.goodfoodapp.UnsavedChangesListener
import com.google.android.material.bottomnavigation.BottomNavigationView

class MyProfileFragment : Fragment(), UnsavedChangesListener {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var hasSubmitted = false
    private var hasChanged = false

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

        // Initialize Firebase Auth and ProfileViewModel
        auth = FirebaseAuth.getInstance()
        val userDao = AppDatabase.getInstance(requireContext()).userDao()
        val userRepository = UserRepository(userDao, FirebaseFirestore.getInstance())
        val imgurApiService = (activity?.application as GoodFoodApp).imgurApiService

        // Use the custom factory to create the ProfileViewModel
        val factory = ProfileViewModelFactory(requireContext(), userRepository, imgurApiService)
        profileViewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        observeViewModel()

        auth.currentUser?.let {
            profileViewModel.loadUserData(it.uid)
        }

        setupImagePicker()
        setupListeners()
    }

    private fun setupListeners() {
        binding.nameEdit.isEnabled = false
        disableButtons()

        binding.editNameIcon.setOnClickListener { toggleNameEdit() }
        binding.nameEdit.addTextChangedListener { checkForChanges() }
        binding.emailEdit.isEnabled = false

        binding.btnSaveChanges.setOnClickListener {
            auth.currentUser?.let { user ->
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
                profileViewModel.saveUserData(user.uid, binding.nameEdit.text.toString().trim(), profileViewModel.profileImageUri.value)
            }
        }

        binding.changePictureIcon.setOnClickListener { openImagePicker() }
        binding.btnDiscardChanges.setOnClickListener { auth.currentUser?.let { user -> profileViewModel.loadUserData(user.uid) } }
    }

    private fun observeViewModel() {
        profileViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            updateUI(user)
        })

        profileViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                binding.root.findViewById<View>(R.id.loading_overlay)?.showLoadingOverlay()
            } else {
                binding.root.findViewById<View>(R.id.loading_overlay)?.hideLoadingOverlay()
            }
        })

        profileViewModel.message.observe(viewLifecycleOwner, Observer { message ->
            showMessage(message)
        })

        profileViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            // Stop editing when data is successfully saved
            disableEditing()
            updateUI(user)
        })
    }

    private fun disableEditing() {
        binding.nameEdit.isEnabled = false
        disableButtons()
    }

    private fun toggleNameEdit() {
        binding.nameEdit.isEnabled = !binding.nameEdit.isEnabled
        checkForChanges()
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                profileViewModel.setProfileImageUri(uri)
                Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.ic_default_user_profile)
                    .error(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)
                checkForChanges()
            }
        }
    }

    private fun updateUI(user: User?) {
        user?.let {
            binding.nameEdit.setText(it.name)
            binding.emailEdit.setText(it.email)

            // Check if profilePic is not empty or null before loading with Picasso
            if (!it.profilePic.isNullOrEmpty()) {
                Picasso.get()
                    .load(it.profilePic)
                    .placeholder(R.drawable.ic_default_user_profile)
                    .error(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)
            } else {
                // Load a default profile image if the path is empty
                Picasso.get()
                    .load(R.drawable.ic_default_user_profile)
                    .transform(CircleTransform())
                    .into(binding.profileImage)
            }

            disableButtons()
        }
    }

    private fun checkForChanges() {
        val updatedName = binding.nameEdit.text.toString()
        val currentImageUri = profileViewModel.profileImageUri.value
        val hasChanges = profileViewModel.hasUnsavedChanges(updatedName, profileViewModel.user.value?.name, currentImageUri)

        if (hasChanges) {
            enableButtons()
        } else {
            disableButtons()
        }
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
        val currentImageUri = profileViewModel.profileImageUri.value
        return profileViewModel.hasUnsavedChanges(updatedName, profileViewModel.user.value?.name, currentImageUri)
    }

    override fun showUnsavedChangesDialog(onDiscardChanges: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("Are you sure you want to leave without saving your profile?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                hasChanged = false

                // Use the activity's view if the fragment's view is null
                val rootView = view ?: requireActivity().findViewById(android.R.id.content)
                Snackbar.make(rootView, "Changes discarded", Snackbar.LENGTH_LONG).show()

                // Navigate back to the previous screen
//                onDiscardChanges()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog and keep the user on the same fragment

                // Determine where to navigate back to based on the current fragment
                val navController = findNavController()

                navController.navigate(R.id.myProfileFragment)

                val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
                bottomNavigationView.selectedItemId = R.id.nav_my_profile

            }
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!hasSubmitted && hasUnsavedChanges()) {
            showUnsavedChangesDialog {
                // Logic to discard changes if user confirms
                findNavController().navigateUp()
            }
        }
        _binding = null
    }

}
