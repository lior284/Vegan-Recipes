package com.example.veganism

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecipeDetailsActivity : AppCompatActivity() {

    private var isFromAddRecipe = false
    private var isFromNotification = false
    private var isRecipeScreenVisible = false
    private var isTimerRunning = false
    private var totalTimerMillis = 0L
    private var timeLeftMillis = 0L
    private var countDownTimer: CountDownTimer? = null

    private lateinit var floatingTimer: LinearLayout
    private lateinit var timerProgress: ProgressBar
    private lateinit var timerIcon: ImageView
    private lateinit var timerText: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is needed for timer alerts.", Toast.LENGTH_LONG).show()
        }
    }

    // The following functions: activity setup and main loading flow
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isFromAddRecipe = intent.getBooleanExtra("fromAddRecipe", false)
        isFromNotification = intent.getBooleanExtra("fromNotification", false)

        if (!isFromAddRecipe && !isFromNotification) {
            supportPostponeEnterTransition() // Only postpone if the user came from the list of recipes and not from the add recipe or notification
        }

        setContentView(R.layout.activity_recipe_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createNotificationChannel()

        val recipeFadeIn = findViewById<LinearLayout>(R.id.recipeDetails_fadeIn_ll)
        recipeFadeIn.visibility = View.INVISIBLE

        val recipeImage = findViewById<ImageView>(R.id.recipeDetails_recipeImage_iv)
        recipeImage.visibility = View.VISIBLE

        if (isFromAddRecipe || isFromNotification) {
            showLoadingOverlay()
        }

        loadRecipe() // Main function

        val addToWeekPlanButton = findViewById<Button>(R.id.recipeDetails_addToWeekPlan_btn)
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            addToWeekPlanButton.visibility = View.GONE
        } else {
            addToWeekPlanButton.setOnClickListener {
                showDayPickerDialog(intent.getStringExtra("recipeId").toString())
            }
        }

        setupBackButtonsHandling()
    }

    override fun onStart() {
        isRecipeScreenVisible = true
        super.onStart()
    }

    override fun onStop() {
        isRecipeScreenVisible = false
        super.onStop()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun setupBackButtonsHandling() {
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed(this)
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        val back = findViewById<TextView>(R.id.back)
        back.setOnClickListener {
            handleBackPressed(backCallback)
        }
    }

    private fun loadRecipe() {
        val recipeId = intent.getStringExtra("recipeId").toString()
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        db.collection("recipes")
            .document(recipeId)
            .get()
            .addOnSuccessListener { document ->
                loadRecipeImage(storage, document)
                assignRecipeData(document)
                setupTimer(document)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load recipe, please try again later", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadRecipeImage(storage: FirebaseStorage, document: DocumentSnapshot) {
        val recipeFadeIn = findViewById<LinearLayout>(R.id.recipeDetails_fadeIn_ll)
        val recipeImage = findViewById<ImageView>(R.id.recipeDetails_recipeImage_iv)

        storage.getReference("recipes_images/${document.getString("recipeImage")}").downloadUrl
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
                            recipeFadeIn.visibility = View.VISIBLE
                            recipeImage.visibility = View.VISIBLE
                            hideLoadingOverlay()
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isFromAddRecipe && !isFromNotification) {
                                supportStartPostponedEnterTransition()
                            }

                            recipeFadeIn.visibility = View.VISIBLE
                            recipeImage.visibility = View.VISIBLE
                            hideLoadingOverlay()

                            // Start the text slightly lower, then fade and slide it into place.
                            recipeFadeIn.translationY = 100f
                            recipeFadeIn.alpha = 0f
                            recipeFadeIn.animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(500)
                                .setInterpolator(android.view.animation.DecelerateInterpolator())
                                .start()

                            return false
                        }
                    })
                    .into(recipeImage)
            }
            .addOnFailureListener { // If the image doesn't load correctly I finish the animation just with the example image
                recipeImage.setImageResource(R.drawable.img_recipe_item_example)
                recipeFadeIn.visibility = View.VISIBLE
                recipeImage.visibility = View.VISIBLE
                hideLoadingOverlay()

                recipeFadeIn.translationY = 100f
                recipeFadeIn.alpha = 0f
                recipeFadeIn.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
    }

    // Navigation
    private fun handleBackPressed(backCallback: OnBackPressedCallback) {
        if (isFromAddRecipe || isFromNotification) {
            // In these two entry paths, "back" should return to the menu/home flow
            // instead of just closing this activity like a normal details screen.
            val prefs = getSharedPreferences(AppPrefsConstants.APP_PREFS_NAME, MODE_PRIVATE)
            prefs.edit().putString(AppPrefsConstants.LAST_FRAGMENT_KEY, "HomeFragment").apply()

            startActivity(Intent(this@RecipeDetailsActivity, MenuActivity::class.java))
            finish()
        } else {
            backCallback.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // The following functions: week plan flow

    private fun showDayPickerDialog(recipeId: String) {
        val nextSevenDays = getNextSevenDays()
        val dayLabels = nextSevenDays.map { it.displayLabel }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Choose a day:")
            .setItems(dayLabels) { _, chosenIndex ->
                val selectedDay = nextSevenDays[chosenIndex]
                showMealTypeDialog(recipeId, selectedDay)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMealTypeDialog(recipeId: String, selectedDay: PlannedDay) {
        val mealTypeLabels = arrayOf("Breakfast", "Lunch", "Dinner")

        AlertDialog.Builder(this)
            .setTitle("Choose a meal:")
            .setItems(mealTypeLabels) { _, chosenIndex ->
                val mealField = when (chosenIndex) {
                    0 -> "breakfastId"
                    1 -> "lunchId"
                    else -> "dinnerId"
                }
                val mealName = mealTypeLabels[chosenIndex]

                addRecipeToWeekPlan(recipeId, selectedDay, mealField, mealName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addRecipeToWeekPlan(
        recipeId: String,
        selectedDay: PlannedDay,
        mealField: String,
        mealName: String
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val mealPlanDocument = db.collection("users")
            .document(user.uid)
            .collection("mealPlans")
            .document(selectedDay.documentId)

        mealPlanDocument.get()
            .addOnSuccessListener { document ->
                val existingRecipeId = document.getString(mealField)

                // If the same recipe is already there then there is nothing to replace
                if (existingRecipeId == recipeId) {
                    Toast.makeText(
                        this,
                        "This recipe is already planned for ${selectedDay.displayLabel.lowercase()} $mealName.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (existingRecipeId.isNullOrBlank()) {
                    saveMealPlanSlot(mealPlanDocument, recipeId, mealField, selectedDay)
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Replace planned meal?")
                        .setMessage("$mealName on ${selectedDay.displayLabel} already has a recipe. Do you want to replace it?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Replace") { _, _ ->
                            saveMealPlanSlot(mealPlanDocument, recipeId, mealField, selectedDay)
                        }
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to check week plan.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveMealPlanSlot(
        mealPlanDocument: com.google.firebase.firestore.DocumentReference,
        recipeId: String,
        mealField: String,
        date: PlannedDay
    ) {
        val data = mapOf(mealField to recipeId)

        mealPlanDocument.set(data, SetOptions.merge())
            .addOnSuccessListener {
                // Sending RESULT_OK back to the week plan page so it knows it needs to refresh
                setResult(RESULT_OK)

                val mealType = getMealType(mealField)
                val mealTypeLabel = mealType.lowercase().replaceFirstChar { it.uppercase() }
                val user = FirebaseAuth.getInstance().currentUser
                val notificationTime = getMealNotificationTime(mealField, user!!.uid)

                Toast.makeText(this@RecipeDetailsActivity, "Added to ${date.displayLabel} $mealTypeLabel.", Toast.LENGTH_SHORT).show()
                // Need to activate the alarm for the new meal
                MealPlanNotificationManager.scheduleMealNotification(
                    this@RecipeDetailsActivity,
                    recipeId,
                    mealType,
                    date.documentId,
                    notificationTime
                )
            }
            .addOnFailureListener {
                Toast.makeText(this@RecipeDetailsActivity, "Failed to add to week plan.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getMealType(mealField: String): String {
        return when (mealField) {
            "breakfastId" -> MealType.BREAKFAST.toString()
            "lunchId" -> MealType.LUNCH.toString()
            "dinnerId" -> MealType.DINNER.toString()
            else -> ""
        }
    }

    private fun getMealNotificationTime(mealField: String, uid: String): String {
        return when (mealField) {
            "breakfastId" -> SettingsManager.getBreakfastNotificationTime(this, uid)
            "lunchId" -> SettingsManager.getLunchNotificationTime(this, uid)
            "dinnerId" -> SettingsManager.getDinnerNotificationTime(this, uid)
            else -> "08:00"
        }
    }

    private fun getNextSevenDays(): List<PlannedDay> {
        val documentFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val days = mutableListOf<PlannedDay>()

        for (i in 0 until 7) {
            val currentDate = calendar.time
            val baseLabel = displayFormatter.format(currentDate)
            val displayLabel = when (i) {
                0 -> "Today ($baseLabel)"
                1 -> "Tomorrow ($baseLabel)"
                else -> baseLabel
            }

            days.add(
                PlannedDay(
                    documentId = documentFormatter.format(currentDate),
                    displayLabel = displayLabel
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return days
    }

    data class PlannedDay(
        val documentId: String,
        val displayLabel: String
    )

    // Content display
    fun assignRecipeData(doc: DocumentSnapshot) {
        val recipeName = findViewById<TextView>(R.id.recipeDetails_recipeName_tv)
        val recipeDescription = findViewById<TextView>(R.id.recipeDetails_recipeDescription_tv)
        val recipeCookingTime = findViewById<TextView>(R.id.recipeDetails_recipeTime_tv)
        val recipeIngredients = findViewById<TextView>(R.id.recipeDetails_recipeIngredients_tv)
        val recipeInstructions = findViewById<TextView>(R.id.recipeDetails_recipeInstructions_tv)
        val recipeNotesTitle = findViewById<TextView>(R.id.recipeDetails_recipeNotesTitle_tv)
        val recipeNotes = findViewById<TextView>(R.id.recipeDetails_recipeNotes_tv)
        val recipeChef = findViewById<TextView>(R.id.recipeDetails_recipeChef_tv)

        floatingTimer = findViewById(R.id.recipeDetails_floatingTimer_ll)
        timerProgress = findViewById(R.id.recipeDetails_floatingTimerProgress_pb)
        timerIcon = findViewById(R.id.recipeDetails_floatingTimerIcon_iv)
        timerText = findViewById(R.id.recipeDetails_floatingTimerText_tv)

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

        recipeIngredients.text = createBulletedList(doc.getString("ingredients") ?: "Error loading, please try again later.")
        recipeInstructions.text = createNumberedList(doc.getString("instructions") ?: "Error loading, please try again later.")

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

    // The following functions: timer flow
    private fun setupTimer(doc: DocumentSnapshot) {
        val timerMinutes = (doc.get("timerMinutes") ?: "0").toString().toIntOrNull() ?: 0
        val recipeId = doc.id

        if (timerMinutes == 0) {
            floatingTimer.visibility = View.GONE
            return
        }

        floatingTimer.visibility = View.VISIBLE
        totalTimerMillis = timerMinutes * 60 * 1000L
        timerProgress.max = timerMinutes * 60 // The progress is in seconds

        // If this recipe already had a running timer saved in prefs, then restoring the remaining time.
        // Otherwise showing the normal initial state of a timer.
        val savedTimerEndTime = getSavedTimerEndTime(recipeId)
        if (savedTimerEndTime != null && savedTimerEndTime > System.currentTimeMillis()) {
            timeLeftMillis = savedTimerEndTime - System.currentTimeMillis()
            startTimerCountdown(recipeId, timerMinutes, false)
        } else {
            initialOrResetTimer(recipeId, timerMinutes)
            if(savedTimerEndTime != null) // If entering the activity after the timer has finished then I want to show 'Done!'
            {
                timerText.text = "DONE!"
                timerProgress.visibility = View.GONE
                changeViewMarginBottom(timerText, 0)
            }
        }

        floatingTimer.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer(recipeId)
            } else {
                startTimerCountdown(recipeId, timerMinutes, true)
            }
        }

        floatingTimer.setOnLongClickListener {
            if (isTimerRunning) {
                initialOrResetTimer(recipeId, timerMinutes)
                true
            } else {
                false
            }
        }
    }

    private fun showInitialTimerState(timerMinutes: Int) {
        isTimerRunning = false
        timeLeftMillis = totalTimerMillis
        timerProgress.visibility = View.GONE
        timerIcon.visibility = View.VISIBLE
        changeViewMarginBottom(timerText, 5)
        timerText.text = timerMinutes.toString()
        timerText.setTextColor(Color.argb(255, 0, 0, 0))
    }

    private fun initialOrResetTimer(recipeId: String, timerMinutes: Int) {
        countDownTimer?.cancel()
        clearSavedTimerState(recipeId) // Clearing prefs
        cancelTimerAlarm(recipeId) // Canceling scheduled alarm
        showInitialTimerState(timerMinutes) // Resetting UI
    }

    private fun pauseTimer(recipeId: String) {
        countDownTimer?.cancel()
        cancelTimerAlarm(recipeId)
        clearSavedTimerState(recipeId)

        timerProgress.visibility = View.VISIBLE
        timerProgress.progressDrawable = ContextCompat.getDrawable(this, R.drawable.bg_progress_bar_paused)
        timerProgress.progress = ((totalTimerMillis - timeLeftMillis) / 1000).toInt()
        timerText.setTextColor(resources.getColor(R.color.timerProgressBar))
        isTimerRunning = false
    }

    private fun startTimerCountdown(recipeId: String, timerMinutes: Int, needToScheduleAlarm: Boolean) {
        countDownTimer?.cancel() // Just in case there's an old timer running

        if (needToScheduleAlarm) {
            // Saving the exact finish time so the timer can be restored if the user leaves the screen, and scheduling an alarm in case
            // the timer finishes and the user has left the screen.
            val timerEndTime = System.currentTimeMillis() + timeLeftMillis
            saveRunningTimerState(recipeId, timerEndTime)
            if (areTimerNotificationsEnabled()) {
                requestNotificationPermissionIfNeeded()
                scheduleTimerAlarm(recipeId, timerMinutes, timerEndTime)
            }
        }

        countDownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished

                val min = millisUntilFinished / 1000 / 60
                val sec = millisUntilFinished / 1000 % 60
                timerText.text = "%d:%02d".format(min, sec)
                timerProgress.progress = ((totalTimerMillis - timeLeftMillis) / 1000).toInt()
                timerProgress.visibility = View.VISIBLE
            }

            override fun onFinish() {
                clearSavedTimerState(recipeId)
                cancelTimerAlarm(recipeId)

                timerText.text = "DONE!"
                timerProgress.visibility = View.GONE
                changeViewMarginBottom(timerText, 0)
                timeLeftMillis = totalTimerMillis
                isTimerRunning = false

                if (isRecipeScreenVisible) {
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (vibrator.hasVibrator()) {
                        val effect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500), -1)
                        vibrator.vibrate(effect)
                    }

                    val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone: Ringtone? = RingtoneManager.getRingtone(this@RecipeDetailsActivity, notificationSound)
                    ringtone?.play()

                    AlertDialog.Builder(this@RecipeDetailsActivity)
                        .setTitle("Timer Finished!")
                        .setMessage("Your $timerMinutes minutes timer has finished!")
                        .setCancelable(false)
                        .setPositiveButton("OK") { dialog, _ ->
                            vibrator.cancel()
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    // If this screen is no longer visible, the mark for the timer finish should
                    // create a notification instead of a dialog.
                    if (areTimerNotificationsEnabled()) {
                        showTimerFinishedNotification(recipeId)
                    }
                }
            }
        }

        countDownTimer?.start()
        timerProgress.progressDrawable = ContextCompat.getDrawable(this, R.drawable.bg_progress_bar)
        timerText.setTextColor(Color.argb(255, 0, 0, 0))
        isTimerRunning = true
        timerProgress.visibility = View.VISIBLE
        timerIcon.visibility = View.GONE
        changeViewMarginBottom(timerText, 0)
    }

    private fun saveRunningTimerState(recipeId: String, timerEndTime: Long) {
        val prefs = getSharedPreferences(TIMER_PREFS_NAME, Context.MODE_PRIVATE)
        val oldRecipeId = prefs.getString(TIMER_RECIPE_ID_KEY, null)

        // Only one recipe timer is allowed at a time, so cancel the old recipe's alarm
        // before saving the new one.
        if (oldRecipeId != null && oldRecipeId != recipeId) {
            cancelTimerAlarm(oldRecipeId)
        }

        prefs.edit()
            .putString(TIMER_RECIPE_ID_KEY, recipeId)
            .putLong(TIMER_END_TIME_KEY, timerEndTime)
            .apply()
    }

    private fun clearSavedTimerState(recipeId: String) {
        val prefs = getSharedPreferences(TIMER_PREFS_NAME, Context.MODE_PRIVATE)
        val savedRecipeId = prefs.getString(TIMER_RECIPE_ID_KEY, null)

        if (savedRecipeId == recipeId) {
            prefs.edit()
                .remove(TIMER_RECIPE_ID_KEY)
                .remove(TIMER_END_TIME_KEY)
                .apply()
        }
    }

    private fun getSavedTimerEndTime(recipeId: String): Long? {
        val prefs = getSharedPreferences(TIMER_PREFS_NAME, MODE_PRIVATE)
        val savedRecipeId = prefs.getString(TIMER_RECIPE_ID_KEY, null)
        // If the current recipe is not the recipe that has a saved timer then return null
        if (savedRecipeId != recipeId) {
            return null
        }

        val savedTime = prefs.getLong(TIMER_END_TIME_KEY, 0L)
        return if (savedTime > 0L) savedTime else null
    }

    private fun scheduleTimerAlarm(recipeId: String, timerMinutes: Int, timerEndTime: Long) {
        val intent = Intent(this, TimerFinishedReceiver::class.java).apply {
            putExtra("recipeId", recipeId)
            putExtra("timerMinutes", timerMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            recipeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timerEndTime,
            pendingIntent
        )
    }

    private fun cancelTimerAlarm(recipeId: String) {
        val intent = Intent(this, TimerFinishedReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            recipeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun showTimerFinishedNotification(recipeId: String) {
        val intent = Intent(this, RecipeDetailsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("recipeId", recipeId)
            putExtra("fromNotification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            recipeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "TIMER_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Timer Finished!")
            .setContentText("Your cooking timer has finished!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(recipeId.hashCode(), notification)
    }

    // The following functions: formatting and shared helpers
    fun changeViewMarginBottom(view: View, newMarginDp: Int) {
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
            val bullet = "ג€¢ "
            val fullLine = bullet + line.trim() + "\n"
            val spannable = SpannableString(fullLine)

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("TIMER_CHANNEL", name, importance).apply {
                description = "Notifications for recipe timer"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun areTimerNotificationsEnabled(): Boolean {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        return if (userUid.isBlank()) {
            false
        } else {
            SettingsManager.isTimerNotificationsEnabled(this, userUid)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun showLoadingOverlay() {
        findViewById<FrameLayout>(R.id.recipeDetails_loadingOverlay_fl).visibility = View.VISIBLE
    }

    fun hideLoadingOverlay() {
        findViewById<FrameLayout>(R.id.recipeDetails_loadingOverlay_fl).visibility = View.GONE
    }

    companion object {
        const val TIMER_PREFS_NAME = "timer_prefs"
        const val TIMER_RECIPE_ID_KEY = "timerRecipeId"
        const val TIMER_END_TIME_KEY = "timerEndTime"
    }
}
