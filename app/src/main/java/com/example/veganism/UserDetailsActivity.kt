package com.example.veganism

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etUsername: EditText
    private lateinit var tvEmail: TextView
    private lateinit var etBirthYear: EditText
    private lateinit var rgIsVegan: RadioGroup
    private lateinit var tvIsVegan: TextView

    private var originalFirstName: String = ""
    private var originalLastName: String = ""
    private var originalUsername: String = ""
    private var originalBirthYear: String = ""
    private var originalIsVegan: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etFirstName = findViewById(R.id.userDetails_firstName_et)
        etLastName = findViewById(R.id.userDetails_lastName_et)
        etUsername = findViewById(R.id.userDetails_username_et)
        tvEmail = findViewById(R.id.userDetails_email_tv)
        etBirthYear = findViewById(R.id.userDetails_birthYear_et)
        rgIsVegan = findViewById(R.id.userDetails_isVegan_rg)
        tvIsVegan = findViewById(R.id.userDetails_isVegan_tv)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        tvEmail.text = user!!.email
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener {
                val myUser = it.toObject(MyUser::class.java)
                originalFirstName = myUser!!.firstName
                originalLastName = myUser.lastName
                originalUsername = myUser.username
                originalBirthYear = myUser.birthYear.toString()
                originalIsVegan = myUser.isVegan


                etFirstName.setText(originalFirstName)
                etLastName.setText(originalLastName)
                etUsername.setText(originalUsername)
                etBirthYear.setText(originalBirthYear)

                if(originalIsVegan)
                    rgIsVegan.check(R.id.userDetails_yes_rb)
                else
                    rgIsVegan.check(R.id.userDetails_no_rb)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error getting user data.", Toast.LENGTH_LONG).show()
            }


        etFirstName.addTextChangedListener {
            val text = it.toString()
            val drawable = etFirstName.background as GradientDrawable
            if(text != originalFirstName)
                drawable.setStroke(3, "#2196F3".toColorInt())
            else
                drawable.setStroke(1, "#DDDDDD".toColorInt())
        }
        etLastName.addTextChangedListener {
            val text = it.toString()
            val drawable = etLastName.background as GradientDrawable
            if(text != originalLastName)
                drawable.setStroke(3, "#2196F3".toColorInt())
            else
                drawable.setStroke(1, "#DDDDDD".toColorInt())
        }
        etUsername.addTextChangedListener {
            val text = it.toString()
            val drawable = etUsername.background as GradientDrawable
            if(text != originalUsername)
                drawable.setStroke(3, "#2196F3".toColorInt())
            else
                drawable.setStroke(1, "#DDDDDD".toColorInt())
        }
        etBirthYear.addTextChangedListener {
            val text = it.toString()
            val drawable = etBirthYear.background as GradientDrawable
            if(text != originalBirthYear)
                drawable.setStroke(3, "#2196F3".toColorInt())
            else
                drawable.setStroke(1, "#DDDDDD".toColorInt())
        }
        rgIsVegan.setOnCheckedChangeListener { _, _ ->
            val changed = (rgIsVegan.checkedRadioButtonId == R.id.userDetails_yes_rb) != originalIsVegan

            val rbIsVeganYes = findViewById<RadioButton>(R.id.userDetails_yes_rb)
            val rbIsVeganNo = findViewById<RadioButton>(R.id.userDetails_no_rb)

            if(changed)
            {
                tvIsVegan.setTextColor("#2196F3".toColorInt())
                rbIsVeganYes.setTextColor("#2196F3".toColorInt())
                rbIsVeganYes.buttonTintList = ColorStateList.valueOf("#2196F3".toColorInt())
                rbIsVeganNo.setTextColor("#2196F3".toColorInt())
                rbIsVeganNo.buttonTintList = ColorStateList.valueOf("#2196F3".toColorInt())
            } else {
                tvIsVegan.setTextColor(AppCompatResources.getColorStateList(this, R.color.primaryUI))
                rbIsVeganYes.setTextColor(AppCompatResources.getColorStateList(this, R.color.primaryUI))
                rbIsVeganYes.buttonTintList = ColorStateList.valueOf(AppCompatResources.getColorStateList(this, R.color.primaryUI).defaultColor)
                rbIsVeganNo.setTextColor(AppCompatResources.getColorStateList(this, R.color.primaryUI))
                rbIsVeganNo.buttonTintList = ColorStateList.valueOf(AppCompatResources.getColorStateList(this, R.color.primaryUI).defaultColor)
            }
        }


        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed(this)
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        val backBtn = findViewById<TextView>(R.id.userDetails_backArrow_iv)
        backBtn.setOnClickListener {
            handleBackPressed(backCallback)
        }

        val btnReset = findViewById<Button>(R.id.userDetails_reset_btn)
        btnReset.setOnClickListener { resetFields() }

        val btnChange = findViewById<Button>(R.id.userDetails_change_btn)
        btnChange.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser

            val myUser = MyUser(
                etFirstName.text.toString(),
                etLastName.text.toString(),
                etUsername.text.toString(),
                etBirthYear.text.toString().toInt(),
                rgIsVegan.checkedRadioButtonId == R.id.userDetails_yes_rb,
                "${user!!.uid}.jpg"
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).set(myUser)
                .addOnSuccessListener {
                    saveUserDetailsInPrefs()
                    Toast.makeText(this, "User Updated Successfully.", Toast.LENGTH_SHORT).show()

                    finish()
                    startActivity(Intent(this, MenuActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error Updating User.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun handleBackPressed(backCallback: OnBackPressedCallback) {
        if (madeChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Yes") { _, _ ->
                    backCallback.isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            backCallback.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun madeChanges(): Boolean {
        val currentIsVegan = rgIsVegan.checkedRadioButtonId == R.id.userDetails_yes_rb

        return etFirstName.text.toString() != originalFirstName ||
                etLastName.text.toString() != originalLastName ||
                etUsername.text.toString() != originalUsername ||
                etBirthYear.text.toString() != originalBirthYear ||
                currentIsVegan != originalIsVegan
    }
    private fun saveUserDetailsInPrefs() {
        val prefs = getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, MODE_PRIVATE)
        prefs.edit {
            putString(AppPrefsConstants.FIRST_NAME_KEY, etFirstName.text.toString())
            putString(AppPrefsConstants.LAST_NAME_KEY, etLastName.text.toString())
            putString(AppPrefsConstants.USERNAME_KEY, etUsername.text.toString())
            apply()
        }
    }
    private fun resetFields() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user!!.uid).get()
            .addOnSuccessListener {
                etFirstName.setText(it.get("firstName").toString())
                etLastName.setText(it.get("lastName").toString())
                etUsername.setText(it.get("username").toString())
                etBirthYear.setText(it.get("birthYear").toString())
                if(it.get("isVegan").toString() == "true")
                {
                    rgIsVegan.check(R.id.userDetails_yes_rb)
                }
                else
                {
                    rgIsVegan.check(R.id.userDetails_no_rb)
                }
            }
    }
}
