package com.example.veganism

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StartPageActivity : AppCompatActivity() {
    private lateinit var imageSwitcher: ImageSwitcher

    private val images = listOf(
        R.drawable.img_example_1,
        R.drawable.img_example_2,
        R.drawable.img_example_3
    )
    private var index = 0
    private val switchInterval: Long = 5000
    private val pauseAfterClick: Long = 5000
    private val handler = Handler(Looper.getMainLooper())
    private val switchRunnable = object : Runnable {
        override fun run() {
            index = (index + 1) % images.size
            imageSwitcher.setImageResource(images[index])
            handler.postDelayed(this, switchInterval)

        }
    }

    @SuppressLint("UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        if (prefs.getBoolean("rememberMe", false) && user != null) {
            showLoadingOverlay()
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener {
                    val myUser = it.toObject(MyUser::class.java)
                    prefs.edit() {
                        putString("firstName", myUser!!.firstName)
                        putString("lastName", myUser.lastName)
                        putString("username", myUser.username)
                        putString("email", auth.currentUser?.email)
                        putString("userUID", auth.currentUser?.uid)
                        apply()
                    }

                    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                    storage.getReference("profile_pictures/" + myUser?.profilePicture).getBytes(1024 * 1024) // Picture is 1MB
                        .addOnSuccessListener { bytes ->
                            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            // Save Base64 in SharedPreferences
                            val prefs = this.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            val stream = java.io.ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                            val bytes = stream.toByteArray()
                            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                            prefs.edit().putString("profilePicture", base64).apply()
                        }
                        .addOnFailureListener {
                            prefs.edit().putString("profilePicture", "img_take_profile_picture.png").apply()
                        }

                    loadUserSettingsFromPrefs(user.uid)

                    hideLoadingOverlay()
                    startActivity(Intent(this, MenuActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    hideLoadingOverlay()
                    Toast.makeText(this, "Error Loading User", Toast.LENGTH_LONG).show()
                    prefs.edit().putBoolean("rememberMe", false).apply()
                }
        } else if (user != null) {
            auth.signOut() // Clear currentUser if user is not logged in and rememberMe is false
        } else {
            prefs.edit().putBoolean("rememberMe", false).apply() // Reset rememberMe if user is not logged in
        }

        val cvImageSwitcher = findViewById<CardView>(R.id.startPage_isView_cv)
        if (getResources().configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            == android.content.res.Configuration.UI_MODE_NIGHT_YES
        ) {
            cvImageSwitcher.alpha = 0.7f
        } else {
            cvImageSwitcher.alpha = 1.0f
        }

        val registerBtn = findViewById<Button>(R.id.startPage_register_btn)
        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val signInBtn = findViewById<Button>(R.id.startPage_signIn_btn)
        signInBtn.setOnClickListener {
            startActivity(Intent(this, SigninActivity::class.java))
        }

        val tvGuest = findViewById<TextView>(R.id.startPage_guest_tv)
        tvGuest.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        imageSwitcher = findViewById(R.id.startPage_startImages_is)
        imageSwitcher.setFactory {
            ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }
        imageSwitcher.setImageResource(images[index])

        handler.postDelayed(switchRunnable, switchInterval)

        imageSwitcher.setOnClickListener {
            // Switch image immediately
            index = (index + 1) % images.size
            imageSwitcher.setImageResource(images[index])

            // Pause automatic switching, then resume
            handler.removeCallbacks(switchRunnable)
            handler.postDelayed(switchRunnable, pauseAfterClick)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Stop the handler when the activity is destroyed
        handler.removeCallbacks(switchRunnable)
    }

    private fun loadUserSettingsFromPrefs(uid: String) {
        SettingsManager.applySettings(this, uid)
    }

    fun showLoadingOverlay() {
        findViewById<FrameLayout>(R.id.startPage_loadingOverlay_fl).visibility = View.VISIBLE
    }

    fun hideLoadingOverlay() {
        findViewById<FrameLayout>(R.id.startPage_loadingOverlay_fl).visibility = View.GONE
    }
}
