package com.example.vegan_recipes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import java.io.File
import java.io.FileOutputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddRecipeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddRecipeFragment : Fragment() {
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

    private lateinit var etRecipeName: EditText
    private lateinit var etRecipeDescription: EditText
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var imageUri: Uri = Uri.EMPTY
    private lateinit var ivRecipeImage: ImageView
    private lateinit var llRecipeImage: LinearLayout

    private var invalidFields: String = ""

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_recipe, container, false)

        etRecipeName = view.findViewById<EditText>(R.id.addRecipeFragment_recipeName_et)
        etRecipeDescription = view.findViewById<EditText>(R.id.addRecipeFragment_recipeDescription_et)
        ivRecipeImage = view.findViewById<ImageView>(R.id.addRecipeFragment_recipeImage_iv)
        llRecipeImage = view.findViewById<LinearLayout>(R.id.addRecipeFragment_recipeImage_ll)
        val btnNext = view.findViewById<Button>(R.id.addRecipeFragment_next_btn)
        val btnReset = view.findViewById<Button>(R.id.addRecipeFragment_reset_btn)

        ivRecipeImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
            } else {
                openCamera()
            }
        }

        // After the user pressed the confirmation btn
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    ivRecipeImage.setImageBitmap(it)
                    imageUri = saveBitmapToTempFile(it)
                }
            }
        }

        btnReset.setOnClickListener {
            etRecipeName.text.clear()
            etRecipeDescription.text.clear()
            ivRecipeImage.setImageResource(R.drawable.img_recipe_item_example)
            imageUri = Uri.EMPTY
            invalidFields = ""
            setErrorOutline(etRecipeName, true)
            setErrorOutline(etRecipeDescription, true)
            setErrorOutline(llRecipeImage, true)
        }


        btnNext.setOnClickListener {
            if(checkAllInputsValid())
            {
                val intent = Intent(requireContext(), AddRecipeNextActivity::class.java)
                intent.putExtra("recipeName", etRecipeName.text.trim().toString())
                intent.putExtra("recipeDescription", etRecipeDescription.text.trim().toString())
                intent.putExtra("recipeImage", imageUri.toString())
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Invalid fields: $invalidFields", Toast.LENGTH_SHORT).show()
                invalidFields = "" // Reset the invalid fields if the user tries submitting again with invalid fields
            }
        }

        return view
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
                Toast.makeText(requireContext(), "Please enable camera permission", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun checkAllInputsValid(): Boolean {
        var allValid = true

        val recipeNameText = etRecipeName.text.trim()
        var curValid = recipeNameText.isNotEmpty() && recipeNameText.length >= 3
        if (!curValid)
        {
            allValid = false
            invalidFields += " Recipe Name,"
        }
        setErrorOutline(etRecipeName, curValid)

        val recipeDescriptionText = etRecipeDescription.text.trim()
        curValid = recipeDescriptionText.isNotEmpty() && recipeDescriptionText.length >= 3
        if (!curValid)
        {
            allValid = false
            invalidFields += " Recipe Description,"
        }
        setErrorOutline(etRecipeDescription, curValid)

        curValid = imageUri != Uri.EMPTY
        if (!curValid)
        {
            allValid = false
            invalidFields += " Recipe Image,"
        }
        setErrorOutline(llRecipeImage, curValid)


        if(invalidFields != "")
        {
            invalidFields = invalidFields.substring(0, invalidFields.length-1)
        }

        return allValid
    }

    private fun setErrorOutline(view: View, isValid: Boolean) {
        val drawable = view.background as GradientDrawable
        if (!isValid) {
            drawable.setStroke(3, Color.RED)
        } else {
            drawable.setStroke(1, "#DDDDDD".toColorInt())
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddRecipeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddRecipeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
