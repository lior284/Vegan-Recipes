package com.example.veganism

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.edit
import com.google.firebase.FirebaseNetworkException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etBirthYear: EditText
    private lateinit var rgIsVegan: RadioGroup

    private lateinit var llPassword: LinearLayout
    private lateinit var etPassword: EditText
    private lateinit var llConfirmPassword: LinearLayout
    private lateinit var etConfirmPassword: EditText
    private lateinit var cbRememberMe: CheckBox

    private var invalidFields: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etFirstName = findViewById(R.id.register_firstName_et)
        etLastName = findViewById(R.id.register_lastName_et)
        etUsername = findViewById(R.id.register_username_et)
        etEmail = findViewById(R.id.register_email_et)
        etBirthYear = findViewById(R.id.register_birthYear_et)
        rgIsVegan = findViewById(R.id.register_isVegan_rg)

        etPassword = findViewById(R.id.register_password_et)
        llPassword = findViewById(R.id.register_password_ll)
        etConfirmPassword = findViewById(R.id.register_confirmPassword_et)
        llConfirmPassword = findViewById(R.id.register_confirmPassword_ll)

        cbRememberMe = findViewById(R.id.register_rememberMe_cb)

        passwordsListeners()

        val btnReset = findViewById<Button>(R.id.register_reset_btn)
        btnReset.setOnClickListener { resetFields() }

        val btnSubmit = findViewById<Button>(R.id.register_submit_btn)
        btnSubmit.setOnClickListener {
            val allValid = checkAllInputsValid()
            if (allValid) {
                val auth = FirebaseAuth.getInstance()
                auth.createUserWithEmailAndPassword(
                    etEmail.text.toString(),
                    etPassword.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Adding the user to the database
                            val user = auth.currentUser
                            val myUser = MyUser(
                                etFirstName.text.toString(),
                                etLastName.text.toString(),
                                etUsername.text.toString(),
                                etBirthYear.text.toString().toInt(),
                                rgIsVegan.checkedRadioButtonId == R.id.register_yes_rb,
                                "img_take_profile_picture.png"
                            )
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(user!!.uid).set(myUser)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "User Created Successfully.", Toast.LENGTH_LONG)
                                        .show()
                                    // Signing in the user after the registration
                                    auth.signInWithEmailAndPassword(
                                        etEmail.text.toString(),
                                        etPassword.text.toString()
                                    )

                                    saveUserDetailsInPrefs()
                                    saveDefaultSettingsInPrefs(user.uid)
                                    setDefaultSettings()

                                    startActivity(Intent(this, MenuActivity::class.java))
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error Creating User.", Toast.LENGTH_LONG)
                                        .show()
                                }
                        } else {
                            val exception = task.exception
                            showSpecificErrorMessage(exception!!)
                        }
                    }
            } else {
                Toast.makeText(this, "There is a problem in the following input(s):$invalidFields", Toast.LENGTH_LONG).show()
                invalidFields = "" // If the user still haVe invalids fields than I write it again to him
            }
        }
    }
    private fun passwordsListeners()
    {
        var isPasswordVisible = false
        var isConfirmVisible = false

        val btnTogglePassword = findViewById<ImageButton>(R.id.register_togglePassword_btn)
        val btnToggleConfirm = findViewById<ImageButton>(R.id.register_toggleConfirmPassword_btn)

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, isPasswordVisible, btnTogglePassword)
        }

        btnToggleConfirm.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            togglePasswordVisibility(etConfirmPassword, isConfirmVisible, btnToggleConfirm)
        }
    }
    private fun togglePasswordVisibility(editText: EditText, visible: Boolean, button: ImageButton)
    {
        val typeface = editText.typeface // save current font

        if (visible) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setImageResource(R.drawable.ic_eye)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setImageResource(R.drawable.ic_eye_off)
        }

        editText.typeface = typeface // restore the font
        editText.setSelection(editText.text.length) // make the cursor at the end of the text
    }
    private fun resetFields() {
        etFirstName.setText("")
        (etFirstName.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())

        etLastName.setText("")
        (etLastName.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())

        etEmail.setText("")
        (etEmail.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())

        etBirthYear.setText("")
        (etBirthYear.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())

        rgIsVegan.check(R.id.register_no_rb)

        etPassword.setText("")
        (llPassword.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())
        etConfirmPassword.setText("")
        (llConfirmPassword.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())
    }
    private fun checkAllInputsValid(): Boolean {
        var allValid = true

        var curValid = !etFirstName.text.isNullOrEmpty() && etFirstName.text.all { it.isLetter() } && etFirstName.text.length >= 2
        if (!curValid)
        {
            allValid = false
            invalidFields += " First Name,"
        }
        setErrorOutline(etFirstName, curValid)

        curValid = !etLastName.text.isNullOrEmpty() && etLastName.text.all { it.isLetter() } && etLastName.text.length >= 2
        if (!curValid)
        {
            allValid = false
            invalidFields += " Last Name,"
        }
        setErrorOutline(etLastName, curValid)

        curValid = !etEmail.text.isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(etEmail.text).matches()
        if (!curValid)
        {
            allValid = false
            invalidFields += " Email,"
        }
        setErrorOutline(etEmail, curValid)

        curValid = !etBirthYear.text.isNullOrEmpty() && etBirthYear.text.all { it.isDigit() } && etBirthYear.text.toString().toInt() <= 2025
        if (!curValid)
        {
            allValid = false
            invalidFields += " Birth Year,"
        }
        setErrorOutline(etBirthYear, curValid)

        val password = etPassword.text
        curValid = !password.isNullOrEmpty() && password.length >= 8 &&
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

        curValid = !etConfirmPassword.text.isNullOrEmpty() && etConfirmPassword.text.toString() == etPassword.text.toString()
        if (!curValid)
        {
            allValid = false
            invalidFields += " Confirm Password,"
        }
        setErrorOutline(llConfirmPassword, curValid)

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


    private fun saveUserDetailsInPrefs() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser

        prefs.edit {
            putBoolean("rememberMe", cbRememberMe.isChecked)

            putString("firstName", etFirstName.text.toString())
            putString("lastName", etLastName.text.toString())
            putString("username", etUsername.text.toString())
            putString("email", etEmail.text.toString())
            putString("profilePicture", "img_take_profile_picture.png")
            putString("userUID", user!!.uid)
            apply()
        }
    }
    private fun saveDefaultSettingsInPrefs(uid: String) {
        SettingsManager.saveDefaultSettings(this, uid)
    }
    private fun setDefaultSettings()
    {
        SettingsManager.applyDefaultSettings()
    }
    private fun showSpecificErrorMessage(exception: Exception) {
        when (exception) {
            is  FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "User already exists.", Toast.LENGTH_LONG).show()
            }
            is FirebaseAuthWeakPasswordException -> {
                Toast.makeText(this, "Password is too weak.", Toast.LENGTH_LONG)
                    .show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Email is invalid.", Toast.LENGTH_LONG).show()
            }
            is FirebaseNetworkException -> {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Error Creating User.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
