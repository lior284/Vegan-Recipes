package com.example.vegan_recipes

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("UseKtx")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val prefs = requireContext().getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, Context.MODE_PRIVATE)

        val etEmail = view.findViewById<TextView>(R.id.profileFragment_email_tv)
        val etFullName = view.findViewById<TextView>(R.id.profileFragment_fullName_tv)
        val ivProfilePicture = view.findViewById<ImageView>(R.id.profileFragment_profilePicture_iv)

        val tvUserDetails = view.findViewById<TextView>(R.id.profileFragment_userDetails_tv)
        val tvSavedRecipes = view.findViewById<TextView>(R.id.profileFragment_savedRecipes_tv)
        val scDarkMode = view.findViewById<SwitchCompat>(R.id.profileFragment_darkMode_sc)
        val tvNotifications = view.findViewById<TextView>(R.id.profileFragment_notifications_tv)
        val tvAbout = view.findViewById<TextView>(R.id.profileFragment_about_tv)
        val tvSignOut = view.findViewById<TextView>(R.id.profileFragment_signOut_tv)

        handleProfilePicture(prefs, ivProfilePicture)

        etEmail.text = prefs.getString(AppPrefsConstants.EMAIL_KEY, "")
        val firstName = prefs.getString(AppPrefsConstants.FIRST_NAME_KEY, "")
        val lastName = prefs.getString(AppPrefsConstants.LAST_NAME_KEY, "")
        etFullName.text = "$firstName $lastName"

        val userUID = prefs.getString(AppPrefsConstants.USER_UID_KEY, "")
        scDarkMode.isChecked = SettingsManager.isDarkModeEnabled(requireContext(), userUID.orEmpty())

        tvUserDetails.setOnClickListener {
            startActivity(Intent(requireContext(), UserDetailsActivity::class.java))
        }

        tvSavedRecipes.setOnClickListener {
            startActivity(Intent(requireContext(), SavedRecipesActivity::class.java))
        }

        scDarkMode.setOnCheckedChangeListener { _, isChecked ->
            scDarkMode.postDelayed({
                    SettingsManager.setDarkModeEnabled(requireContext(), userUID.orEmpty(), isChecked)
                    SettingsManager.applySettings(requireContext(), userUID.orEmpty())
                }, 175)
        }

        tvNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsSettingsActivity::class.java))
        }

        tvAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        tvSignOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Sign out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes") { _, _ ->
                    // Clearing all the data from the shared prefs
                    prefs.edit().clear().apply()

                    // Signing out the user
                    val auth = FirebaseAuth.getInstance()
                    auth.signOut()
                    Toast.makeText(requireContext(), "User Signed Out.", Toast.LENGTH_LONG).show()

                    startActivity(Intent(requireContext(), StartPageActivity::class.java))
                    requireActivity().finish()

                    SettingsManager.applyDefaultSettings()

                    activity?.finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        return view
    }

    // Loading the profile picture from prefs, setting a clickListener for it, and a callback for it
    @SuppressLint("UseKtx")
    private fun handleProfilePicture(prefs: SharedPreferences, ivProfilePicture: ImageView) {
        val savedBase64 = prefs.getString(AppPrefsConstants.PROFILE_PICTURE_KEY, null)
        if (savedBase64 != null) {
            try {
                val bytes = android.util.Base64.decode(savedBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                Glide.with(this)
                    .load(bitmap)
                    .circleCrop()
                    .into(ivProfilePicture)
            } catch (_: Exception) {
                Glide.with(this)
                    .load(R.drawable.img_take_profile_picture)
                    .circleCrop()
                    .into(ivProfilePicture)
            }
        } else {
            Glide.with(this)
                .load(R.drawable.img_take_profile_picture)
                .circleCrop()
                .into(ivProfilePicture)
        }

        ivProfilePicture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) { // If the user doesn't have the permission then requesting it
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
            } else {
                openCamera()
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let { bmp ->
                    // After the user confirmed the picture
                    Glide.with(this)
                        .load(bmp)
                        .circleCrop()
                        .into(ivProfilePicture)
                    val imageUri = saveBitmapToTempFile(bmp)

                    (requireContext() as MenuActivity).showLoadingOverlayOnMenu()

                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    val db = FirebaseFirestore.getInstance()
                    val storage = FirebaseStorage.getInstance()

                    storage.getReference("profile_pictures/" + user!!.uid + ".jpg")
                        .putFile(imageUri)
                        .addOnSuccessListener {
                            // Save file name in Firestore
                            db.collection("users")
                                .document(user.uid)
                                .update("profilePicture", user.uid + ".jpg")
                                .addOnSuccessListener {
                                    // Save image as base64 in SharedPreferences
                                    val prefs = requireContext().getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, Context.MODE_PRIVATE)
                                    val stream = java.io.ByteArrayOutputStream()
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                    val bytes = stream.toByteArray()
                                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

                                    prefs.edit().putString(AppPrefsConstants.PROFILE_PICTURE_KEY, base64).apply()
                                    (requireContext() as MenuActivity).hideLoadingOverlayOnMenu()
                                }
                                .addOnFailureListener {
                                    storage.getReference("profile_pictures/" + user.uid + ".jpg").delete()
                                    Toast.makeText(requireContext(), "Error uploading image, please try again later.", Toast.LENGTH_SHORT).show()
                                    (requireContext() as MenuActivity).hideLoadingOverlayOnMenu()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error uploading image, please try again later.", Toast.LENGTH_SHORT).show()
                            ivProfilePicture.setImageResource(R.drawable.img_take_profile_picture)
                            prefs.edit().remove(AppPrefsConstants.PROFILE_PICTURE_KEY).apply()
                            (requireContext() as MenuActivity).hideLoadingOverlayOnMenu()
                        }
                }
            }
        }
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "temp_image.jpg")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        return Uri.fromFile(file)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
