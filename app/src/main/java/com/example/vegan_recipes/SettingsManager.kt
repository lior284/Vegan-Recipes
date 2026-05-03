package com.example.vegan_recipes

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object SettingsManager {
    private const val SETTINGS_PREFIX = "settings_"
    private const val DARK_MODE_KEY = "darkMode"
    private const val TIMER_NOTIFICATIONS_KEY = "timerNotifications"
    private const val WEEK_PLAN_NOTIFICATIONS_KEY = "weekPlanNotifications"
    private const val BREAKFAST_NOTIFICATIONS_KEY = "breakfastNotifications"
    private const val LUNCH_NOTIFICATIONS_KEY = "lunchNotifications"
    private const val DINNER_NOTIFICATIONS_KEY = "dinnerNotifications"
    private const val BREAKFAST_TIME_KEY = "breakfastNotificationTime"
    private const val LUNCH_TIME_KEY = "lunchNotificationTime"
    private const val DINNER_TIME_KEY = "dinnerNotificationTime"

    private const val DEFAULT_BREAKFAST_TIME = "08:00"
    private const val DEFAULT_LUNCH_TIME = "13:00"
    private const val DEFAULT_DINNER_TIME = "19:00"

    private fun getUserPrefs(context: Context, uid: String): SharedPreferences {
        return context.getSharedPreferences("$SETTINGS_PREFIX$uid", Context.MODE_PRIVATE)
    }

    fun saveDefaultSettings(context: Context, uid: String) {
        getUserPrefs(context, uid).edit {
            putBoolean(DARK_MODE_KEY, false)
                .putBoolean(TIMER_NOTIFICATIONS_KEY, false)
                .putBoolean(WEEK_PLAN_NOTIFICATIONS_KEY, false)
                .putBoolean(BREAKFAST_NOTIFICATIONS_KEY, true)
                .putBoolean(LUNCH_NOTIFICATIONS_KEY, true)
                .putBoolean(DINNER_NOTIFICATIONS_KEY, true)
                .putString(BREAKFAST_TIME_KEY, DEFAULT_BREAKFAST_TIME)
                .putString(LUNCH_TIME_KEY, DEFAULT_LUNCH_TIME)
                .putString(DINNER_TIME_KEY, DEFAULT_DINNER_TIME)
        }
    }

    fun isDarkModeEnabled(context: Context, uid: String): Boolean {
        return getUserPrefs(context, uid).getBoolean(DARK_MODE_KEY, false)
    }

    fun setDarkModeEnabled(context: Context, uid: String, isEnabled: Boolean) {
        getUserPrefs(context, uid).edit()
            .putBoolean(DARK_MODE_KEY, isEnabled)
            .apply()
    }

    fun isTimerNotificationsEnabled(context: Context, uid: String): Boolean {
        return getUserPrefs(context, uid).getBoolean(TIMER_NOTIFICATIONS_KEY, true)
    }

    fun setTimerNotificationsEnabled(context: Context, uid: String, isEnabled: Boolean) {
        getUserPrefs(context, uid).edit()
            .putBoolean(TIMER_NOTIFICATIONS_KEY, isEnabled)
            .apply()
    }

    fun isWeekPlanNotificationsEnabled(context: Context, uid: String): Boolean {
        return getUserPrefs(context, uid).getBoolean(WEEK_PLAN_NOTIFICATIONS_KEY, true)
    }

    fun setWeekPlanNotificationsEnabled(context: Context, uid: String, isEnabled: Boolean) {
        getUserPrefs(context, uid).edit()
            .putBoolean(WEEK_PLAN_NOTIFICATIONS_KEY, isEnabled)
            .apply()
    }

    fun isBreakfastNotificationsEnabled(context: Context, uid: String): Boolean {
        return getUserPrefs(context, uid).getBoolean(BREAKFAST_NOTIFICATIONS_KEY, true)
    }

    fun setBreakfastNotificationsEnabled(context: Context, uid: String, isEnabled: Boolean) {
        getUserPrefs(context, uid).edit()
            .putBoolean(BREAKFAST_NOTIFICATIONS_KEY, isEnabled)
            .apply()
    }

    fun isLunchNotificationsEnabled(context: Context, uid: String): Boolean {
        return getUserPrefs(context, uid).getBoolean(LUNCH_NOTIFICATIONS_KEY, true)
    }

    fun setLunchNotificationsEnabled(context: Context, uid: String, isEnabled: Boolean) {
        getUserPrefs(context, uid).edit()
            .putBoolean(LUNCH_NOTIFICATIONS_KEY, isEnabled)
            .apply()
    }

    fun isDinnerNotificationsEnabled(context: Context, uid: String): Boolean {
        return getUserPrefs(context, uid).getBoolean(DINNER_NOTIFICATIONS_KEY, true)
    }

    fun setDinnerNotificationsEnabled(context: Context, uid: String, isEnabled: Boolean) {
        getUserPrefs(context, uid).edit()
            .putBoolean(DINNER_NOTIFICATIONS_KEY, isEnabled)
            .apply()
    }

    fun getBreakfastNotificationTime(context: Context, uid: String): String {
        return getUserPrefs(context, uid).getString(BREAKFAST_TIME_KEY, DEFAULT_BREAKFAST_TIME) ?: DEFAULT_BREAKFAST_TIME
    }

    fun setBreakfastNotificationTime(context: Context, uid: String, time: String) {
        getUserPrefs(context, uid).edit()
            .putString(BREAKFAST_TIME_KEY, time)
            .apply()
    }

    fun getLunchNotificationTime(context: Context, uid: String): String {
        return getUserPrefs(context, uid).getString(LUNCH_TIME_KEY, DEFAULT_LUNCH_TIME) ?: DEFAULT_LUNCH_TIME
    }

    fun setLunchNotificationTime(context: Context, uid: String, time: String) {
        getUserPrefs(context, uid).edit()
            .putString(LUNCH_TIME_KEY, time)
            .apply()
    }

    fun getDinnerNotificationTime(context: Context, uid: String): String {
        return getUserPrefs(context, uid).getString(DINNER_TIME_KEY, DEFAULT_DINNER_TIME) ?: DEFAULT_DINNER_TIME
    }

    fun setDinnerNotificationTime(context: Context, uid: String, time: String) {
        getUserPrefs(context, uid).edit()
            .putString(DINNER_TIME_KEY, time)
            .apply()
    }

    fun applySettings(context: Context, uid: String) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled(context, uid))
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applyDefaultSettings() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
