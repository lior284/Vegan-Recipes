package com.example.veganism

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.TextViewCompat

class NotificationsSettingsActivity : AppCompatActivity() {
    private lateinit var userUid: String
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Please enable notifications permission.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userUid = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userUID", "") ?: ""
        if (userUid.isBlank()) {
            finish()
            return
        }

        val back = findViewById<TextView>(R.id.userDetails_backArrow_iv)

        val scTimerNotifications = findViewById<SwitchCompat>(R.id.notificationsSettings_timerNotifications_sc)

        val scWeekPlanNotifications = findViewById<SwitchCompat>(R.id.notificationsSettings_weekPlanNotifications_sc)

        val tvBreakfastNotifications = findViewById<TextView>(R.id.notificationsSettings_breakfastNotifications_tv)
        val scBreakfastNotifications = findViewById<SwitchCompat>(R.id.notificationsSettings_breakfastNotifications_sc)
        val tvBreakfastHour = findViewById<TextView>(R.id.notificationsSettings_breakfastHour_tv)

        val tvLunchNotifications = findViewById<TextView>(R.id.notificationsSettings_lunchNotifications_tv)
        val scLunchNotifications = findViewById<SwitchCompat>(R.id.notificationsSettings_lunchNotifications_sc)
        val tvLunchHour = findViewById<TextView>(R.id.notificationsSettings_lunchHour_tv)

        val tvDinnerNotifications = findViewById<TextView>(R.id.notificationsSettings_dinnerNotifications_tv)
        val scDinnerNotifications = findViewById<SwitchCompat>(R.id.notificationsSettings_dinnerNotifications_sc)
        val tvDinnerHour = findViewById<TextView>(R.id.notificationsSettings_dinnerHour_tv)

        back.setOnClickListener {
            finish()
        }

        scTimerNotifications.isChecked = SettingsManager.isTimerNotificationsEnabled(this, userUid)
        scWeekPlanNotifications.isChecked = SettingsManager.isWeekPlanNotificationsEnabled(this, userUid)
        scBreakfastNotifications.isChecked = SettingsManager.isBreakfastNotificationsEnabled(this, userUid)
        scLunchNotifications.isChecked = SettingsManager.isLunchNotificationsEnabled(this, userUid)
        scDinnerNotifications.isChecked = SettingsManager.isDinnerNotificationsEnabled(this, userUid)

        setHourText(tvBreakfastHour, SettingsManager.getBreakfastNotificationTime(this, userUid))
        setHourText(tvLunchHour, SettingsManager.getLunchNotificationTime(this, userUid))
        setHourText(tvDinnerHour, SettingsManager.getDinnerNotificationTime(this, userUid))

        updateMealNotificationState(tvBreakfastNotifications, scBreakfastNotifications, tvBreakfastHour, scWeekPlanNotifications.isChecked)
        updateMealNotificationState(tvLunchNotifications, scLunchNotifications, tvLunchHour, scWeekPlanNotifications.isChecked)
        updateMealNotificationState(tvDinnerNotifications, scDinnerNotifications, tvDinnerHour, scWeekPlanNotifications.isChecked)

        tvBreakfastHour.setOnClickListener {
            if (tvBreakfastHour.isEnabled) {
                showTimePicker(tvBreakfastHour) { selectedTime ->
                    SettingsManager.setBreakfastNotificationTime(this, userUid, selectedTime)
                    MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
                }
            }
        }

        tvLunchHour.setOnClickListener {
            if (tvLunchHour.isEnabled) {
                showTimePicker(tvLunchHour) { selectedTime ->
                    SettingsManager.setLunchNotificationTime(this, userUid, selectedTime)
                    MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
                }
            }
        }

        tvDinnerHour.setOnClickListener {
            if (tvDinnerHour.isEnabled) {
                showTimePicker(tvDinnerHour) { selectedTime ->
                    SettingsManager.setDinnerNotificationTime(this, userUid, selectedTime)
                    MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
                }
            }
        }

        scTimerNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            }
            SettingsManager.setTimerNotificationsEnabled(this, userUid, isChecked)
        }

        scWeekPlanNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            }
            updateMealNotificationState(tvBreakfastNotifications, scBreakfastNotifications, tvBreakfastHour, isChecked)
            updateMealNotificationState(tvLunchNotifications, scLunchNotifications, tvLunchHour, isChecked)
            updateMealNotificationState(tvDinnerNotifications, scDinnerNotifications, tvDinnerHour, isChecked)
            SettingsManager.setWeekPlanNotificationsEnabled(this, userUid, isChecked)
            MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
        }

        scBreakfastNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            }
            updateHourState(tvBreakfastHour, isChecked && scWeekPlanNotifications.isChecked)
            SettingsManager.setBreakfastNotificationsEnabled(this, userUid, isChecked)
            MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
        }

        scLunchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            }
            updateHourState(tvLunchHour, isChecked && scWeekPlanNotifications.isChecked)
            SettingsManager.setLunchNotificationsEnabled(this, userUid, isChecked)
            MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
        }

        scDinnerNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            }
            updateHourState(tvDinnerHour, isChecked && scWeekPlanNotifications.isChecked)
            SettingsManager.setDinnerNotificationsEnabled(this, userUid, isChecked)
            MealPlanNotificationManager.rescheduleWeekPlanNotifications(this, userUid)
        }
    }

    private fun updateMealNotificationState(
        titleView: TextView,
        switchView: SwitchCompat,
        hourView: TextView,
        isParentEnabled: Boolean
    ) {
        titleView.isEnabled = isParentEnabled
        switchView.isEnabled = isParentEnabled
        switchView.isClickable = isParentEnabled
        titleView.alpha = if (isParentEnabled) 1f else 0.5f
        switchView.alpha = if (isParentEnabled) 1f else 0.5f
        titleView.setTextColor(getNotificationTextColor(isParentEnabled))

        updateHourState(hourView, isParentEnabled && switchView.isChecked)
    }

    private fun updateHourState(hourView: TextView, isEnabled: Boolean) {
        hourView.isEnabled = isEnabled
        hourView.isClickable = isEnabled
        hourView.alpha = if (isEnabled) 1f else 0.5f
        hourView.setTextColor(getNotificationTextColor(isEnabled))
        TextViewCompat.setCompoundDrawableTintList(
            hourView,
            ColorStateList.valueOf(getNotificationTextColor(isEnabled))
        )
        setHourText(hourView, extractTimeFromHourText(hourView))
    }

    private fun showTimePicker(hourView: TextView, onTimeSelected: (String) -> Unit) {
        val currentTime = extractTimeFromHourText(hourView)
        val currentHour = currentTime.substringBefore(":").toInt()
        val currentMinute = currentTime.substringAfter(":").toInt()

        TimePickerDialog(this, { _, hour, minute ->
            val selectedTime = "%02d:%02d".format(hour, minute)
            setHourText(hourView, selectedTime)
            onTimeSelected(selectedTime)
        }, currentHour, currentMinute, true).show()
    }

    private fun setHourText(hourView: TextView, time: String) {
        val fullText = "Change time: $time"

        if (!hourView.isEnabled) {
            hourView.text = fullText
            return
        }

        val spannableText = SpannableString(fullText)
        val underlineStart = fullText.indexOf(time)
        val underlineEnd = underlineStart + time.length
        spannableText.setSpan(UnderlineSpan(), underlineStart, underlineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        hourView.text = spannableText
    }

    private fun extractTimeFromHourText(hourView: TextView): String {
        return hourView.text.toString().substringAfter("Change time: ")
    }

    private fun getNotificationTextColor(isEnabled: Boolean): Int {
        val colorRes = if (isEnabled) R.color.primaryUI else R.color.disabledSettingText
        return ContextCompat.getColor(this, colorRes)
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
}
