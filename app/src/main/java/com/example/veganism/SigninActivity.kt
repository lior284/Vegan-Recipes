package com.example.veganism

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SigninActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var llPassword: LinearLayout
    private lateinit var etPassword: EditText
    private lateinit var cbRememberMe: CheckBox
    private var invalidFields: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etEmail = findViewById(R.id.signIn_email_et)
        etPassword = findViewById(R.id.signIn_password_et)
        llPassword = findViewById(R.id.signIn_password_ll)
        cbRememberMe = findViewById(R.id.signIn_rememberMe_cb)

        var isPasswordVisible = false
        val btnTogglePassword = findViewById<ImageButton>(R.id.signIn_togglePassword_btn)
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(isPasswordVisible, btnTogglePassword)
        }

        val btnReset = findViewById<Button>(R.id.signIn_reset_btn)
        btnReset.setOnClickListener { resetFields() }

        val btnSubmit = findViewById<Button>(R.id.signIn_submit_btn)
        btnSubmit.setOnClickListener {
            val allValid = checkAllInputsValid()
            if (allValid) {
                val auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(
                    etEmail.text.toString(),
                    etPassword.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "User Signed In Successfully.", Toast.LENGTH_SHORT).show()

                            val user = auth.currentUser
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(user!!.uid).get()
                                .addOnSuccessListener {
                                    val myUser = it.toObject(MyUser::class.java)
                                    saveUserDetailsInPrefs(myUser!!)
                                    loadUserSettingsFromPrefs(user.uid)

                                    startActivity(Intent(this, MenuActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error Loading User", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            val exception = task.exception
                            showSpecificErrorMessage(exception!!)
                        }
                    }
            } else {
                Toast.makeText(this, "Invalid field(s): $invalidFields", Toast.LENGTH_LONG).show()
                invalidFields = ""
            }
        }
    }
    private fun togglePasswordVisibility(visible: Boolean, button: ImageButton)
    {
        val typeface = etPassword.typeface // save current font

        if (visible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setImageResource(R.drawable.ic_eye)
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setImageResource(R.drawable.ic_eye_off)
        }

        etPassword.typeface = typeface // restore the font
        etPassword.setSelection(etPassword.text.length) // make the cursor at the end of the text
    }
    private fun resetFields() {
        etEmail.setText("")
        (etEmail.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())

        etPassword.setText("")
        (llPassword.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())
    }
    private fun checkAllInputsValid(): Boolean {
        var allValid = true

        var curValid = Patterns.EMAIL_ADDRESS.matcher(etEmail.text).matches()
        if (!curValid)
        {
            allValid = false
            invalidFields += " Email,"
        }
        setErrorOutline(etEmail, curValid)

        val password = etPassword.text
        curValid = password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isUpperCase() } &&
                password.any { !it.isLetterOrDigit() } // Checks for special character
        if (!curValid)
        {
            allValid = false
            invalidFields += " Password,"
        }
        setErrorOutline(llPassword, curValid)

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

    @SuppressLint("UseKtx")
    private fun saveUserDetailsInPrefs(myUser: MyUser) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser

        prefs.edit {
            putBoolean("rememberMe", cbRememberMe.isChecked)

            putString("firstName", myUser.firstName)
            putString("lastName", myUser.lastName)
            putString("email", etEmail.text.toString())
            putString("userUID", user!!.uid)
        }

        val storage = FirebaseStorage.getInstance()
        storage.getReference("profile_pictures/" + myUser.profilePicture).getBytes(1024 * 1024) // Picture is 1MB
            .addOnSuccessListener { bytes ->
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Save Base64 in SharedPreferences
                val prefs = this@SigninActivity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                prefs.edit().putString("profilePicture", base64).apply()
            }
            .addOnFailureListener {
                prefs.edit().putString("profilePicture", "img_take_profile_picture.png").apply()
            }
    }
    private fun loadUserSettingsFromPrefs(uid: String) {
        SettingsManager.applySettings(this, uid)
    }
    private fun showSpecificErrorMessage(exception: Exception) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                Toast.makeText(this, "User does not exist.", Toast.LENGTH_LONG).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Email or password is incorrect", Toast.LENGTH_LONG).show()
            }
            is FirebaseNetworkException -> {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Error Signing In.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
