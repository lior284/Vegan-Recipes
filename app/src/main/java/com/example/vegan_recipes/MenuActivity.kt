package com.example.vegan_recipes

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

    private lateinit var tvHome: TextView
    private lateinit var tvAddRecipe: TextView
    private lateinit var tvWeekPlan: TextView
    private lateinit var tvProfile: TextView
    private lateinit var tvRegisterSignIn: TextView

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

        tvHome = findViewById<TextView>(R.id.menuPage_home_tv)
        tvAddRecipe = findViewById<TextView>(R.id.menuPage_addRecipe_tv)
        tvWeekPlan = findViewById<TextView>(R.id.menuPage_weekPlan_tv)
        tvProfile = findViewById<TextView>(R.id.menuPage_profile_tv)
        tvRegisterSignIn = findViewById<TextView>(R.id.menuPage_registerSignIn_tv)

        indicator = findViewById(R.id.menuPage_indicator_vw)

        val prefs = getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, MODE_PRIVATE)

        val lastFragment = prefs.getString(AppPrefsConstants.LAST_FRAGMENT_KEY, null)

        val (fragment, textView) = when (lastFragment) {
            "AddRecipeFragment" -> AddRecipeFragment() to tvAddRecipe
            "WeekPlanFragment" -> WeekPlanFragment() to tvWeekPlan
            "ProfileFragment" -> ProfileFragment() to tvProfile
            else -> HomeFragment() to tvHome
        }

        switchFragment(fragment, textView)


        if (FirebaseAuth.getInstance().currentUser != null) {
            tvAddRecipe.visibility = View.VISIBLE
            tvWeekPlan.visibility = View.VISIBLE
            tvProfile.visibility = View.VISIBLE
            tvRegisterSignIn.visibility = View.GONE

            // Get screen width in pixels
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            // Set indicator width to quarter the screen width (there are four buttons)
            indicator.layoutParams.width = screenWidth / 4
            indicator.requestLayout()
        } else {
            tvAddRecipe.visibility = View.GONE
            tvWeekPlan.visibility = View.GONE
            tvProfile.visibility = View.GONE
            tvRegisterSignIn.visibility = View.VISIBLE

            // Get screen width in pixels
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            // Set indicator width to half the screen width (there are only two buttons)
            indicator.layoutParams.width = screenWidth / 2
            indicator.requestLayout()
        }

        indicator.post {
            indicator.x = textView.x
        }

        tvHome.setOnClickListener {
            switchFragment(HomeFragment(), tvHome)
        }
        tvAddRecipe.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                switchFragment(AddRecipeFragment(), tvAddRecipe)
            } else {
                Toast.makeText(this, "You need to sign in or register", Toast.LENGTH_SHORT).show()
            }
        }
        tvWeekPlan.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                switchFragment(WeekPlanFragment(), tvWeekPlan)
            } else {
                Toast.makeText(this, "You need to sign in or register", Toast.LENGTH_SHORT).show()
            }
        }
        tvProfile.setOnClickListener {
            switchFragment(ProfileFragment(), tvProfile)
        }
        tvRegisterSignIn.setOnClickListener {
            startActivity(Intent(this, StartPageActivity::class.java))
        }
    }


    @SuppressLint("UseKtx")
    private fun switchFragment(fragment: Fragment, textView: TextView) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.menuPage_fragmentContainer_fl, fragment)
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

    public fun showLoadingOverlayOnMenu() {
        findViewById<FrameLayout>(R.id.menuPage_loadingOverlay_fl).visibility = View.VISIBLE
    }

    public fun hideLoadingOverlayOnMenu() {
        findViewById<FrameLayout>(R.id.menuPage_loadingOverlay_fl).visibility = View.GONE
    }
}
