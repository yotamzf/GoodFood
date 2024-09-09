package com.example.goodfoodapp.profile

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.goodfoodapp.R
import com.example.goodfoodapp.databinding.FragmentMyProfileBinding
import com.example.goodfoodapp.utils.CircleTransform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.squareup.picasso.Picasso


class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user != null) {
            loadUserData(user.uid)
        }

        // Edit name on pencil click
        binding.editNameIcon.setOnClickListener {
            binding.nameEdit.isEnabled = true
        }

        // Save changes on button click
        binding.btnSaveChanges.setOnClickListener {
            saveUserData(user!!.uid)
        }

        // Change profile picture on camera icon click
        binding.changePictureIcon.setOnClickListener {
            // Logic for changing picture
        }

        // Discard changes
        binding.btnDiscardChanges.setOnClickListener {
            loadUserData(user!!.uid)
        }
    }

    private fun loadUserData(uid: String) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val profilePicUrl = document.getString("profilePic")

                    binding.nameEdit.setText(name)
                    binding.emailEdit.setText(email)

                    if (!profilePicUrl.isNullOrEmpty()) {
                        // Load the image using Picasso with circular transformation
                        Picasso.get()
                            .load(profilePicUrl)
                            .placeholder(R.drawable.ic_default_user_profile)
                            .error(R.drawable.ic_default_user_profile)
                            .transform(CircleTransform())
                            .into(binding.profileImage)
                    }
                }
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    private fun saveUserData(uid: String) {
        val updatedName = binding.nameEdit.text.toString()

        val updates = hashMapOf(
            "name" to updatedName,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                // Notify user of success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
