package com.example.vegan_recipes

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
import java.util.Calendar

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
            if (!checkAllInputsValid()) {
                Toast.makeText(this, "There is a problem in the following input(s):$invalidFields", Toast.LENGTH_LONG).show()
                invalidFields = "" // Reset the invalid fields if the user tries submitting again with invalid fields
                return@setOnClickListener
            }

            validateUsernameAndCreateAccount() // Main function - checking if username already exist and creating account if not
        }
    }
    private fun passwordsListeners() {
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
    private fun togglePasswordVisibility(editText: EditText, visible: Boolean, button: ImageButton) {
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

        etUsername.setText("")
        (etUsername.background as GradientDrawable).setStroke(1, "#DDDDDD".toColorInt())

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

        val firstNameText = etFirstName.text.trim()
        var curValid = firstNameText.isNotEmpty() && firstNameText.all { it.isLetter() } && firstNameText.length >= 2
        if (!curValid)
        {
            allValid = false
            invalidFields += " First Name,"
        }
        setErrorOutline(etFirstName, curValid)

        val lastNameText = etLastName.text.trim()
        curValid = lastNameText.isNotEmpty() && lastNameText.all { it.isLetter() } && lastNameText.length >= 2
        if (!curValid)
        {
            allValid = false
            invalidFields += " Last Name,"
        }
        setErrorOutline(etLastName, curValid)

        val usernameText = etUsername.text.trim()
        curValid = usernameText.isNotEmpty() && usernameText.length >= 3
        if (!curValid)
        {
            allValid = false
            invalidFields += " Username,"
        }
        setErrorOutline(etUsername, curValid)

        val emailText = etEmail.text.trim()
        curValid = emailText.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailText).matches()
        if (!curValid)
        {
            allValid = false
            invalidFields += " Email,"
        }
        setErrorOutline(etEmail, curValid)

        val birthYearText = etBirthYear.text.trim()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val birthYearInt = birthYearText.toString().toIntOrNull() ?: 0
        curValid = birthYearText.isNotEmpty() && birthYearText.all { it.isDigit() } && birthYearInt <= currentYear && birthYearInt >= 1900
        if (!curValid)
        {
            allValid = false
            invalidFields += " Birth Year,"
        }
        setErrorOutline(etBirthYear, curValid)

        val password = etPassword.text.trim()
        curValid = password.isNotEmpty() && password.length >= 8 &&
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

        val confirmPassword = etConfirmPassword.text.trim()
        curValid = confirmPassword.isNotEmpty() && confirmPassword.toString() == password.toString()
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

    private fun validateUsernameAndCreateAccount() {
        val usernameText = etUsername.text.trim()
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("username", usernameText)
            .get()
            .addOnSuccessListener {
                if(!it.isEmpty) {
                    Toast.makeText(this, "Username already exists.", Toast.LENGTH_LONG).show()
                    setErrorOutline(etUsername, false)
                } else {
                    createAccount()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error creating user, please try again later.", Toast.LENGTH_LONG).show()
            }
    }
    private fun createAccount() {
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
                    db.collection("users")
                        .document(user!!.uid)
                        .set(myUser)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User Created Successfully.", Toast.LENGTH_LONG).show()
                            // Signing in the user after the registration
                            auth.signInWithEmailAndPassword(
                                etEmail.text.toString(),
                                etPassword.text.toString()
                            )

                            saveUserDetailsInPrefs()
                            SettingsManager.applyDefaultSettings()
                            SettingsManager.saveDefaultSettings(this, user.uid)

                            startActivity(Intent(this, MenuActivity::class.java))
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error creating user, please try again later.", Toast.LENGTH_LONG).show()
                        }
                } else {
                    val exception = task.exception
                    showSpecificErrorMessage(exception)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error creating user, please try again later.", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserDetailsInPrefs() {
        val prefs = getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser

        prefs.edit {
            putBoolean(AppPrefsConstants.REMEMBER_ME_KEY, cbRememberMe.isChecked)

            putString(AppPrefsConstants.FIRST_NAME_KEY, etFirstName.text.toString())
            putString(AppPrefsConstants.LAST_NAME_KEY, etLastName.text.toString())
            putString(AppPrefsConstants.USERNAME_KEY, etUsername.text.toString())
            putString(AppPrefsConstants.EMAIL_KEY, etEmail.text.toString())
            remove(AppPrefsConstants.PROFILE_PICTURE_KEY)
            putString(AppPrefsConstants.USER_UID_KEY, user!!.uid)
            apply()
        }
    }
    private fun showSpecificErrorMessage(exception: Exception?) {
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
                Toast.makeText(this, "No internet connection, please try again later.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Error creating user, please try again later.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
