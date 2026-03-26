package com.example.veganism

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.LeadingMarginSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RecipeDetailsActivity : AppCompatActivity() {

    private lateinit var timerReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isFromAddRecipe = intent.getBooleanExtra("fromAddRecipe", false)

        if (!isFromAddRecipe) {
            supportPostponeEnterTransition() // Only postpone if the user came from the list of recipes and not from the add recipe
        }

        setContentView(R.layout.activity_recipe_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        createNotificationChannel()

        val back = findViewById<TextView>(R.id.back)
        back.setOnClickListener {
            if (isFromAddRecipe) {
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putString("lastFragment", "HomeFragment").apply()

                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            } else {
                supportFinishAfterTransition()
            }
        }

        val recipeId = intent.getStringExtra("recipeId").toString()

        val recipeImage = findViewById<ImageView>(R.id.recipeDetails_recipeImage_iv)

        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        db.collection("recipes").document(recipeId).get()
            .addOnSuccessListener {
                storage.getReference("recipes_images/${it.getString("recipeImage")}").downloadUrl
                    .addOnSuccessListener { uri ->
                        Glide.with(this)
                            .load(uri)
                            .dontAnimate()
                            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                                override fun onLoadFailed(
                                    e: com.bumptech.glide.load.engine.GlideException?,
                                    model: Any?,
                                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    supportStartPostponedEnterTransition()
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: android.graphics.drawable.Drawable,
                                    model: Any,
                                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                    dataSource: com.bumptech.glide.load.DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    if (!isFromAddRecipe) {
                                        supportStartPostponedEnterTransition()
                                    }

                                    val recipeFadeIn = findViewById<LinearLayout>(R.id.recipeDetails_fadeIn_ll)

                                    // 1. Position the text slightly lower than its final spot
                                    recipeFadeIn.translationY = 100f
                                    recipeFadeIn.alpha = 0f

                                    // 2. Animate it slowly to its original position (translationY = 0)
                                    recipeFadeIn.animate()
                                        .translationY(0f) // Move up to its natural spot
                                        .alpha(1f) // Fade in
                                        .setDuration(500)
                                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                                        .start()

                                    return false
                                }
                            })
                            .into(recipeImage)
                    }
                    .addOnFailureListener {
                        recipeImage.setImageResource(R.drawable.img_recipe_item_example)
                    }
                assignRecipeData(it)
                createAndControlTimer(it)
            }
            .addOnFailureListener {
                // Need to handle failure
            }
    }

    private lateinit var floatingTimer: LinearLayout
    private lateinit var timerProgress: ProgressBar
    private lateinit var timerIcon: ImageView
    private lateinit var timerText: TextView
    fun assignRecipeData(doc: DocumentSnapshot) {
        val recipeName = findViewById<TextView>(R.id.recipeDetails_recipeName_tv)
        val recipeDescription = findViewById<TextView>(R.id.recipeDetails_recipeDescription_tv)
        val recipeCookingTime = findViewById<TextView>(R.id.recipeDetails_recipeTime_tv)
        val recipeIngredients = findViewById<TextView>(R.id.recipeDetails_recipeIngredients_tv)
        val recipeInstructions = findViewById<TextView>(R.id.recipeDetails_recipeInstructions_tv)
        val recipeNotesTitle = findViewById<TextView>(R.id.recipeDetails_recipeNotesTitle_tv)
        val recipeNotes = findViewById<TextView>(R.id.recipeDetails_recipeNotes_tv)

        floatingTimer = findViewById(R.id.recipeDetails_floatingTimer_ll)
        timerProgress = findViewById(R.id.recipeDetails_floatingTimerProgress_pb)
        timerIcon = findViewById(R.id.recipeDetails_floatingTimerIcon_iv)
        timerText = findViewById(R.id.recipeDetails_floatingTimerText_tv)

        val recipeChef = findViewById<TextView>(R.id.recipeDetails_recipeChef_tv)

        recipeName.text = doc.getString("name")
        recipeDescription.text = doc.getString("description")

        val totalCookingTime = (doc.getLong("cookingTimeMinutes") ?: 0L).toInt()
        val hours = totalCookingTime / 60
        val minutes = totalCookingTime % 60

        recipeCookingTime.text =
            if (hours == 0) {
                "Total cooking time: $minutes min"
            } else {
                "Total cooking time: $hours h $minutes min"
            }

        // Creating a bulleted and numbered list of ingredients and instructions
        recipeIngredients.text = createBulletedList(doc.getString("ingredients") ?: "Empty")
        recipeInstructions.text = createNumberedList(doc.getString("instructions") ?: "Empty")

        // If notes exist display it
        val notes = doc.getString("notes").orEmpty()
        if (notes.isBlank()) {
            recipeNotesTitle.visibility = View.GONE
            recipeNotes.visibility = View.GONE
        } else {
            recipeNotesTitle.visibility = View.VISIBLE
            recipeNotes.visibility = View.VISIBLE
            recipeNotes.text = notes
        }

        recipeChef.text = "Recipe by ${doc.getString("chefUsername")}"
    }

    fun createAndControlTimer(doc: DocumentSnapshot)
    {
        // If the AI suggested a timer then setting it
        val timerMinutes = (doc.get("timerMinutes") ?: "0").toString().toInt()
        var isTimerRunning = false // For pause/resume functionality
        var countDownTimer: CountDownTimer? = null

        if (timerMinutes > 0) {
            floatingTimer.visibility = View.VISIBLE
            timerProgress.visibility = View.GONE
            timerIcon.visibility = View.VISIBLE
            changeViewMarginBottom(timerText, 5)
            timerText.text = timerMinutes.toString()
            timerProgress.max = timerMinutes * 60 // Progress in seconds

            val totalTimerMillis = timerMinutes * 60 * 1000L
            var timeLeftMillis = totalTimerMillis

            floatingTimer.setOnClickListener {
                if (isTimerRunning) {
                    countDownTimer?.cancel()

                    timerProgress.visibility = View.VISIBLE
                    timerProgress.progressDrawable = ContextCompat.getDrawable(this, R.drawable.bg_progress_bar_paused)
                    timerProgress.progress = ((totalTimerMillis - timeLeftMillis) / 1000).toInt()

                    timerText.setTextColor(resources.getColor(R.color.timerProgressBar))
                    isTimerRunning = false
                } else {
                    countDownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            timeLeftMillis = millisUntilFinished

                            val min = (millisUntilFinished / 1000) / 60
                            val sec = (millisUntilFinished / 1000) % 60
                            timerText.text = "%d:%02d".format(min, sec)
                            timerProgress.progress = ((totalTimerMillis - timeLeftMillis) / 1000).toInt()
                            timerProgress.visibility = View.VISIBLE
                        }

                        override fun onFinish() {
                            timerText.text = "DONE!"
                            timerProgress.visibility = View.GONE
                            timeLeftMillis = totalTimerMillis
                            isTimerRunning = false

                            if (isAppInForeground()) { // If the user is in the app then making an alert, a repeated vibration and a sound
                                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

                                if (vibrator.hasVibrator()) {
                                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500), -1)
                                    vibrator.vibrate(effect)
                                }

                                val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val ringtone: Ringtone? = RingtoneManager.getRingtone(this@RecipeDetailsActivity, notificationSound)
                                ringtone?.play()

                                AlertDialog.Builder(this@RecipeDetailsActivity)
                                    .setTitle("Timer Finished ⏱")
                                    .setMessage("Your %d minutes timer has finished!".format(timerMinutes))
                                    .setCancelable(false)
                                    .setPositiveButton("OK") { dialog, _ ->
                                        vibrator.cancel()
                                        dialog.dismiss()
                                    }
                                    .show()
                            } else {
                                // If the app is in the background then sending notification
                                val intent = Intent(this@RecipeDetailsActivity, RecipeDetailsActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("recipeId", doc.id)

                                val pendingIntent = PendingIntent.getActivity(
                                    this@RecipeDetailsActivity,
                                    0,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

                                val builder = NotificationCompat.Builder(this@RecipeDetailsActivity, "TIMER_CHANNEL")
                                    .setSmallIcon(R.drawable.ic_timer)
                                    .setContentTitle("Timer Finished ⏱")
                                    .setContentText("Your cooking timer has finished!")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent)
                                    .setDefaults(NotificationCompat.DEFAULT_ALL)

                                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.notify(1, builder.build())
                            }
                        }
                    }

                    countDownTimer.start()
                    timerProgress.progressDrawable = ContextCompat.getDrawable(this, R.drawable.bg_progress_bar)
                    timerText.setTextColor(Color.argb(255,0,0,0))
                    isTimerRunning = true
                    timerProgress.visibility = View.VISIBLE
                    timerIcon.visibility = View.GONE
                    changeViewMarginBottom(timerText, 0)
                }
            }

        } else {
            floatingTimer.visibility = View.GONE
        }
    }
    fun changeViewMarginBottom(view: View, newMarginDp: Int)
    {
        val scale = resources.displayMetrics.density
        val marginInPx = (newMarginDp * scale).toInt()

        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = marginInPx
        view.layoutParams = params
    }
    fun createBulletedList(text: String, bulletMargin: Int = 30): SpannableStringBuilder {
        val lines = text.lines().filter { it.isNotBlank() }
        val result = SpannableStringBuilder()

        for (line in lines) {
            val bullet = "• "
            val fullLine = bullet + line.trim() + "\n"
            val spannable = SpannableString(fullLine)

            // Hanging indent: first line = 0, wrapped lines = bulletMargin
            spannable.setSpan(
                LeadingMarginSpan.Standard(0, bulletMargin),
                0,
                fullLine.length,
                0
            )

            result.append(spannable)
        }

        return result
    }
    fun createNumberedList(text: String, numberMargin: Int = 50): SpannableStringBuilder {
        val lines = text.lines().filter { it.isNotBlank() }
        val result = SpannableStringBuilder()

        for ((index, line) in lines.withIndex()) {
            val number = "${index + 1}. "
            val fullLine = number + line.trim() + "\n"
            val spannable = SpannableString(fullLine)

            // Hanging indent: first line = 0, wrapped lines = numberMargin
            spannable.setSpan(
                LeadingMarginSpan.Standard(0, numberMargin),
                0,
                fullLine.length,
                0
            )

            result.append(spannable)
        }

        return result
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        return appProcesses.any { it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && it.processName == packageName }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Channel"
            val descriptionText = "Notifications for recipe timer"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("TIMER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

}