package com.example.veganism

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class MenuActivity : AppCompatActivity() {

    private lateinit var homeBtn: TextView
    private lateinit var addRecipeBtn: TextView
    private lateinit var weekPlanBtn: TextView
    private lateinit var profileBtn: TextView
    private lateinit var registerSignInBtn: TextView

    private lateinit var indicator: View
    private var firstLoad = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        homeBtn = findViewById<TextView>(R.id.homePage_home_tv)
        addRecipeBtn = findViewById<TextView>(R.id.homePage_addRecipe_tv)
        weekPlanBtn = findViewById<TextView>(R.id.homePage_WeekPlan_tv)
        profileBtn = findViewById<TextView>(R.id.homePage_profile_tv)
        registerSignInBtn = findViewById<TextView>(R.id.homePage_registerSignIn_tv)

        indicator = findViewById(R.id.homePage_indicator_v)

        val prefs = getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, MODE_PRIVATE)

        val lastFragment = prefs.getString(AppPrefsConstants.LAST_FRAGMENT_KEY, null)

        val (fragment, button) = when (lastFragment) {
            "AddRecipeFragment" -> AddRecipeFragment() to addRecipeBtn
            "WeekPlanFragment" -> WeekPlanFragment() to weekPlanBtn
            "ProfileFragment" -> ProfileFragment() to profileBtn
            else -> HomeFragment() to homeBtn
        }

        switchFragment(fragment, button)


        if (FirebaseAuth.getInstance().currentUser != null) {
            addRecipeBtn.visibility = View.VISIBLE
            weekPlanBtn.visibility = View.VISIBLE
            profileBtn.visibility = View.VISIBLE
            registerSignInBtn.visibility = View.GONE

            // Get screen width in pixels
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            // Set indicator width to quarter the screen width (there are four buttons)
            indicator.layoutParams.width = screenWidth / 4
            indicator.requestLayout()
        } else {
            addRecipeBtn.visibility = View.GONE
            weekPlanBtn.visibility = View.GONE
            profileBtn.visibility = View.GONE
            registerSignInBtn.visibility = View.VISIBLE

            // Get screen width in pixels
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            // Set indicator width to half the screen width (there are only two buttons)
            indicator.layoutParams.width = screenWidth / 2
            indicator.requestLayout()
        }

        indicator.post {
            indicator.x = button.x
        }

        homeBtn.setOnClickListener {
            switchFragment(HomeFragment(), homeBtn)
        }
        addRecipeBtn.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                switchFragment(AddRecipeFragment(), addRecipeBtn)
            } else {
                Toast.makeText(this, "You need to sign in or register", Toast.LENGTH_SHORT).show()
            }
        }
        weekPlanBtn.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                switchFragment(WeekPlanFragment(), weekPlanBtn)
            } else {
                Toast.makeText(this, "You need to sign in or register", Toast.LENGTH_SHORT).show()
            }
        }
        profileBtn.setOnClickListener {
            switchFragment(ProfileFragment(), profileBtn)
        }
        registerSignInBtn.setOnClickListener {
            startActivity(Intent(this, StartPageActivity::class.java))
        }
    }


    @SuppressLint("UseKtx")
    private fun switchFragment(fragment: Fragment, textView: TextView) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()

        if (!firstLoad) {
            moveIndicator(textView)
        }

        firstLoad = false


        val prefs = getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(AppPrefsConstants.LAST_FRAGMENT_KEY, fragment::class.simpleName).apply()
    }

    private fun moveIndicator(target: TextView) {
        // Get the final X position
        val x = target.x

        // Animate the indicator's X movement
        indicator.animate().x(x).setDuration(200).start()
    }

    fun showLoadingOverlayOnMenu() {
        findViewById<FrameLayout>(R.id.homePage_loadingOverlay_fl).visibility = View.VISIBLE
    }

    fun hideLoadingOverlayOnMenu() {
        findViewById<FrameLayout>(R.id.homePage_loadingOverlay_fl).visibility = View.GONE
    }
}
